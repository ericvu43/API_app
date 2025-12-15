package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class SavedCallActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var docListener: ListenerRegistration? = null
    private var callId: String? = null
    private var passedName: String? = null

    private lateinit var callNameTv: TextView
    private lateinit var callDescriptionTv: TextView
    private lateinit var callURLTv: TextView
    private lateinit var callKeyTv: TextView

    private val IGNORE_KEYS = setOf("generationtime_ms", "current")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        callNameTv = findViewById(R.id.call_name)
        callDescriptionTv = findViewById(R.id.call_description)
        callURLTv = findViewById(R.id.call_url)
        callKeyTv = findViewById(R.id.call_key)

        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        callId = intent.getStringExtra("CALL_ID")
        passedName = intent.getStringExtra("CALL_NAME")

        val keyToggle = findViewById<CheckBox>(R.id.key_check)
        val keyToggleLabel = findViewById<TextView>(R.id.key_toggle)

        // default masked
        callKeyTv.transformationMethod = PasswordTransformationMethod.getInstance()

        keyToggle.setOnCheckedChangeListener { _, isChecked ->
            keyToggleLabel.text = if (isChecked) getString(R.string.key_hide) else getString(R.string.key_show)

            if (!isChecked) {
                callKeyTv.transformationMethod = PasswordTransformationMethod.getInstance()
                callKeyTv.text = if (callKeyTv.text.isNullOrBlank()) "" else "••••••••••"
                return@setOnCheckedChangeListener
            }

            val uidNow = auth.currentUser?.uid ?: return@setOnCheckedChangeListener
            val idNow = callId ?: return@setOnCheckedChangeListener

            db.collection("users").document(uidNow).collection("savedCalls").document(idNow)
                .get()
                .addOnSuccessListener { doc ->
                    callKeyTv.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    callKeyTv.text = doc.getString("apiKey").orEmpty()
                }
        }

        findViewById<Button>(R.id.save_edit_button).setOnClickListener {
            val uid2 = auth.currentUser?.uid
            val id2 = callId
            if (uid2.isNullOrBlank() || id2.isNullOrBlank()) {
                AlertDialog.Builder(this)
                    .setTitle("Expected Response")
                    .setMessage("Missing CALL_ID — open this call from the menu so it passes CALL_ID.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            showExpectedResponsePopup(uid2, id2)
        }

        findViewById<Button>(R.id.response_button).setOnClickListener {
            val uidNow = auth.currentUser?.uid
            val idNow = callId
            if (uidNow.isNullOrBlank() || idNow.isNullOrBlank()) {
                simpleMsg("API Call Error", "Missing CALL_ID.")
                return@setOnClickListener
            }

            val cleanedUrl = callURLTv.text.toString().trim().replace("\n", "").replace(" ", "")
            if (cleanedUrl.isBlank()) {
                simpleMsg("API Call Error", "No API Call URL provided.")
                return@setOnClickListener
            }
            if (!cleanedUrl.startsWith("http://") && !cleanedUrl.startsWith("https://")) {
                simpleMsg("API Call Error", "URL must start with http:// or https://")
                return@setOnClickListener
            }

            val loadingDialog = AlertDialog.Builder(this)
                .setTitle("Testing API Call...")
                .setMessage("Fetching response…")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            val callRef = db.collection("users").document(uidNow).collection("savedCalls").document(idNow)
            callRef.get()
                .addOnSuccessListener { doc ->
                    val apiKey = doc.getString("apiKey").orEmpty().trim()

                    fetchApi(
                        cleanedUrl,
                        apiKey,
                        onSuccess = { rawBody ->
                            recordTestRun(uidNow, idNow, cleanedUrl, rawBody)

                            runOnUiThread {
                                loadingDialog.dismiss()
                                showJsonDialog(cleanedUrl, prettyJsonOrRaw(rawBody))
                            }
                        },
                        onFailure = { msg ->
                            runOnUiThread {
                                loadingDialog.dismiss()
                                simpleMsg("API Call Error", msg)
                            }
                        }
                    )
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        loadingDialog.dismiss()
                        simpleMsg("API Call Error", e.localizedMessage ?: "Could not load API key.")
                    }
                }
        }

        findViewById<Button>(R.id.edit_button).setOnClickListener {
            val i = Intent(this, SaveEditCallActivity::class.java)
            callId?.let { i.putExtra("CALL_ID", it) }
            startActivity(i)
        }

        findViewById<Button>(R.id.history_button).setOnClickListener {
            val id = callId
            if (id.isNullOrBlank()) {
                simpleMsg("History Error", "Missing CALL_ID.")
                return@setOnClickListener
            }
            startActivity(Intent(this, CallHistoryActivity::class.java).putExtra("CALL_ID", id))
        }

        findViewById<Button>(R.id.compare_button).setOnClickListener {
            val id = callId
            if (id.isNullOrBlank()) {
                simpleMsg("Compare Error", "Missing CALL_ID.")
                return@setOnClickListener
            }
            startActivity(Intent(this, CompareCallActivity::class.java).putExtra("CALL_ID", id))
        }

        if (callId.isNullOrBlank() && passedName.isNullOrBlank()) {
            callNameTv.text = "Missing call"
            callDescriptionTv.text = "No CALL_ID or CALL_NAME was provided."
            callURLTv.text = ""
            callKeyTv.text = ""
        }
    }

    override fun onStart() {
        super.onStart()
        val uid = auth.currentUser?.uid ?: return

        docListener?.remove()
        docListener = null

        val id = callId
        val name = passedName

        if (!id.isNullOrBlank()) {
            docListener = db.collection("users")
                .document(uid)
                .collection("savedCalls")
                .document(id)
                .addSnapshotListener { doc, _ ->
                    if (doc == null) return@addSnapshotListener
                    if (!doc.exists()) {
                        callNameTv.text = "Not found"
                        callDescriptionTv.text = "This saved call was deleted."
                        callURLTv.text = ""
                        callKeyTv.text = ""
                        return@addSnapshotListener
                    }

                    val newName = doc.getString("name").orEmpty()
                    val newDesc = doc.getString("description").orEmpty()
                    val newUrl = doc.getString("url").orEmpty()
                    val key = doc.getString("apiKey").orEmpty()

                    if (callNameTv.text.toString() != newName) callNameTv.text = newName
                    if (callDescriptionTv.text.toString() != newDesc) callDescriptionTv.text = newDesc
                    if (callURLTv.text.toString() != newUrl) callURLTv.text = newUrl

                    if (keyToggleMasked()) {
                        callKeyTv.text = if (key.isBlank()) "" else "••••••••••"
                        callKeyTv.transformationMethod = PasswordTransformationMethod.getInstance()
                    }
                }
        } else if (!name.isNullOrBlank()) {
            callNameTv.text = name
        }
    }

    override fun onStop() {
        super.onStop()
        docListener?.remove()
        docListener = null
    }

    private fun keyToggleMasked(): Boolean {
        val keyToggle = findViewById<CheckBox>(R.id.key_check)
        return !keyToggle.isChecked
    }

    private fun showExpectedResponsePopup(uid: String, callId: String) {
        val input = android.widget.EditText(this).apply {
            setPadding(40, 30, 40, 30)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            minLines = 12
            hint = "Paste expected JSON here..."
        }
        val scroll = android.widget.ScrollView(this).apply { addView(input) }

        db.collection("users").document(uid).collection("savedCalls").document(callId)
            .get()
            .addOnSuccessListener { doc ->
                input.setText(doc.getString("expectedJson").orEmpty())

                AlertDialog.Builder(this)
                    .setTitle("Expected Response")
                    .setView(scroll)
                    .setNegativeButton("Close", null)
                    .setPositiveButton("Save") { _, _ ->
                        val expected = input.text.toString()
                        db.collection("users").document(uid).collection("savedCalls").document(callId)
                            .set(
                                mapOf(
                                    "expectedJson" to expected,
                                    "expectedUpdatedAt" to System.currentTimeMillis()
                                ),
                                SetOptions.merge()
                            )
                    }
                    .show()
            }
    }

    private fun showJsonDialog(title: String, body: String) {
        val tv = TextView(this).apply {
            setPadding(40, 30, 40, 30)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            val maxChars = 5_000
            text = if (body.length > maxChars) body.take(maxChars) + "\n\n...(truncated)" else body
        }
        val scroll = android.widget.ScrollView(this).apply { addView(tv) }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scroll)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun simpleMsg(title: String, msg: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
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

    private fun recordTestRun(uid: String, callId: String, url: String, responseRaw: String) {
        val callRef = db.collection("users").document(uid).collection("savedCalls").document(callId)

        callRef.get().addOnSuccessListener { doc ->
            val expected = doc.getString("expectedJson").orEmpty()

            ioExecutor.execute {
                val passed = passedIgnoringKeys(expected, responseRaw)

                val now = System.currentTimeMillis()
                val dt = java.text.SimpleDateFormat(
                    "MMM d, yyyy • h:mm a",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(now))

                val historyDoc = mapOf(
                    "createdAt" to now,
                    "dateTime" to dt,
                    "url" to url,
                    "responseRaw" to responseRaw,
                    "responsePretty" to responseRaw,
                    "expectedJsonAtTime" to expected.trim(),
                    "passed" to passed
                )

                db.runBatch { batch ->
                    batch.set(
                        callRef,
                        mapOf(
                            "lastTestedAt" to now,
                            "lastTestedDateTime" to dt,
                            "lastResponseJson" to responseRaw,
                            "lastResponsePretty" to responseRaw,
                            "lastPassed" to passed
                        ),
                        SetOptions.merge()
                    )
                    batch.set(callRef.collection("history").document(), historyDoc)
                }
            }
        }
    }

    private fun passedIgnoringKeys(expected: String, actual: String): Boolean? {
        val e = expected.trim()
        val a = actual.trim()
        if (e.isBlank()) return null
        if (e == a) return true

        return try {
            jsonEq(stripKeys(parseJson(e)), stripKeys(parseJson(a)))
        } catch (_: Exception) {
            e == a
        }
    }

    private fun parseJson(s: String): Any =
        when {
            s.startsWith("{") -> JSONObject(s)
            s.startsWith("[") -> JSONArray(s)
            else -> s
        }

    private fun stripKeys(v: Any?): Any? = when (v) {
        null, JSONObject.NULL -> null
        is JSONObject -> JSONObject().also { out ->
            val it = v.keys()
            while (it.hasNext()) {
                val k = it.next()
                if (k in IGNORE_KEYS) continue
                out.put(k, stripKeys(v.opt(k)))
            }
        }
        is JSONArray -> JSONArray().also { out ->
            for (i in 0 until v.length()) out.put(stripKeys(v.opt(i)))
        }
        is Number -> v.toDouble()
        else -> v
    }

    private fun jsonEq(a: Any?, b: Any?): Boolean {
        val aa = if (a === JSONObject.NULL) null else a
        val bb = if (b === JSONObject.NULL) null else b
        if (aa == null || bb == null) return aa == bb
        if (aa is Number && bb is Number) return aa.toDouble() == bb.toDouble()

        return when {
            aa is JSONObject && bb is JSONObject -> {
                val aKeys = aa.keys().asSequence().toSet()
                val bKeys = bb.keys().asSequence().toSet()
                aKeys == bKeys && aKeys.all { k -> jsonEq(aa.opt(k), bb.opt(k)) }
            }
            aa is JSONArray && bb is JSONArray -> {
                aa.length() == bb.length() && (0 until aa.length()).all { i -> jsonEq(aa.opt(i), bb.opt(i)) }
            }
            else -> aa == bb
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
        docListener?.remove()
        ioExecutor.shutdownNow()
    }
}
