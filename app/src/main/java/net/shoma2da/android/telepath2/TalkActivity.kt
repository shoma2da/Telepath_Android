package net.shoma2da.android.telepath2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.PowerManager
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import dmax.dialog.SpotsDialog
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

class TalkActivity : AppCompatActivity() {

    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var token: String? = null

    private lateinit var sensorManager: SensorManager
    private lateinit var wakeLock: PowerManager.WakeLock

    val recorder = MediaRecorder()

    private val dialog by lazy {
        SpotsDialog(this).apply { setCancelable(false) }
    }

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
    private var targetPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk)

        val targetPhoneNumber = intent.getStringExtra("phone_number")
        this.targetPhoneNumber = targetPhoneNumber

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

    override fun onResume() {
        super.onResume()
    }

    private fun onNearFace() {
        if (haveNear) {
            return
        }

        Log.d("test", "Near!!")

        //バイブで知らせる
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(longArrayOf(0, 200), -1)

        //録音
        recorder.reset()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
        val filePath = "${filesDir.absolutePath}/audio.amr"
        recorder.setOutputFile(filePath)
        recorder.prepare()
        recorder.start()

        haveNear = true
    }

    private fun onFarFace() {
        Log.d("test", "Far!!")

        if (haveNear) {
            haveNear = false

            //録音終了
            recorder.stop()
            recorder.reset()
            recorder.release()

            //バイブで知らせる
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(longArrayOf(0, 100, 100, 100), -1)

            dialog.show()

            //ファイルアップロード
            val storageRef = storage.getReferenceFromUrl("gs://telepath2-96608.appspot.com")
            storageRef
                    .child("voices/${DateTime().toString(ISODateTimeFormat.basicDateTime())}.amr")
                    .putStream(FileInputStream(File("${filesDir.absolutePath}/audio.amr")))
                    .addOnSuccessListener {
                        val url = it.downloadUrl
                        val targetToken = token
                        if (url != null && targetToken != null) {
                            storeTalkHistory(url.toString())
                            sendPush(url.toString(), targetToken)
                        }
                    }
        }
    }

    private fun storeTalkHistory(fileUrl: String) {
        val key = database.getReference("talks").push().key
        database.getReference("talks")
                .child(key)
                .setValue(mapOf(
                        "from" to myPhoneNumber,
                        "to" to targetPhoneNumber,
                        "voice" to fileUrl,
                        "timestamp" to ServerValue.TIMESTAMP
                ))
    }

    private fun sendPush(fileUrl: String, targetToken: String) {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val myName = pref.getString("name", "NO_NAME")
        val myToken = pref.getString("token", "")
        val myPhoneNumber = pref.getString("phone_number", "")

        val client = AsyncHttpClient()
        client.addHeader("Authorization", "key=AAAA9srKnfw:APA91bF5KT3hgrnLprTiVxeei0uLpOQK7LZJtKg3TDupcNPwqNneZVQhNGtUURZotjjiE8C_9NprHbACUXUJv9NlS1lMBRRZr99yWEX4n8KDlmYIXCvvHywyUdO_lyk9A654kRfRa9NZ")
        val parameter = JSONObject(mapOf("to" to targetToken, "data" to mapOf(
                "name" to myName,
                "token" to myToken,
                "phone_number" to myPhoneNumber,
                "voice_url" to fileUrl
        ))).toString(2)

        Log.d("test", parameter)
        client.post(this, "https://fcm.googleapis.com/fcm/send", StringEntity(parameter, "UTF-8"), "application/json", object : AsyncHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<out Header>?, responseBody: ByteArray?) {
                Log.d("test", "Success: $statusCode")
            }

            override fun onFailure(statusCode: Int, headers: Array<out Header>?, responseBody: ByteArray?, error: Throwable?) {
                Log.d("test", "Failure: $statusCode")

            }
        })

        Toast.makeText(this, "送信しました", Toast.LENGTH_SHORT).show()

        dialog.dismiss()
        finish()
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
