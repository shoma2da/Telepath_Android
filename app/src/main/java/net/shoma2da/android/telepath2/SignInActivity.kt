package net.shoma2da.android.telepath2

import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance()

    private val dialog by lazy {
        SpotsDialog(this).apply { setCancelable(false) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        profile_image.setOnClickListener {
            Toast.makeText(this, "（画像は未対応です...）", Toast.LENGTH_SHORT).show()
        }

        create_account_button.setOnClickListener {
            val name = name_text.text.toString()
            val phoneNumber = phone_number_text.text.toString()

            if (name.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "値を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.show()

            database.getReference("users")
                    .child(phoneNumber)
                    .setValue(mapOf("name" to name))
                    .addOnSuccessListener {
                        val pref = PreferenceManager.getDefaultSharedPreferences(this)
                        pref.edit().putString("name", name).apply()
                        pref.edit().putString("phone_number", phoneNumber).apply()

                        dialog.dismiss()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "保存に失敗しました", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
        }
    }

}
