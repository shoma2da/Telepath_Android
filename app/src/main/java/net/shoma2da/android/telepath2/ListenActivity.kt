package net.shoma2da.android.telepath2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_listen.*

class ListenActivity : Activity() {

    companion object {
        val KEY_TARGET_NAME = "target_name"
        val KEY_TARGET_TOKEN = "target_token"
        val KEY_TARGET_VOICE_URL = "target_voice_url"
        val KEY_TARGET_PHONE_NUMBER = "target_phone_number"
    }

    private var voiceUrl = ""

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var mediaPlayer: MediaPlayer

    private var haveNear = false

    private val sensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.d("test", "onAccuracyChanged: $accuracy")
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                val value = event!!.values[0]!!
                if (value >= -0.01 && value <= 0.01) {
                    onNearFace()
                } else {
                    Log.d("test", "++++++++ value is $value")
                    onFarFace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listen)

        val name = intent.getStringExtra(KEY_TARGET_NAME)
        val phoneNumber = intent.getStringExtra(KEY_TARGET_PHONE_NUMBER)
        voiceUrl = intent.getStringExtra(KEY_TARGET_VOICE_URL)

        description_text.text = description_text.text.toString().replace("○○", name)

        history_button.setOnClickListener {
            Toast.makeText(this, "未実装です...", Toast.LENGTH_SHORT).show()
        }

        reply_button.setOnClickListener {
            val intent = Intent(this, TalkActivity::class.java).apply {
                putExtra("phone_number", phoneNumber)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        setupProximitySensor()
    }

    private fun setupProximitySensor() {
        // 近接センサー
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(sensorEventListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)

        // 顔が近づいたら画面を消す
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
        wakeLock.acquire()
    }

    private fun onNearFace() {
        if (haveNear) {
            return
        }

        Log.d("test", "Near!!")

        //バイブで知らせる
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(longArrayOf(0, 200), -1)

        //再生開始
        playAudio(voiceUrl)

        haveNear = true
    }

    private fun playAudio(url: String) {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(url)
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
        mediaPlayer.prepare()
        mediaPlayer.setOnCompletionListener {
            onFarFace()
        }
        mediaPlayer.start()
    }

    private fun onFarFace() {
        Log.d("test", "Far!!")

        if (haveNear) {
            haveNear = false

            //再生終了
            mediaPlayer.stop()
            Toast.makeText(this, "再生が終了しました", Toast.LENGTH_SHORT).show()

            //バイブで知らせる
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)
        }
    }

    override fun onPause() {
        super.onPause()
        wakeLock.release()
        sensorManager.unregisterListener(sensorEventListener)
    }
}
