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

class CallHistoryActivity : AppCompatActivity() {
    lateinit var dataSet: MutableList<Triple<String, String, Boolean>>
    lateinit var callHistoryAdapter: CallHistoryAdapter

    var toggleState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call_history)
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

        val title: TextView = findViewById(R.id.title)

        if(intent.hasExtra("CALL_NAME")) // REPLACE WITH ITEM ID INSTEAD
            title.text = intent.getStringExtra("CALL_NAME").toString()+" - History" // REPLACE WITH ITEM ID INSTEAD

        dataSet = mutableListOf(Triple("Name 1", "DateTime 1", true), Triple("Name 2", "DateTime 2", false), Triple("Name 3", "DateTime 3", true))
        callHistoryAdapter = CallHistoryAdapter(dataSet) { clickedItem, function ->
            if(function == "navigate") {
                val intent = Intent(this, CompareCallActivity::class.java)
                intent.putExtra("CALL_NAME", clickedItem.first) // REPLACE WITH ITEM ID INSTEAD
                intent.putExtra("DATE_TIME", clickedItem.second)
                startActivity(intent)
            }
            if(function == "delete") {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("You are about to delete ${clickedItem.first}, do you wish to continue?").setPositiveButton("Confirm Delete") { dialog, which ->
                    val itemPosition = dataSet.indexOf(clickedItem)
                    dataSet.remove(clickedItem)
                    callHistoryAdapter.notifyItemRemoved(itemPosition)
                    callHistoryAdapter.notifyItemRangeChanged(0, dataSet.size)
                }.setNegativeButton("Cancel") { dialog, which -> }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
        val callHistoryRecyclerview: RecyclerView = findViewById(R.id.call_history_recyclerview)
        callHistoryRecyclerview.layoutManager = LinearLayoutManager(this)
        callHistoryRecyclerview.adapter = callHistoryAdapter
    }
}