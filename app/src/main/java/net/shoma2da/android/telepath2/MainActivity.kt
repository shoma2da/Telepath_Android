package net.shoma2da.android.telepath2

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val name = pref.getString("name", "")
        val phoneNumber = pref.getString("phone_number", "")

        if (name.isEmpty() || phoneNumber.isEmpty()) {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}
