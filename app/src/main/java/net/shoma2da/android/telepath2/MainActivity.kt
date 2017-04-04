package net.shoma2da.android.telepath2

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val instanceId = FirebaseInstanceId.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val name = pref.getString("name", "")
        val phoneNumber = pref.getString("phone_number", "")

        if (name.isEmpty() || phoneNumber.isEmpty()) {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        updateToken(phoneNumber)
    }

    private fun updateToken(phoneNumber: String) {
        val reference = database.getReference("users")
        val token = instanceId.token
        val changedToken = mapOf("$phoneNumber/token" to token)
        reference.updateChildren(changedToken)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.edit().putString("token", token).apply()
    }
}
