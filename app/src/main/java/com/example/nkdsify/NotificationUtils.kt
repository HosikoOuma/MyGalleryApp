package com.example.nkdsify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

fun showUpdateNotification(context: Context, newVersion: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("update_channel", "Update Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, "update_channel")
        .setContentTitle(context.getString(R.string.update_available_title))
        .setContentText(context.getString(R.string.update_available_text, newVersion))
        .setSmallIcon(R.mipmap.ic_launcher) // Используем иконку приложения
        .build()

    notificationManager.notify(1, notification)
}
