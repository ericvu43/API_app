package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
            return
        }

        val navigationButton: Button = findViewById(R.id.login_navigate_button)
        navigationButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        val emailAddress: EditText = findViewById(R.id.login_email)
        val password: EditText = findViewById(R.id.login_password)

        val loginButton: Button = findViewById(R.id.login_submit_button)
        loginButton.setOnClickListener {
            val email = emailAddress.text.toString().trim()
            val pass = password.text.toString()

            if (email.isBlank() || pass.isBlank()) {
                Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage ?: "Login failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        val forgotPassword: TextView = findViewById(R.id.forgot_password)
        forgotPassword.setOnClickListener {
            val email = emailAddress.text.toString().trim()

            if (email.isBlank()) {
                Toast.makeText(this, "Enter your email first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Forgot Password")
                .setMessage("Send a password reset email to:\n$email ?")
                .setPositiveButton("Send") { _, _ ->
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Reset email sent.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    this,
                                    task.exception?.localizedMessage ?: "Failed to send reset email.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
