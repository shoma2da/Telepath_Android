package net.shoma2da.android.telepath2

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Vibrator
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        if (remoteMessage == null) {
            return
        }

        val name = remoteMessage.data["name"]
        val token = remoteMessage.data["token"]
        val phoneNumber = remoteMessage.data["phone_number"]
        val voiceUrl = remoteMessage.data["voice_url"]

        val notificationIntent = Intent(this, ListenActivity::class.java)
        notificationIntent.putExtra(ListenActivity.KEY_TARGET_NAME, name)
        notificationIntent.putExtra(ListenActivity.KEY_TARGET_TOKEN, token)
        notificationIntent.putExtra(ListenActivity.KEY_TARGET_VOICE_URL, voiceUrl)
        notificationIntent.putExtra(ListenActivity.KEY_TARGET_PHONE_NUMBER, phoneNumber)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this)
                .setContentTitle("${name}さんからメッセージが届きました")
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setAutoCancel(true)
                .setPriority(Integer.MAX_VALUE)
                .setColor(resources.getColor(R.color.colorAccent))
                .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(10, notification)

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(longArrayOf(0, 200, 200, 200, 300, 100, 100, 100), -1)
    }

}
