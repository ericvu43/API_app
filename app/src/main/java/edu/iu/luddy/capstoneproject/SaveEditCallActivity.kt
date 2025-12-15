package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class SaveEditCallActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val ioExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_save_edit_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val callName: EditText = findViewById(R.id.name_field)
        val callDescription: EditText = findViewById(R.id.description_field)
        val callURL: EditText = findViewById(R.id.url_field)
        val callKey: EditText = findViewById(R.id.key_field)

        val callId = intent.getStringExtra("CALL_ID")
        val isEdit = !callId.isNullOrBlank()

        if (isEdit) {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

            db.collection("users")
                .document(uid)
                .collection("savedCalls")
                .document(callId!!)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        callName.setText(doc.getString("name").orEmpty())
                        callDescription.setText(doc.getString("description").orEmpty())
                        callURL.setText(doc.getString("url").orEmpty())
                        callKey.setText(doc.getString("apiKey").orEmpty())
                    }
                }
        }

        findViewById<Button>(R.id.back_button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Exit without saving?")
                .setPositiveButton("Exit") { _, _ ->
                    setResult(RESULT_CANCELED, Intent())
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.response_button).setOnClickListener {
            val rawUrl = callURL.text.toString()
            val cleanedUrl = rawUrl.trim().replace("\n", "").replace(" ", "")
            val key = callKey.text.toString().trim()

            if (cleanedUrl.isBlank()) {
                showMsg("API Call Error", "No API Call URL provided")
                return@setOnClickListener
            }
            if (!cleanedUrl.startsWith("http://") && !cleanedUrl.startsWith("https://")) {
                showMsg("API Call Error", "URL must start with http:// or https://")
                return@setOnClickListener
            }

            val loadingDialog = AlertDialog.Builder(this)
                .setTitle("Testing API Call...")
                .setMessage("Fetching response…")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            fetchApi(
                cleanedUrl,
                key,
                onSuccess = { rawBody ->
                    val pretty = prettyJsonOrRaw(rawBody)
                    runOnUiThread {
                        loadingDialog.dismiss()
                        showJsonDialog(cleanedUrl, pretty.ifBlank { "(Empty response body)" })
                    }
                },
                onFailure = { msg ->
                    runOnUiThread {
                        loadingDialog.dismiss()
                        showMsg("API Call Error", msg)
                    }
                }
            )
        }

        findViewById<Button>(R.id.save_button).setOnClickListener {
            val name = callName.text.toString().trim()
            val desc = callDescription.text.toString().trim()
            val url = callURL.text.toString().trim()
            val key = callKey.text.toString().trim()

            if (name.isBlank() || url.isBlank()) {
                showMsg("API Call Error", "Name and URL are required.")
                return@setOnClickListener
            }

            if (!isEdit) {
                val data = Intent()
                data.putExtra("MODE", "ADD")
                data.putExtra("CALL_NAME", name)
                data.putExtra("CALL_DESCRIPTION", desc)
                data.putExtra("CALL_URL", url)
                data.putExtra("CALL_KEY", key)
                setResult(RESULT_OK, data)
                finish()
                return@setOnClickListener
            }

            val uid = auth.currentUser?.uid
            if (uid == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val updates = hashMapOf<String, Any>(
                "name" to name,
                "description" to desc,
                "url" to url,
                "apiKey" to key,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .collection("savedCalls")
                .document(callId!!)
                .update(updates)
                .addOnSuccessListener {
                    val data = Intent()
                    data.putExtra("MODE", "EDIT")
                    data.putExtra("CALL_ID", callId)
                    setResult(RESULT_OK, data)
                    finish()
                }
                .addOnFailureListener { e ->
                    showMsg("Save failed", e.localizedMessage ?: "Could not update this API call.")
                }
        }
    }

    private fun showMsg(title: String, msg: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showJsonDialog(title: String, body: String) {
        val tv = TextView(this).apply {
            setPadding(40, 30, 40, 30)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            val maxChars = 20_000
            text = if (body.length > maxChars) body.take(maxChars) + "\n\n...(truncated)" else body
        }
        val scroll = android.widget.ScrollView(this).apply { addView(tv) }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scroll)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun fetchApi(
        urlString: String,
        apiKey: String,
        onSuccess: (rawBody: String) -> Unit,
        onFailure: (msg: String) -> Unit
    ) {
        ioExecutor.execute {
            try {
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("Accept", "application/json")
                    if (apiKey.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $apiKey")
                        setRequestProperty("x-api-key", apiKey)
                    }
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream

                val body = BufferedReader(InputStreamReader(stream)).use { br ->
                    buildString {
                        while (true) {
                            val line = br.readLine() ?: break
                            append(line).append('\n')
                        }
                    }
                }.trim()

                if (code !in 200..299) {
                    onFailure("HTTP $code\n\n$body")
                    return@execute
                }

                onSuccess(body)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Request failed.")
            }
        }
    }

    private fun prettyJsonOrRaw(text: String): String {
        val t = text.trim()
        if (t.isBlank()) return t
        return try {
            when {
                t.startsWith("{") -> JSONObject(t).toString(2)
                t.startsWith("[") -> JSONArray(t).toString(2)
                else -> t
            }
        } catch (_: Exception) {
            t
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdownNow()
    }
}
