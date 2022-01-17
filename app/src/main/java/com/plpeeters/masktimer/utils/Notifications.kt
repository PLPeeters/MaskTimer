package com.plpeeters.masktimer.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.plpeeters.masktimer.*
import com.plpeeters.masktimer.data.Mask


const val MASK_TIMER_NOTIFICATION_CHANNEL_ID = "MASK_TIMER"
const val MASK_EXPIRY_NOTIFICATION_CHANNEL_ID = "MASK_EXPIRY"
const val MASK_TIMER_NOTIFICATION_ID = 1
const val MASK_TIMER_EXPIRED_NOTIFICATION_ID = 2


fun NotificationManager.createNotificationChannels(context: Context) {
    NotificationChannel(
        MASK_TIMER_NOTIFICATION_CHANNEL_ID,
        context.resources.getString(R.string.mask_timer_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.resources.getString(R.string.mask_timer_channel_description)
        setShowBadge(false)
        enableLights(false)
        enableVibration(false)
    }.let {
        createNotificationChannel(it)
    }

    NotificationChannel(
        MASK_EXPIRY_NOTIFICATION_CHANNEL_ID,
        context.resources.getString(R.string.mask_expiry_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.resources.getString(R.string.mask_expiry_channel_description)
        setShowBadge(true)
        enableLights(true)
        lightColor = Color.GREEN
        enableVibration(true)
    }.let {
        createNotificationChannel(it)
    }
}

@SuppressLint("UnspecifiedImmutableFlag")
fun NotificationManager.sendMaskTimerExpiredNotification(context: Context) {
    val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(
            context,
            MASK_TIMER_EXPIRED_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    } else {
        PendingIntent.getActivity(
            context,
            MASK_TIMER_EXPIRED_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    val stopWearingIntent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(STOP_WEARING_EXTRA, true)
    }
    val stopWearingPendingIntent = PendingIntent.getBroadcast(context, 0, stopWearingIntent, PendingIntent.FLAG_IMMUTABLE)
    val replaceIntent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(REPLACE_EXTRA, true)
    }
    val replacePendingIntent = PendingIntent.getBroadcast(context, 1, replaceIntent, PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(context, MASK_EXPIRY_NOTIFICATION_CHANNEL_ID).apply {
        priority = NotificationCompat.PRIORITY_MAX
        setCategory(NotificationCompat.CATEGORY_ALARM)
        setContentIntent(pendingIntent)
        setAutoCancel(true)
        setLocalOnly(false)
        setOngoing(false)
        setSmallIcon(R.mipmap.ic_launcher_foreground_trimmed)
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(context.resources.getString(R.string.change_your_mask))

        addAction(NotificationCompat.Action(null, context.resources.getString(R.string.stop_wearing), stopWearingPendingIntent))
        addAction(NotificationCompat.Action(null, context.resources.getString(R.string.replace), replacePendingIntent))
    }.build()

    notify(MASK_TIMER_EXPIRED_NOTIFICATION_ID, notification)
}

fun NotificationManager.dismissMaskTimerExpiredNotification() {
    cancel(MASK_TIMER_EXPIRED_NOTIFICATION_ID)
}

@SuppressLint("UnspecifiedImmutableFlag")
fun NotificationManager.createOrUpdateMaskTimerNotification(context: Context, mask: Mask) {
    val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(
            context,
            MASK_TIMER_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    } else {
        PendingIntent.getActivity(
            context,
            MASK_TIMER_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    val stopWearingPendingIntent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(STOP_WEARING_EXTRA, true)
    }.let {
        PendingIntent.getBroadcast(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
    }

    val replacePendingIntent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
        putExtra(REPLACE_EXTRA, true)
    }.let {
        PendingIntent.getBroadcast(context, 1, it, PendingIntent.FLAG_IMMUTABLE)
    }

    val notification = NotificationCompat.Builder(context, MASK_TIMER_NOTIFICATION_CHANNEL_ID).apply {
        priority = NotificationCompat.PRIORITY_HIGH
        setCategory(NotificationCompat.CATEGORY_ALARM)
        setContentIntent(pendingIntent)
        setAutoCancel(true)
        setLocalOnly(false)
        setOngoing(true)
        setSilent(true)
        setSmallIcon(R.mipmap.ic_launcher_foreground_trimmed)
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setContentTitle(context.resources.getString(R.string.wearing_your_mask, mask.name, mask.getDisplayType(context)))
        setWhen(mask.getExpirationTimestamp(context))
        setChronometerCountDown(true)
        setUsesChronometer(true)
        setOnlyAlertOnce(true)

        addAction(NotificationCompat.Action(null, context.resources.getString(R.string.stop_wearing), stopWearingPendingIntent))
        addAction(NotificationCompat.Action(null, context.resources.getString(R.string.replace), replacePendingIntent))
    }.build()

    notify(MASK_TIMER_NOTIFICATION_ID, notification)
}

fun NotificationManager.dismissMaskTimerNotification() {
    cancel(MASK_TIMER_NOTIFICATION_ID)
}
