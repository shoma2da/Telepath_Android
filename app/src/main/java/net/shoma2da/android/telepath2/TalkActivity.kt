package net.shoma2da.android.telepath2

import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class TalkActivity : AppCompatActivity() {

    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val myPhoneNumber by lazy {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.getString("phone_number", "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_talk)

        val targetPhoneNumber = intent.getStringExtra("phone_number")
    }

}
