package com.plpeeters.masktimer

import android.app.AlarmManager
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.view.ContextMenu
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AdapterView
import androidx.lifecycle.ViewModelProvider
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.data.persistence.MaskDao
import com.plpeeters.masktimer.data.persistence.MaskDatabaseSingleton
import com.plpeeters.masktimer.databinding.ActivityMainBinding
import com.plpeeters.masktimer.utils.addMaskDialog
import com.plpeeters.masktimer.utils.dismissMaskTimerExpiredNotification
import com.plpeeters.masktimer.utils.dismissMaskTimerNotification
import com.plpeeters.masktimer.utils.createOrUpdateMaskTimerNotification
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var maskListAdapter: MaskListAdapter
    private lateinit var maskListViewModel: MaskListViewModel
    private val maskDatabaseDao: MaskDao by lazy { MaskDatabaseSingleton(application).maskDatabaseDao() }
    private val notificationManager: NotificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val alarmManager: AlarmManager by lazy { getSystemService(AlarmManager::class.java)}
    private val alarmPendingIntent: PendingIntent by lazy {
        Intent(this, AlarmReceiver::class.java).let {
            PendingIntent.getBroadcast(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
    }
    private val blinkingAnimation = AlphaAnimation(0F, 1F).apply {
        duration = 250
        startOffset = 0
        repeatMode = Animation.REVERSE
        repeatCount = Animation.INFINITE
    }
    private var baseTimerColor: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.maskTimerContainer.visibility = View.INVISIBLE
        setContentView(binding.root)

        baseTimerColor = binding.maskTimer.textColors.defaultColor

        maskListViewModel = ViewModelProvider(this).get(MaskListViewModel::class.java)
        maskListAdapter = MaskListAdapter(
            this,
            R.layout.mask_list_item,
            maskListViewModel.masks)

        if (maskListViewModel.masks.size > 0) {
            binding.listEmpty.visibility = View.INVISIBLE
            binding.maskList.visibility = View.VISIBLE
            binding.instructions.visibility = View.VISIBLE
        } else {
            binding.maskList.visibility = View.INVISIBLE
            binding.instructions.visibility = View.INVISIBLE
            binding.listEmpty.visibility = View.VISIBLE
        }

        binding.maskList.adapter = maskListAdapter
        binding.maskList.setOnItemClickListener { _, _, position, _ ->
            onMaskSelected(maskListAdapter.getItem(position))
        }

        registerForContextMenu(binding.maskList)

        binding.stopWearingButton.setOnClickListener {
            onStopWearingMask()
        }

        binding.replaceButton.setOnClickListener {
            maskListViewModel.currentMask?.let {
                replaceCurrentMask()
            }
        }

        binding.fab.setOnClickListener {
            AlertDialog.Builder(this).addMaskDialog(maskListViewModel.masks) { type: String, name: String ->
                val mask = Mask(type, name)

                if (binding.instructions.visibility != View.VISIBLE) {
                    binding.listEmpty.visibility = View.INVISIBLE
                    binding.maskList.visibility = View.VISIBLE
                    binding.instructions.visibility = View.VISIBLE
                }

                maskListViewModel.masks.add(mask)
                maskListAdapter.notifyDataSetChanged()

                Thread {
                    maskDatabaseDao.insert(mask.toMaskEntity())
                }.start()
            }.show()
        }
    }

    private fun updateCurrentMaskState() {
        maskListViewModel.currentMask?.let {
            if (it.wearingSince != null) {
                setupChronometerListener(it)
            } else {
                // Was stopped by the notification, this only updates our view
                onStopWearingMask()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        updateCurrentMaskState()
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
            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)

        if (v.id == binding.maskList.id) {
            val listPosition = (menuInfo as AdapterView.AdapterContextMenuInfo).position

            menu.add(0, listPosition, 0, R.string.replace)
            menu.add(0, listPosition, 1, R.string.delete)
        }
    }

    private fun replaceCurrentMask() {
        maskListViewModel.currentMask!!.let {
            onStopWearingMask()
            it.replace()
            onMaskSelected(it)
            notificationManager.dismissMaskTimerExpiredNotification()
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val listPosition = (item.menuInfo as AdapterView.AdapterContextMenuInfo).position
        val mask = maskListAdapter.getItem(listPosition)

        when (item.order) {
            0 -> {  // Replace
                if (maskListViewModel.currentMask?.equals(mask) == true) {
                    replaceCurrentMask()
                } else {
                    mask.replace()
                    maskListAdapter.notifyDataSetChanged()
                }

                return true
            }
            1 -> {  // Delete
                if (maskListViewModel.currentMask?.equals(mask) == true) {
                    onStopWearingMask()
                }

                maskListViewModel.masks.remove(mask)
                maskListAdapter.notifyDataSetChanged()

                if (maskListViewModel.masks.size == 0) {
                    binding.maskList.visibility = View.INVISIBLE
                    binding.instructions.visibility = View.INVISIBLE
                    binding.listEmpty.visibility = View.VISIBLE
                }

                Thread {
                    maskDatabaseDao.delete(mask.toMaskEntity())
                }.start()

                return true
            }
        }

        return super.onContextItemSelected(item)
    }

    private fun onStopWearingMask(): Mask? {
        maskListViewModel.currentMask?.let {
            alarmManager.cancel(alarmPendingIntent)

            binding.maskTimer.stop()
            binding.maskTimer.animation = null
            binding.maskTimer.setTextColor(baseTimerColor)

            if (it.wearingSince != null) {
                it.stopWearing()
            }

            maskListAdapter.notifyDataSetChanged()

            binding.maskTimerContainer.visibility = View.INVISIBLE
            binding.instructions.visibility = View.VISIBLE

            maskListViewModel.currentMask = null
            maskListViewModel.previousMask = it

            return it
        }

        return null
    }

    private fun setupChronometerListener(mask: Mask) {
        binding.maskTimer.base = SystemClock.elapsedRealtime() + (mask.getExpirationTimestamp() - Date().time)
        binding.instructions.visibility = View.INVISIBLE
        binding.maskTimerContainer.visibility = View.VISIBLE
        binding.wearingMask.text = resources.getString(R.string.wearing_your_mask, mask.name, mask.getDisplayType(this))
        binding.maskTimer.start()

        binding.maskTimer.setOnChronometerTickListener {
            val remainingMillis = it.base - SystemClock.elapsedRealtime()

            if (remainingMillis <= 0L) {
                binding.maskTimer.setTextColor(resources.getColor(R.color.timer_expired_color, theme))
                binding.maskTimer.startAnimation(blinkingAnimation)
            }
        }
    }

    private fun onMaskSelected(mask: Mask) {
        if (onStopWearingMask() == mask) {
            return
        }

        maskListViewModel.currentMask = mask
        mask.startWearing()
        maskListAdapter.notifyDataSetChanged()

        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + (mask.getExpirationTimestamp() - Date().time),
            alarmPendingIntent
        )

        setupChronometerListener(mask)
    }

    override fun onStop() {
        super.onStop()

        maskListViewModel.currentMask?.let {
            notificationManager.createOrUpdateMaskTimerNotification(this, it)
        }
    }

    override fun onStart() {
        super.onStart()

        notificationManager.dismissMaskTimerNotification()
    }
}
