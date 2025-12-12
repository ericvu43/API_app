package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SaveEditCallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_save_edit_call)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val backButton: Button = findViewById(R.id.back_button)
        backButton.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Exit without saving?").setPositiveButton("Exit") { dialog, which ->
                val intent = Intent()
                setResult(RESULT_CANCELED, intent)
                finish()
            }.setNegativeButton("Cancel") { dialog, which -> }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        val callName: EditText = findViewById(R.id.name_field)
        val callDescription: EditText = findViewById(R.id.description_field)
        val callURL: EditText = findViewById(R.id.url_field)
        val callKey: EditText = findViewById(R.id.key_field)

        if(intent.hasExtra("CALL_NAME")) // REPLACE WITH ITEM ID INSTEAD
            callName.setText(intent.getStringExtra("CALL_NAME").toString()) // REPLACE WITH ITEM ID INSTEAD

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

        val saveButton: Button = findViewById(R.id.save_button)
        saveButton.setOnClickListener {
            if(callName.text.toString() != "" && callDescription.text.toString() != "" && callURL.text.toString() != "") {
                /*
                    Save API call information to local storage and firestore
                */
                val intent = Intent()
                intent.putExtra("CALL_NAME", callName.text.toString())
                intent.putExtra("CALL_DESCRIPTION", callDescription.text.toString())
                setResult(RESULT_OK, intent)
                finish()
            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("API Call Error").setMessage("No API Call URL provided").setNegativeButton("Close") { dialog, which -> }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }

    }
}