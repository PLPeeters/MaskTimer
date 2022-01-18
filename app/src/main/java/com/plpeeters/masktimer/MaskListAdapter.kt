package com.plpeeters.masktimer

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.LayoutRes
import com.plpeeters.masktimer.data.Mask
import com.plpeeters.masktimer.databinding.MaskListItemBinding
import kotlin.math.roundToInt


class MaskListAdapter(
    context: Context, @LayoutRes
    private val layoutResource: Int,
    private var masks: List<Mask>
): ArrayAdapter<Mask>(context, layoutResource, masks) {
    private var baseTextColor: Int = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val mask = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as View

        val binding = MaskListItemBinding.bind(view)
        val maskType = mask.getDisplayType(context)

        binding.maskNameAndType.text = context.resources.getString(R.string.mask_name_and_type, mask.name, maskType)

        if (mask.isBeingWorn || mask.isPaused) {
            binding.wornDuration.text = context.resources.getString(R.string.currently_wearing)
        } else {
            val wornTimeSeconds = mask.wornTimeMillis / 1000

            if (wornTimeSeconds >= 3600) {
                val wornHours = wornTimeSeconds.floorDiv(3600).toInt()
                val wornMinutes = (wornTimeSeconds % 3600 / 60.0).roundToInt()

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
                val wornMinutes = (mask.wornTimeMillis / (60 * 1000.0)).roundToInt()

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
            binding.wornDuration.setTypeface(binding.wornDuration.typeface, Typeface.NORMAL)
        }

        return view
    }

    override fun getItem(position: Int) = masks[position]

    override fun getCount() = masks.size
}
