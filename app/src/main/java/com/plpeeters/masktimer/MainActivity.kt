package com.plpeeters.masktimer

import android.app.AlarmManager
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.*
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.data.persistence.MaskDao
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
import com.plpeeters.masktimer.data.persistence.MaskTypes
import com.plpeeters.masktimer.databinding.ActivityMainBinding
import com.plpeeters.masktimer.databinding.MaskListItemBinding
import com.plpeeters.masktimer.utils.*


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var maskListAdapter: MaskListAdapter
    private lateinit var maskListViewModel: MaskListViewModel
    private val maskDatabaseDao: MaskDao by lazy { MaskDatabaseSingleton(application).maskDatabaseDao() }
    private val notificationManager: NotificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val alarmManager: AlarmManager by lazy { getSystemService(AlarmManager::class.java)}
    private val localBroadcastManager: LocalBroadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_REPLACE || intent.action == ACTION_STOP_WEARING) {
                updateUi()
            } else if (intent.action == ACTION_DELETE) {
                val mask = intent.extras?.getParcelable<Mask>(MASK_EXTRA)

                if (maskListViewModel.previousMask == mask) {
                    maskListViewModel.previousMask = null
                }
            }
        }
    }
    private val sharedPreferences: SharedPreferences by lazy { getSharedPreferences() }
    private val blinkingAnimation = AlphaAnimation(0F, 1F).apply {
        duration = 250
        startOffset = 0
        repeatMode = Animation.REVERSE
        repeatCount = Animation.INFINITE
    }
    private var baseTimerColor: Int = -1
    private var actionsShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionsShown = sharedPreferences.getBoolean(Preferences.ACTIONS_SHOWN, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        baseTimerColor = binding.maskTimer.textColors.defaultColor

        maskListViewModel = ViewModelProvider(this).get(MaskListViewModel::class.java)
        maskListAdapter = MaskListAdapter(
            this,
            R.layout.mask_list_item,
            maskListViewModel.masks)

        binding.maskList.adapter = maskListAdapter
        binding.maskList.setOnItemClickListener { _, _, position, _ ->
            onMaskSelected(maskListAdapter.getItem(position))
        }

        if (!actionsShown) {
            binding.maskList.setOnHierarchyChangeListener(object: ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (maskListViewModel.masks.size == 1 && !actionsShown) {
                        child?.let {
                            val firstMaskListItemView = MaskListItemBinding.bind(it)

                            Handler(Looper.getMainLooper()).post {
                                firstMaskListItemView.swipeContainer.open(true)
                            }

                            Snackbar.make(binding.root, R.string.swipe_to_reveal_actions, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.got_it) {
                                    firstMaskListItemView.swipeContainer.close(true)

                                    actionsShown = true

                                    sharedPreferences.edit {
                                        putBoolean(Preferences.ACTIONS_SHOWN, true)
                                    }
                                }.show()
                        }
                    }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
        }

        binding.pauseWearingButton.setOnClickListener {
            onPauseCurrentMask()
        }

        binding.stopWearingButton.setOnClickListener {
            onStopWearingMask()
        }

        binding.replaceButton.setOnClickListener {
            onReplaceCurrentMask()
        }

        binding.fab.setOnClickListener {
            AlertDialog.Builder(this).addMaskDialog(maskListViewModel.masks) { type: String, name: String ->
                val mask = Mask(type, name)

                maskListViewModel.masks.add(mask)

                updateUi()

                Thread {
                    maskDatabaseDao.insert(mask.toMaskEntity())
                }.start()
            }.show()
        }
    }

    override fun onResume() {
        super.onResume()

        updateUi()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)

                return true
            }
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onPauseCurrentMask() {
        maskListViewModel.currentMask?.let {
            if (it.isBeingWorn) {
                it.pauseWearing()
                binding.maskTimer.stop()

                alarmManager.cancelMaskAlarm(this)
                binding.pauseWearingButton.setImageResource(R.drawable.baseline_play_arrow_24dp)
            } else {
                onMaskSelected(it, true)
                binding.pauseWearingButton.setImageResource(R.drawable.baseline_pause_24dp)
            }
        }
    }

    private fun onReplaceCurrentMask() {
        maskListViewModel.currentMask?.let {
            onStopWearingMask()
            it.replace()
            onMaskSelected(it)
            notificationManager.dismissMaskTimerExpiredNotification()
        }
    }

    fun updateUi() {
        maskListViewModel.currentMask.let { currentMask ->
            if (currentMask?.isBeingWorn == true || currentMask?.isPaused == true) {
                binding.maskTimer.base = SystemClock.elapsedRealtime() + currentMask.getRemainingLifespanMillis(this)
                binding.instructions.visibility = View.INVISIBLE
                binding.maskTimerContainer.visibility = View.VISIBLE
                binding.wearingMask.text = resources.getString(R.string.wearing_your_mask, currentMask.name, currentMask.getDisplayType(this))

                if (!currentMask.isPaused) {
                    binding.pauseWearingButton.setImageResource(R.drawable.baseline_pause_24dp)
                    binding.maskTimer.start()
                    binding.maskTimer.setTextColor(baseTimerColor)

                    if (currentMask.isExpired(this)) {
                        binding.maskTimer.onChronometerTickListener = null

                        binding.maskTimer.setTextColor(resources.getColor(R.color.timer_expired_color, theme))
                        binding.maskTimer.startAnimation(blinkingAnimation)
                    } else {
                        binding.maskTimer.setTextColor(baseTimerColor)
                        binding.maskTimer.animation = null

                        binding.maskTimer.setOnChronometerTickListener {
                            val remainingMillis = it.base - SystemClock.elapsedRealtime()

                            if (remainingMillis <= 0L) {
                                binding.maskTimer.setTextColor(resources.getColor(R.color.timer_expired_color, theme))
                                binding.maskTimer.startAnimation(blinkingAnimation)

                                binding.maskTimer.onChronometerTickListener = null
                            }
                        }
                    }
                } else {
                    binding.maskTimer.stop()
                    binding.pauseWearingButton.setImageResource(R.drawable.baseline_play_arrow_24dp)
                }
            } else {
                binding.maskTimer.stop()
                binding.maskTimer.animation = null
                binding.maskTimer.setTextColor(baseTimerColor)

                binding.maskTimerContainer.visibility = View.INVISIBLE

                if (maskListViewModel.masks.size > 0) {
                    binding.listEmpty.visibility = View.INVISIBLE
                    binding.instructions.visibility = View.VISIBLE
                } else {
                    binding.instructions.visibility = View.INVISIBLE
                    binding.listEmpty.visibility = View.VISIBLE
                }

                maskListViewModel.currentMask = null
            }

            maskListAdapter.notifyDataSetChanged()
        }
    }

    private fun onStopWearingMask(): Mask? {
        maskListViewModel.currentMask?.let {
            if (it.isBeingWorn) {
                alarmManager.cancelMaskAlarm(this)
                notificationManager.dismissMaskTimerExpiredNotification()

                it.stopWearing()
            }

            maskListViewModel.currentMask = null

            maskListViewModel.previousMask?.isPrevious = false
            maskListViewModel.previousMask = it
            it.isPrevious = true

            updateUi()

            return it
        }

        return null
    }

    private fun onMaskSelected(mask: Mask, unpausing: Boolean = false) {
        if (!unpausing && onStopWearingMask() == mask) {
            return
        }

        maskListViewModel.currentMask = mask

        if (mask == maskListViewModel.previousMask) {
            maskListViewModel.previousMask = null
        }

        mask.startWearing()
        maskListAdapter.notifyDataSetChanged()

        alarmManager.setAlarmForMask(this, mask)

        updateUi()
    }

    override fun onStop() {
        super.onStop()

        localBroadcastManager.unregisterReceiver(broadcastReceiver)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        maskListViewModel.currentMask?.let {
            notificationManager.createOrUpdateMaskTimerNotification(this, it, maskListViewModel.previousMask)
        }
    }

    override fun onStart() {
        super.onStart()

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        localBroadcastManager.registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(ACTION_REPLACE)
            addAction(ACTION_STOP_WEARING)
            addAction(ACTION_DELETE)
        })

        notificationManager.dismissMaskTimerNotification()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Preferences.SURGICAL_MASK_EXPIRATION_HOURS, Preferences.FFP_MASK_EXPIRATION_HOURS -> {
                maskListViewModel.currentMask?.let {
                    if ((key == Preferences.SURGICAL_MASK_EXPIRATION_HOURS && it.type == MaskTypes.SURGICAL) ||
                            key == Preferences.FFP_MASK_EXPIRATION_HOURS && it.type == MaskTypes.FFP) {
                        if (!it.isExpired(this)) {
                            notificationManager.dismissMaskTimerExpiredNotification()
                        }

                        alarmManager.setAlarmForMask(this, it)
                    }
                }
            }
        }
    }
}
