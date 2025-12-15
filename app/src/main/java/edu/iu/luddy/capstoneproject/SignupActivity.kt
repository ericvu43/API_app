package edu.iu.luddy.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        auth.currentUser?.let {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
            return
        }

        val emailField: EditText = findViewById(R.id.signup_email)
        val passField: EditText = findViewById(R.id.signup_password)
        val confirmField: EditText = findViewById(R.id.signup_confirm_password)

        val navigateButton: Button = findViewById(R.id.signup_navigate_button)
        navigateButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        val signupButton: Button = findViewById(R.id.signup_submit_button)
        signupButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val pass = passField.text.toString()
            val confirm = confirmField.text.toString()

            if (email.isBlank() || pass.isBlank() || confirm.isBlank()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signupButton.isEnabled = false

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    signupButton.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            task.exception?.localizedMessage ?: "Signup failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
