package com.plpeeters.masktimer

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.daimajia.swipe.adapters.ArraySwipeAdapter
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.databinding.MaskListItemBinding
import com.plpeeters.masktimer.utils.cancelMaskAlarm
import com.plpeeters.masktimer.utils.dismissMaskTimerExpiredNotification
import com.plpeeters.masktimer.utils.durationDialog
import com.plpeeters.masktimer.utils.setAlarmForMask
import kotlin.math.roundToInt


class MaskListAdapter(
    context: Context, @LayoutRes
    private val layoutResource: Int,
    private var masks: ArrayList<Mask>
): ArraySwipeAdapter<Mask>(context, layoutResource, masks) {
    private var baseTextColor: Int = -1
    private val notificationManager: NotificationManager by lazy { context.getSystemService(NotificationManager::class.java) }
    private val alarmManager: AlarmManager by lazy { context.getSystemService(AlarmManager::class.java)}
    private val localBroadcastManager by lazy { LocalBroadcastManager.getInstance(context) }

    private fun updateUi() {
        if (context is MainActivity) {
            (context as MainActivity).updateUi()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val mask = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as View

        val binding = MaskListItemBinding.bind(view)
        val maskType = mask.getDisplayType(context)

        binding.adjustWearTimeButton.setOnClickListener {
            AlertDialog.Builder(context).durationDialog(mask) { durationSeconds, adding ->
                if (adding) {
                    mask.addWearTime(durationSeconds)
                } else {
                    mask.subtractWearTime(durationSeconds)
                }

                if (mask.isBeingWorn) {
                    if (!mask.isExpired(context)) {
                        notificationManager.dismissMaskTimerExpiredNotification()
                    }

                    alarmManager.setAlarmForMask(context, mask)
                }

                updateUi()
                notifyDataSetChanged()
                binding.swipeContainer.close(true)
            }.show()
        }
        binding.adjustWearTimeButton.setOnLongClickListener {
            Toast.makeText(context, R.string.adjust_wear_time, Toast.LENGTH_SHORT).show()

            true
        }
        binding.replaceButton.setOnClickListener {
            notificationManager.dismissMaskTimerExpiredNotification()
            mask.replace()
            alarmManager.setAlarmForMask(context, mask)

            updateUi()
            notifyDataSetChanged()
            binding.swipeContainer.close(true)
        }
        binding.replaceButton.setOnLongClickListener {
            Toast.makeText(context, R.string.replace, Toast.LENGTH_SHORT).show()

            true
        }
        binding.deleteButton.setOnClickListener {
            mask.isBeingWorn.let { wasBeingWorn ->
                mask.delete()
                masks.remove(mask)

                if (wasBeingWorn) {
                    notificationManager.dismissMaskTimerExpiredNotification()
                    alarmManager.cancelMaskAlarm(context)
                }

                localBroadcastManager.sendBroadcast(Intent(ACTION_DELETE).apply {
                    putExtra(MASK_EXTRA, mask)
                })
            }

            binding.swipeContainer.close(true)
            updateUi()
            notifyDataSetChanged()
        }
        binding.deleteButton.setOnLongClickListener {
            Toast.makeText(context, R.string.delete, Toast.LENGTH_SHORT).show()

            true
        }

        binding.maskNameAndType.text = context.resources.getString(R.string.mask_name_and_type, mask.name, maskType)

        if (mask.isBeingWorn || mask.isPaused) {
            binding.wornDuration.text = context.resources.getString(R.string.currently_wearing)
        } else {
            val wornTimeSeconds = mask.wornTimeMillis / 1000

            // Subtracting 30 seconds for the check to handle rounding of 59m30+ to 1h
            if (wornTimeSeconds >= (3600 - 30)) {
                var wornHours = wornTimeSeconds.floorDiv(3600).toInt()
                var wornMinutes = (wornTimeSeconds % 3600 / 60.0).roundToInt()

                if (wornMinutes == 60) {
                    wornHours++
                    wornMinutes = 0
                }

                if (wornMinutes > 0) {
                    binding.wornDuration.text = context.resources.getString(R.string.worn_for_and,
                        context.resources.getQuantityString(R.plurals.hours, wornHours, wornHours),
                        context.resources.getQuantityString(R.plurals.minutes, wornMinutes, wornMinutes)
                    )
                } else {
                    val wornHoursString = context.resources.getQuantityString(R.plurals.hours, wornHours, wornHours)
                    binding.wornDuration.text = context.resources.getString(R.string.worn_for, wornHoursString)
                }
            } else if (wornTimeSeconds >= 60) {
                val wornMinutes = (wornTimeSeconds / 60.0).roundToInt()

                binding.wornDuration.text = context.resources.getString(R.string.worn_for, context.resources.getQuantityString(R.plurals.minutes, wornMinutes, wornMinutes))
            } else if (wornTimeSeconds > 0) {
                binding.wornDuration.text = context.resources.getString(R.string.worn_for_less_than_a_minute)
            } else {
                binding.wornDuration.text = context.resources.getString(R.string.never_worn)
            }
        }

        if (baseTextColor == -1) {
            baseTextColor = binding.wornDuration.textColors.defaultColor
        }

        if (mask.isExpired(context)) {
            binding.wornDuration.setTextColor(context.resources.getColor(R.color.timer_expired_color, context.theme))
            binding.wornDuration.setTypeface(binding.wornDuration.typeface, Typeface.BOLD)
        } else {
            binding.wornDuration.setTextColor(baseTextColor)
            binding.wornDuration.setTypeface(binding.wornDuration.typeface, Typeface.ITALIC)
        }

        return view
    }

    override fun getItem(position: Int) = masks[position]

    override fun getCount() = masks.size

    override fun getSwipeLayoutResourceId(position: Int): Int {
        return masks[position].hashCode()
    }
}
