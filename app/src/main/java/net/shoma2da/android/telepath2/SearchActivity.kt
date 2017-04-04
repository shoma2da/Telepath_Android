package net.shoma2da.android.telepath2

import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_search.*
import java.util.HashMap

class SearchActivity : AppCompatActivity() {

    private val database = FirebaseDatabase.getInstance()

    private val myPhoneNumber by lazy {
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        pref.getString("phone_number", "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        title = "電話番号で検索"

        hiddenResult()

        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query == myPhoneNumber) {
                    Toast.makeText(this@SearchActivity, "自分の電話番号です", Toast.LENGTH_SHORT).show()
                    return false
                }

                readDatabase(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                hiddenResult()
                return false
            }
        })
    }

    private fun hiddenResult() {
        not_found_text.visibility = View.GONE
        profile_image.visibility = View.GONE
        name_text.visibility = View.GONE
        add_button.visibility = View.GONE
    }

    private fun readDatabase(query: String?) {
        val reference = database.getReference("users")
        reference.child(query).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError?) {
            }

            override fun onDataChange(data: DataSnapshot?) {
                if (data == null || data.value == null) {
                    not_found_text.visibility = View.VISIBLE
                } else {
                    profile_image.visibility = View.VISIBLE
                    name_text.visibility = View.VISIBLE
                    add_button.visibility = View.VISIBLE

                    val map = data.value as HashMap<String, String>
                    name_text.text = map["name"]

                    add_button.setOnClickListener {
                        val friends = database.getReference("users/$myPhoneNumber/friends")
                        friends.child(query).setValue("NO_MEAN_VALUE")
                                .addOnSuccessListener {
                                    finish()
                                }

                        Toast.makeText(this@SearchActivity, "${map["name"]} さんを追加しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
