package com.plpeeters.masktimer

import android.app.AlarmManager
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceFragmentCompat
import com.plpeeters.masktimer.data.Data
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.data.persistence.MaskTypes
import com.plpeeters.masktimer.utils.createOrUpdateMaskTimerNotification
import com.plpeeters.masktimer.utils.dismissMaskTimerExpiredNotification
import com.plpeeters.masktimer.utils.getSharedPreferences
import com.plpeeters.masktimer.utils.setAlarmForMask


class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val sharedPreferences: SharedPreferences by lazy { getSharedPreferences() }
    private val notificationManager: NotificationManagerCompat by lazy { NotificationManagerCompat.from(this) }
    private val alarmManager: AlarmManager by lazy { getSystemService(AlarmManager::class.java)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            Preferences.SURGICAL_MASK_EXPIRATION_HOURS, Preferences.FFP_MASK_EXPIRATION_HOURS -> {
                var currentMask: Mask? = null
                var previousMask: Mask? = null

                for (mask in Data.MASKS) {
                    if (mask.isBeingWorn) {
                        currentMask = mask

                        if (previousMask != null) {
                            break
                        }
                    } else if (mask.isPrevious) {
                        previousMask = mask

                        if (currentMask != null) {
                            break
                        }
                    }
                }

                currentMask?.let {
                    if ((key == Preferences.SURGICAL_MASK_EXPIRATION_HOURS && currentMask.type == MaskTypes.SURGICAL) ||
                            key == Preferences.FFP_MASK_EXPIRATION_HOURS && currentMask.type == MaskTypes.FFP) {
                        if (!currentMask.isExpired(this)) {
                            notificationManager.dismissMaskTimerExpiredNotification()
                        }

                        notificationManager.createOrUpdateMaskTimerNotification(this, currentMask, previousMask)
                        alarmManager.setAlarmForMask(this, currentMask)
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
