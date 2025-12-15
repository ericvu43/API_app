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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CompareCallActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private var toggleFormatted = false
    private var callId: String? = null

    private var currentRaw: String = ""
    private var expectedRaw: String = ""

    private lateinit var dateTimeTv: TextView
    private lateinit var callNameTv: TextView
    private lateinit var callUrlTv: TextView
    private lateinit var currentTv: TextView
    private lateinit var expectedTv: TextView
    private lateinit var toggleBtn: Button

    private val IGNORE_KEYS = setOf(
        "generationtime_ms",
        "current"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_compare_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton: Button = findViewById(R.id.back_button)
        dateTimeTv = findViewById(R.id.date_time)
        callNameTv = findViewById(R.id.call_name)
        callUrlTv = findViewById(R.id.call_url)
        currentTv = findViewById(R.id.current_response)
        expectedTv = findViewById(R.id.expected_response)
        toggleBtn = findViewById(R.id.response_toggle)

        backButton.setOnClickListener { finish() }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        callId = intent.getStringExtra("CALL_ID")
        if (callId.isNullOrBlank()) {
            simpleMsg("Compare Error", "Missing CALL_ID.")
            finish()
            return
        }

        toggleBtn.setOnClickListener {
            toggleFormatted = !toggleFormatted
            toggleBtn.text =
                if (toggleFormatted) getString(R.string.toggle_formatted) else getString(R.string.toggle_raw)
            renderResponses()
        }

        findViewById<Button>(R.id.response_button).setOnClickListener {
            val id = callId ?: return@setOnClickListener
            val url = callUrlTv.text.toString().trim().replace("\n", "").replace(" ", "")
            if (url.isBlank()) {
                simpleMsg("API Call Error", "No API Call URL provided.")
                return@setOnClickListener
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                simpleMsg("API Call Error", "URL must start with http:// or https://")
                return@setOnClickListener
            }

            val loadingDialog = AlertDialog.Builder(this)
                .setTitle("Testing API Call...")
                .setMessage("Fetching response…")
                .setCancelable(false)
                .create()
            loadingDialog.show()

            val callRef = db.collection("users").document(uid).collection("savedCalls").document(id)
            callRef.get()
                .addOnSuccessListener { doc ->
                    val apiKey = doc.getString("apiKey").orEmpty().trim()

                    fetchApi(
                        url,
                        apiKey,
                        onSuccess = { rawBody ->
                            val now = System.currentTimeMillis()
                            currentRaw = rawBody
                            runOnUiThread { dateTimeTv.text = formatDateTime(now) }

                            recordTestRun(uid, id, url, rawBody)

                            runOnUiThread {
                                loadingDialog.dismiss()
                                renderResponses()
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
                        simpleMsg("API Call Error", e.localizedMessage ?: "Could not load saved call.")
                    }
                }
        }

        findViewById<Button>(R.id.edit_button).setOnClickListener {
            val i = Intent(this, SaveEditCallActivity::class.java)
            callId?.let { i.putExtra("CALL_ID", it) }
            startActivity(i)
        }

        loadCall(uid, callId!!)
    }

    private fun loadCall(uid: String, callId: String) {
        val callRef = db.collection("users").document(uid).collection("savedCalls").document(callId)

        callRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                callNameTv.text = "Not found"
                callUrlTv.text = ""
                currentRaw = ""
                expectedRaw = ""
                renderResponses()
                return@addOnSuccessListener
            }

            callNameTv.text = doc.getString("name") ?: "(Unnamed)"
            callUrlTv.text = doc.getString("url") ?: ""
            expectedRaw = doc.getString("expectedJson") ?: ""
            currentRaw = doc.getString("lastResponseJson") ?: ""

            val last = doc.getLong("lastTestedAt")
            dateTimeTv.text = if (last != null) formatDateTime(last) else getString(R.string.date_time)

            renderResponses()
        }
    }

    private fun renderResponses() {
        val currentText = if (toggleFormatted) prettyJsonOrRaw(currentRaw) else currentRaw
        val expectedText = if (toggleFormatted) prettyJsonOrRaw(expectedRaw) else expectedRaw

        currentTv.text = currentText.ifBlank { "(No current response yet — press Test API Call)" }
        expectedTv.text = expectedText.ifBlank { "(No expected response saved yet)" }
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
                val dt = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(now))

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

    private fun formatDateTime(ms: Long): String =
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(ms))

    private fun simpleMsg(title: String, msg: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdownNow()
    }
}
