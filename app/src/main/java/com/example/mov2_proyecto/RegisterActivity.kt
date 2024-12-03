package com.example.mov2_proyecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val emailEditText = findViewById<EditText>(R.id.edtCorreoRegistro)
        val passwordEditText = findViewById<EditText>(R.id.edtContrasenaRegistro)
        val registerButton = findViewById<Button>(R.id.btnRegistrarUsuario)
        val irIniciar=findViewById<Button>(R.id.btnIngresarRegreso)
        val Salir=findViewById<Button>(R.id.btnSalirRegistro)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese un correo electrónico", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(email, password)
            }
        }
        irIniciar.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
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
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid  // ID único del usuario proporcionado por Firebase
                    val creationDate = System.currentTimeMillis()  // Fecha de creación actual en milisegundos

                    val userData = hashMapOf(
                        "id" to userId,
                        "email" to email,
                        "fecha_creacion" to creationDate,
                        "id_carrito" to "CARRITO_${userId?.take(8)}"  // Genera un ID de carrito usando una parte del UID
                    )

                    firestore.collection("users")
                        .document(userId!!)  // Usamos el UID como nombre de documento
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Usuario registrado exitosamente en Firestore", Toast.LENGTH_SHORT).show()
                            // Redirigir al login después de un registro exitoso
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish() // Finalizar la actividad de registro
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al registrar el usuario en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("RegisterActivity", "Error al guardar el usuario en Firestore", e)
                        }

                } else {
                    // Mostrar el error si no se pudo registrar
                    Toast.makeText(this, "Error al registrar el usuario: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Error al registrar el usuario: ${task.exception?.message}")
                }
            }
    }
}