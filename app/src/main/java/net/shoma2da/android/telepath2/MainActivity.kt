package net.shoma2da.android.telepath2

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val instanceId = FirebaseInstanceId.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 権限
        //if (!checkOverlayPermission()) { requestOverlayPermission() }
        val needPermissionList =
                arrayOf(android.Manifest.permission.RECORD_AUDIO)
                        .filter { ContextCompat.checkSelfPermission(this, it) !== PackageManager.PERMISSION_GRANTED }
                        .toTypedArray()
        if (!needPermissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, needPermissionList, 0)
        }

        fab.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val name = pref.getString("name", "")
        val phoneNumber = pref.getString("phone_number", "")

        //サインアップ？
        if (name.isEmpty() || phoneNumber.isEmpty()) {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        //プッシュ用Token準備
        updateToken(phoneNumber)

        //リスト管理
        val adapter = ArrayAdapter<User>(this, android.R.layout.simple_list_item_1)
        list_view.adapter = adapter
        database.getReference("/users/$phoneNumber")
                .child("friends").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError?) {
            }

            override fun onDataChange(data: DataSnapshot?) {
                if (data == null || data.value == null) {
                    return
                }

                val friendsMap = data.value as HashMap<String, String>
                friendsMap.keys.forEach {
                    database.getReference("users")
                            .child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?) {
                        }

                        override fun onDataChange(data: DataSnapshot?) {
                            if (data == null || data.value == null) {
                                return
                            } else {
                                val user = data.getValue(User::class.java)
                                user.phoneNumber = it
                                adapter.add(user)
                            }
                        }
                    })
                }
            }
        })

        //リストクリック時の挙動
        list_view.setOnItemClickListener { parent, view, position, id ->
            val user = adapter.getItem(position)
            val intent = Intent(this, TalkActivity::class.java).apply {
                putExtra("phone_number", user.phoneNumber)
            }
            startActivity(intent)
        }

        //TODO リスト長押しでショートカット作成、履歴確認、友だちから削除（ダミーで良い）
        list_view.setOnItemLongClickListener { parent, view, position, id ->
            Toast.makeText(this@MainActivity, "ショートカット・履歴確認など、メニューを出す？？", Toast.LENGTH_SHORT).show()
            true
        }
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

class User(var name: String? = null,
           var token: String? = null,
           var phoneNumber: String? = null) {
    override fun toString() = name ?: "NO_NAME"
}
