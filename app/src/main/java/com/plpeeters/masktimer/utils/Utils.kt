package com.plpeeters.masktimer.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.SystemClock
import android.widget.EditText
import androidx.preference.PreferenceManager
import com.plpeeters.masktimer.AlarmReceiver
import com.plpeeters.masktimer.data.Mask
import java.text.Normalizer


private val UNIDECODE_REGEX = """\p{InCombiningDiacriticalMarks}+""".toRegex()

fun CharSequence.unidecode(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)

    return UNIDECODE_REGEX.replace(temp, "")
}

fun CharSequence.normalize(): String {
    return unidecode().lowercase().trim()
}

fun EditText.getTrimmedText(): String {
    return text.toString().trim()
}

fun Context.getVersionName(): String? {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)

    return packageInfo?.versionName
}

fun Context.getSharedPreferences(): SharedPreferences {
    return PreferenceManager.getDefaultSharedPreferences(this)
}

private object MaskAlarmPendingIntent {
    var intent: PendingIntent? = null
}

private fun ensureMaskAlarmPendingIntent(context: Context) {
    if (MaskAlarmPendingIntent.intent == null) {
        MaskAlarmPendingIntent.intent = Intent(context.applicationContext, AlarmReceiver::class.java).let {
            PendingIntent.getBroadcast(context.applicationContext, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

fun AlarmManager.setAlarmForMask(context: Context, mask: Mask) {
    ensureMaskAlarmPendingIntent(context)

    set(
        AlarmManager.ELAPSED_REALTIME_WAKEUP,
        SystemClock.elapsedRealtime() + mask.getRemainingLifespanMillis(context),
        MaskAlarmPendingIntent.intent
    )
}

fun AlarmManager.cancelMaskAlarm(context: Context) {
    ensureMaskAlarmPendingIntent(context)

    cancel(MaskAlarmPendingIntent.intent)
}
