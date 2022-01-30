package com.plpeeters.masktimer.utils

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.EditText
import androidx.preference.PreferenceManager
import com.plpeeters.masktimer.AlarmReceiver
import com.plpeeters.masktimer.MASK_ALARM_REQUEST_CODE
import com.plpeeters.masktimer.R
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

private fun Context.getMaskAlarmPendingIntent(): PendingIntent {
    return Intent(applicationContext, AlarmReceiver::class.java).let {
        PendingIntent.getBroadcast(applicationContext, MASK_ALARM_REQUEST_CODE, it, PendingIntent.FLAG_IMMUTABLE)
    }
}

fun AlarmManager.checkAndRequestExactAlarmsPermissionIfNecessary(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms()) {
        AlertDialog.Builder(context).simpleDialog(null, R.string.alarm_permission_request, R.string.ok) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }.show()

        return false
    }

    return true
}

fun AlarmManager.setAlarmForMask(context: Context, mask: Mask) {
    val pendingIntent = context.getMaskAlarmPendingIntent()

    if (checkAndRequestExactAlarmsPermissionIfNecessary(context)) {
        setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + mask.getRemainingLifespanMillis(context),
            pendingIntent
        )
    }
}

fun AlarmManager.cancelMaskAlarm(context: Context) {
    val pendingIntent = context.getMaskAlarmPendingIntent()

    cancel(pendingIntent)
}
