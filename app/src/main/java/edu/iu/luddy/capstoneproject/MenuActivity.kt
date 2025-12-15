package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MenuActivity : AppCompatActivity() {

    private lateinit var dataSet: MutableList<Pair<String, String>>
    private lateinit var savedCallAdapter: SavedCallAdapter

    private val fullDataSet = mutableListOf<Pair<String, String>>()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var filterSpinner: Spinner

    private val nameToDocId = mutableMapOf<String, String>()

    private val saveEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val root = findViewById<View>(R.id.main)

        val snack = Snackbar.make(root, "", 4000)
        snack.setBackgroundTint(ContextCompat.getColor(this, R.color.dusty_mauve_dark))
        snack.setTextColor(ContextCompat.getColor(this, R.color.white))
        snack.setActionTextColor(ContextCompat.getColor(this, R.color.almond_silk_dark))

        if (result.resultCode != RESULT_OK) {
            snack.setText("Add API Call Canceled")
            snack.show()
            return@registerForActivityResult
        }

        val callName = result.data?.getStringExtra("CALL_NAME")?.trim().orEmpty()
        val callDesc = result.data?.getStringExtra("CALL_DESCRIPTION")?.trim().orEmpty()
        val callUrl = result.data?.getStringExtra("CALL_URL")?.trim().orEmpty()
        val callKey = result.data?.getStringExtra("CALL_KEY")?.trim().orEmpty()

        if (callName.isBlank()) {
            snack.setText("Call name is required.")
            snack.show()
            return@registerForActivityResult
        }

        createSavedCall(
            name = callName,
            description = callDesc,
            url = callUrl,
            apiKey = callKey,
            onSuccess = { docId ->
                snack.setText("Saved API Call: $callName")
                snack.setAction("View") {
                    val intent = Intent(this, SavedCallActivity::class.java)
                    intent.putExtra("CALL_ID", docId)
                    startActivity(intent)
                }
                snack.show()

                loadSavedCalls()
            },
            onFailure = { msg ->
                snack.setText(msg)
                snack.show()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        dataSet = mutableListOf()
        savedCallAdapter = SavedCallAdapter(dataSet) { clickedItem, function ->
            when (function) {
                "navigate" -> {
                    val callName = clickedItem.first
                    val callId = nameToDocId[callName]

                    val intent = Intent(this, SavedCallActivity::class.java)
                    if (!callId.isNullOrBlank()) {
                        intent.putExtra("CALL_ID", callId)
                    } else {
                        // fallback safety
                        intent.putExtra("CALL_NAME", callName)
                    }
                    startActivity(intent)
                }

                "delete" -> {
                    val name = clickedItem.first
                    AlertDialog.Builder(this)
                        .setTitle("You are about to delete $name, do you wish to continue?")
                        .setPositiveButton("Confirm Delete") { _, _ ->
                            deleteSavedCallByName(
                                name,
                                onSuccess = {
                                    loadSavedCalls()
                                },
                                onFailure = { msg ->
                                    Snackbar.make(findViewById(R.id.main), msg, 3000).show()
                                }
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }

        val rv: RecyclerView = findViewById(R.id.saved_call_recyclerview)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = savedCallAdapter

        findViewById<Button>(R.id.logout_button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setPositiveButton("Confirm") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Back", null)
                .show()
        }

        findViewById<Button>(R.id.add_button).setOnClickListener {
            val intent = Intent(this, SaveEditCallActivity::class.java)
            saveEditLauncher.launch(intent)
        }

        val aboutBtn = findViewById<Button?>(R.id.about_button)
        aboutBtn?.setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }

        filterSpinner = findViewById(R.id.filter_spinner)

        val filterOptions = listOf("Newest", "Oldest", "A–Z", "Z–A")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            filterOptions
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        loadSavedCalls()
    }

    override fun onResume() {
        super.onResume()
        loadSavedCalls()
    }

    private fun userCallsCollection() =
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("savedCalls")

    private fun loadSavedCalls() {
        userCallsCollection()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                fullDataSet.clear()
                nameToDocId.clear()

                for (doc in snap.documents) {
                    val name = doc.getString("name").orEmpty()
                    val desc = doc.getString("description").orEmpty()
                    if (name.isNotBlank()) {
                        nameToDocId[name] = doc.id
                        fullDataSet.add(Pair(name, desc))
                    }
                }

                applyFilter()
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    findViewById(R.id.main),
                    e.localizedMessage ?: "Failed to load saved calls.",
                    4000
                ).show()
            }
    }

    private fun applyFilter() {
        val selected = filterSpinner.selectedItem?.toString() ?: "Newest"

        dataSet.clear()
        when (selected) {
            "A–Z" -> dataSet.addAll(fullDataSet.sortedBy { it.first.lowercase() })
            "Z–A" -> dataSet.addAll(fullDataSet.sortedByDescending { it.first.lowercase() })
            "Oldest" -> {
                dataSet.addAll(fullDataSet.reversed())
            }
            "Newest" -> {
                dataSet.addAll(fullDataSet)
            }
            else -> dataSet.addAll(fullDataSet)
        }

        savedCallAdapter.notifyDataSetChanged()
    }

    private fun createSavedCall(
        name: String,
        description: String,
        url: String,
        apiKey: String,
        onSuccess: (docId: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        val doc = hashMapOf(
            "name" to name,
            "description" to description,
            "url" to url,
            "apiKey" to apiKey,
            "createdAt" to System.currentTimeMillis()
        )

        userCallsCollection()
            .add(doc)
            .addOnSuccessListener { ref -> onSuccess(ref.id) }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Failed to save API call.")
            }
    }

    private fun deleteSavedCallByName(
        name: String,
        onSuccess: () -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        val docId = nameToDocId[name]

        if (docId != null) {
            deleteSavedCallAndHistory(
                callId = docId,
                onSuccess = {
                    nameToDocId.remove(name)
                    onSuccess()
                },
                onFailure = onFailure
            )
            return
        }

        userCallsCollection()
            .whereEqualTo("name", name)
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                if (doc == null) {
                    onFailure("Could not find item to delete.")
                    return@addOnSuccessListener
                }

                deleteSavedCallAndHistory(
                    callId = doc.id,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Delete failed.")
            }
    }

    private fun deleteSavedCallAndHistory(
        callId: String,
        onSuccess: () -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onFailure("Not logged in.")
            return
        }

        val callRef = db.collection("users").document(uid)
            .collection("savedCalls").document(callId)

        val historyRef = callRef.collection("history")

        fun deleteHistoryPage() {
            historyRef.limit(500)
                .get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) {
                        callRef.delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { e ->
                                onFailure(e.localizedMessage ?: "Failed to delete call.")
                            }
                        return@addOnSuccessListener
                    }

                    val batch = db.batch()
                    for (d in snap.documents) batch.delete(d.reference)

                    batch.commit()
                        .addOnSuccessListener {
                            deleteHistoryPage()
                        }
                        .addOnFailureListener { e ->
                            onFailure(e.localizedMessage ?: "Failed to delete history.")
                        }
                }
                .addOnFailureListener { e ->
                    onFailure(e.localizedMessage ?: "Failed to load history for deletion.")
                }
        }

        deleteHistoryPage()
    }
}

