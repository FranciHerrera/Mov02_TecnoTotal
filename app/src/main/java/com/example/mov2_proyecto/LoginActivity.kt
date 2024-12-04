package com.example.mov2_proyecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 9001
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val emailEditText = findViewById<EditText>(R.id.edtCorreo)
        val passwordEditText = findViewById<EditText>(R.id.edtContrasena)
        val loginButton = findViewById<Button>(R.id.btnIngresar)
        val irRegistro=findViewById<Button>(R.id.btnRegistrar)
        val Salir=findViewById<Button>(R.id.btnSalir)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignInButton: Button = findViewById(R.id.btnGoogle)
        googleSignInButton.setOnClickListener {
            signIn()
        }
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
        irRegistro.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        Salir.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Salir")
                setMessage("¿Estás seguro de que deseas salir de la aplicación?")
                setPositiveButton("Sí") { _, _ ->
                    finishAffinity()
                }
                setNegativeButton("No", null)
                show()
            }
        }
    }
    private fun signIn() {
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account?.idToken!!)
        } catch (e: ApiException) {
            Log.w("Login", "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.let {
                        val userId = it.uid  // ID único del usuario
                        val creationDate = System.currentTimeMillis()  // Fecha de creación actual en milisegundos

                        val userData = hashMapOf(
                            "id" to userId,
                            "email" to it.email,
                            "fecha_creacion" to creationDate,
                            "id_carrito" to "CARRITO_${userId.take(8)}"  // Genera un ID de carrito simple
                        )

                        firestore.collection("users")
                            .document(userId)  // Usar el UID del usuario para el documento
                            .set(userData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Usuario registrado exitosamente en Firestore", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al registrar el usuario en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("Login", "Error al guardar el usuario en Firestore", e)
                            }
                    }
                } else {
                    Log.w("Login", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Error al iniciar sesión con Google: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}