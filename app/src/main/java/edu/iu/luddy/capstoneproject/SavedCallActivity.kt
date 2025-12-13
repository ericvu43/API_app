package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SavedCallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_saved_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        }

        val callName: TextView = findViewById(R.id.call_name)
        val callDescription: TextView = findViewById(R.id.call_description)
        val callURL: TextView = findViewById(R.id.call_url)
        val callKey: TextView = findViewById(R.id.call_key)

        // GET VALUES FROM DATABASE/LOCAL
        callName.text = intent.getStringExtra("CALL_NAME")
        callDescription.text = "Temporary description goes here"
        callURL.text = "https://api-url-here/path"
        callKey.text = "API Key"

        val keyToggle: CheckBox = findViewById(R.id.key_check)
        val keyToggleLabel: TextView = findViewById(R.id.key_toggle)
        keyToggle.setOnCheckedChangeListener { keyToggle,  isChecked ->
            callKey.inputType = if (isChecked) InputType.TYPE_TEXT_VARIATION_NORMAL else InputType.TYPE_TEXT_VARIATION_PASSWORD
            callKey.transformationMethod = if (isChecked) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
            keyToggleLabel.text = if (isChecked) getString(R.string.key_hide) else getString(R.string.key_show)
        }

        val saveEditResponse: Button = findViewById(R.id.save_edit_button)
        saveEditResponse.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Go to Activity").setMessage("SaveEditResponseActivity").setPositiveButton("Done") { dialog, which -> }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        val responseButton: Button = findViewById(R.id.response_button)
        responseButton.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            if(callURL.text.toString() != "")
                builder.setTitle(callURL.text).setMessage("JSON Response Here").setNegativeButton("Close") { dialog, which -> }
            else
                builder.setTitle("API Call Error").setMessage("No API Call URL provided").setNegativeButton("Close") { dialog, which -> }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        val compareButton: Button = findViewById(R.id.compare_button)
        compareButton.setOnClickListener {
            val intent = Intent(this, CompareCallActivity::class.java)
            intent.putExtra("CALL_NAME", callName.text.toString()) // REPLACE WITH ITEM ID INSTEAD
            startActivity(intent)
        }

        val editButton: Button = findViewById(R.id.edit_button)
        editButton.setOnClickListener {
            val intent = Intent(this, SaveEditCallActivity::class.java)
            intent.putExtra("CALL_NAME", callName.text.toString()) // REPLACE WITH ITEM ID INSTEAD
            startActivity(intent)
        }

        val historyButton: Button = findViewById(R.id.history_button)
        historyButton.setOnClickListener {
            val intent = Intent(this, CallHistoryActivity::class.java)
            intent.putExtra("CALL_NAME", callName.text.toString()) // REPLACE WITH ITEM ID INSTEAD
            startActivity(intent)
        }
    }
}