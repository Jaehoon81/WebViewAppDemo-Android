@file:JvmName("CommonUtils")

package kr.co.hoonproj.webviewappdemo.utils

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kr.co.hoonproj.webviewappdemo.R
import java.util.*

private const val TAG: String = "[WebViewAppDemo] CommonUtils"

private val NOTIFICATION_CHANNEL_NAME: CharSequence = "Status Notification"
private const val NOTIFICATION_CHANNEL_ID = "STATUS_NOTIFICATION"
private const val NOTIFICATION_ID = 99

fun makeNotification(
    context: Context, title: String, message: String, pendingIntent: PendingIntent? = null
): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentIntent(pendingIntent)
        .setContentTitle(title)
//        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setOngoing(false)
        .setAutoCancel(pendingIntent != null)
        .build()
}

fun showNotification(
    context: Context, title: String, message: String, pendingIntent: PendingIntent? = null,
    notificationId: Int = NOTIFICATION_ID
) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
        Log.w(TAG, "Permission is denied: POST_NOTIFICATIONS")
        return
    }
    val notification = makeNotification(context, title, message, pendingIntent)
    NotificationManagerCompat.from(context).notify(notificationId, notification)
}

fun sleep(delayTimeMillis: Long) {
    try {
        Thread.sleep(delayTimeMillis, 0)
    } catch (e: InterruptedException) {
        Log.e(TAG, e.stackTraceToString())
    }
}

fun finishApplication(activity: Activity) {
    activity.moveTaskToBack(true)
    activity.finishAndRemoveTask()
    activity.finish()
//    android.os.Process.killProcess(android.os.Process.myPid())
}