package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class CompareCallActivity : AppCompatActivity() {
    var toggleState = false

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
        backButton.setOnClickListener {
            val intent = Intent()
            setResult(RESULT_CANCELED, intent)
            finish()
        }

        val dateTime: TextView = findViewById(R.id.date_time)

        if(intent.hasExtra("DATE_TIME")) // REPLACE WITH ITEM ID INSTEAD
            dateTime.text = intent.getStringExtra("DATE_TIME") // REPLACE WITH ITEM ID INSTEAD

        val callName: TextView = findViewById(R.id.call_name)
        val callURL: TextView = findViewById(R.id.call_url)

        if(intent.hasExtra("CALL_NAME")) // REPLACE WITH ITEM ID INSTEAD
            callName.text = intent.getStringExtra("CALL_NAME").toString() // REPLACE WITH ITEM ID INSTEAD

        val currentResponse: TextView = findViewById(R.id.current_response)
        val expectedResponse: TextView = findViewById(R.id.expected_response)

        val toggleButton: Button = findViewById(R.id.response_toggle)
        toggleButton.setOnClickListener {
            toggleButton.text = if (toggleState) getString(R.string.toggle_raw) else getString(R.string.toggle_formatted)
            toggleState = !toggleState
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
            val calendar: Calendar = Calendar.getInstance()
            dateTime.text = calendar.time.toString() // Save this value and response values
        }

        val editButton: Button = findViewById(R.id.edit_button)
        editButton.setOnClickListener {
            val intent = Intent(this, SaveEditCallActivity::class.java)
            intent.putExtra("CALL_NAME", callName.text.toString()) // REPLACE WITH ITEM ID INSTEAD
            startActivity(intent)
        }
    }
}