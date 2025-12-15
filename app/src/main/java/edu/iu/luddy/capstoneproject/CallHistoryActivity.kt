package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CallHistoryActivity : AppCompatActivity() {

    data class HistoryItem(
        val id: String,
        val label: String,
        val dateTime: String,
        val passed: Boolean?,
        val createdAt: Long
    )

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var titleTv: TextView
    private lateinit var recycler: RecyclerView

    private lateinit var dataSet: MutableList<HistoryItem>
    private lateinit var adapter: CallHistoryAdapter

    private var callId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        callId = intent.getStringExtra("CALL_ID")
        if (callId.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("History Error")
                .setMessage("Missing CALL_ID. Please open History from SavedCallActivity.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
            return
        }

        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener { finish() }

        titleTv = findViewById(R.id.title)
        recycler = findViewById(R.id.call_history_recyclerview)

        dataSet = mutableListOf()
        adapter = CallHistoryAdapter(dataSet) { item, action ->
            when (action) {
                "navigate" -> {
                    val i = Intent(this, CompareCallActivity::class.java)
                    i.putExtra("CALL_ID", callId)
                    i.putExtra("HISTORY_ID", item.id)
                    i.putExtra("DATE_TIME", item.dateTime)
                    startActivity(i)
                }

                "delete" -> {
                    AlertDialog.Builder(this)
                        .setTitle("Delete this history entry?")
                        .setMessage("This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            deleteHistory(uid, callId!!, item.id)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        db.collection("users")
            .document(uid)
            .collection("savedCalls")
            .document(callId!!)
            .get()
            .addOnSuccessListener { doc ->
                val callName = doc.getString("name").orEmpty().ifBlank { "API Call" }
                titleTv.text = "$callName - History"
            }
            .addOnFailureListener {
                titleTv.text = "History"
            }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid ?: return
        loadHistory(uid, callId!!)
    }

    private fun historyCollection(uid: String, callId: String) =
        db.collection("users")
            .document(uid)
            .collection("savedCalls")
            .document(callId)
            .collection("history")

    private fun loadHistory(uid: String, callId: String) {
        historyCollection(uid, callId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                dataSet.clear()

                for (doc in snap.documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    val dateTime = doc.getString("dateTime") ?: doc.getString("datetime") ?: ""
                    val passed: Boolean? = doc.getBoolean("passed")

                    dataSet.add(
                        HistoryItem(
                            id = doc.id,
                            label = doc.getString("label") ?: "",
                            dateTime = dateTime,
                            passed = passed,
                            createdAt = createdAt
                        )
                    )
                }
                adapter.notifyDataSetChanged()

            }
            .addOnFailureListener { e ->
                AlertDialog.Builder(this)
                    .setTitle("Load failed")
                    .setMessage(e.localizedMessage ?: "Could not load call history.")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }

    private fun deleteHistory(uid: String, callId: String, historyId: String) {
        historyCollection(uid, callId)
            .document(historyId)
            .delete()
            .addOnSuccessListener {
                loadHistory(uid, callId)
            }
            .addOnFailureListener { e ->
                AlertDialog.Builder(this)
                    .setTitle("Delete failed")
                    .setMessage(e.localizedMessage ?: "Could not delete history entry.")
                    .setPositiveButton("OK", null)
                    .show()
            }
    }
}
