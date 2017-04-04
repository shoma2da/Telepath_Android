package net.shoma2da.android.telepath2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class TalkActivity : AppCompatActivity() {

    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var token: String? = null

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock

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

    private var haveNear = false

    private val myPhoneNumber by lazy {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.getString("phone_number", "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk)

        val targetPhoneNumber = intent.getStringExtra("phone_number")

        val reference = database.getReference("users")
        reference.child(targetPhoneNumber).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError?) {
            }

            override fun onDataChange(data: DataSnapshot?) {
                if (data == null) {
                    return
                }

                val map = data.value as HashMap<String, String>
                token = map["token"]
                Log.d("test", "token is $token")
            }
        })

        setupProximitySensor()
    }

    // 近接センサー
    private fun setupProximitySensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(sensorEventListener, proximity, SensorManager.SENSOR_DELAY_NORMAL)

        // 顔が近づいたら画面を消す
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
        wakeLock.acquire()
    }

    override fun onResume() {
        super.onResume()
    }

    private fun onNearFace() {
        Log.d("test", "Near!!")

        //バイブで知らせる
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(longArrayOf(0, 200), -1)

        haveNear = true
    }

    private fun onFarFace() {
        Log.d("test", "Far!!")

        if (haveNear) {
            haveNear = false

            //バイブで知らせる
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)

            //TODO 送信しました→画面終了
        }
    }

    override fun onPause() {
        super.onPause()
        wakeLock.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorEventListener)
    }
}
