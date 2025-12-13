package edu.iu.luddy.capstoneproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

class MenuActivity : AppCompatActivity() {
    lateinit var dataSet: MutableList<Pair<String, String>>
    lateinit var savedCallAdapter: SavedCallAdapter

    private val saveEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        dataSet.add(Pair(result.data?.getStringExtra("CALL_NAME").toString(), result.data?.getStringExtra("CALL_DESCRIPTION").toString()))
        savedCallAdapter.notifyItemInserted(dataSet.size-1)
        val resultSnackbar: Snackbar = Snackbar.make(findViewById(R.id.main), "Placeholder", 4000)
        resultSnackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.dusty_mauve_dark))
        resultSnackbar.setTextColor(ContextCompat.getColor(this, R.color.white))
        resultSnackbar.setActionTextColor(ContextCompat.getColor(this, R.color.almond_silk_dark))
        if(result.resultCode == Activity.RESULT_OK) {
            val callName = result.data?.getStringExtra("CALL_NAME").toString()
            resultSnackbar.setText("Saved API Call: $callName")
            resultSnackbar.setAction("View") {
                val intent = Intent(this, SavedCallActivity::class.java)
                intent.putExtra("CALL_NAME", callName) // REPLACE WITH ITEM ID INSTEAD
                startActivity(intent)
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED){
            resultSnackbar.setText("Add API Call Canceled")
        }
        resultSnackbar.show()
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
        val logoutButton: Button = findViewById(R.id.logout_button)
        logoutButton.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Sign out?").setPositiveButton("Confirm") { dialog, which ->
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }.setNegativeButton("Back") { dialog, which -> }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        val addButton: Button = findViewById(R.id.add_button)
        addButton.setOnClickListener {
            val intent = Intent(this, SaveEditCallActivity::class.java)
            saveEditLauncher.launch(intent)
        }

        val aboutButton: Button = findViewById(R.id.about_button)
        aboutButton.setOnClickListener {
            val intent = Intent(this, AboutAppActivity::class.java)
            startActivity(intent)
        }

        dataSet = mutableListOf(Pair("Name 1", "Description 1"), Pair("Name 2", "Description 2"), Pair("Name 3", "Description 3"))
        savedCallAdapter = SavedCallAdapter(dataSet) { clickedItem, function ->
            if(function == "navigate") {
                val intent = Intent(this, SavedCallActivity::class.java)
                intent.putExtra("CALL_NAME", clickedItem.first) // REPLACE WITH ITEM ID INSTEAD
                startActivity(intent)
            }
            if(function == "delete") {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("You are about to delete ${clickedItem.first}, do you wish to continue?").setPositiveButton("Confirm Delete") { dialog, which ->
                    val itemPosition = dataSet.indexOf(clickedItem)
                    dataSet.remove(clickedItem)
                    savedCallAdapter.notifyItemRemoved(itemPosition)
                    savedCallAdapter.notifyItemRangeChanged(0, dataSet.size)
                }.setNegativeButton("Cancel") { dialog, which -> }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
        val savedCallRecyclerview: RecyclerView = findViewById(R.id.saved_call_recyclerview)
        savedCallRecyclerview.layoutManager = LinearLayoutManager(this)
        savedCallRecyclerview.adapter = savedCallAdapter
    }
}