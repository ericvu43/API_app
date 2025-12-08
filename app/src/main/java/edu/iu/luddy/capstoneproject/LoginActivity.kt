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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navigationButton: Button = findViewById(R.id.login_navigate_button)
        navigationButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        val emailAddress: EditText = findViewById(R.id.login_email)
        val password: EditText = findViewById(R.id.login_password)
        val loginButton: Button = findViewById(R.id.login_submit_button)
        loginButton.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        val forgotPassword: TextView = findViewById(R.id.forgot_password)
        forgotPassword.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("Forgot Password").setTitle("Forgot Password").setPositiveButton("Reset Password") {dialog, which -> }.setNegativeButton("Cancel") {dialog, which -> }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }
}