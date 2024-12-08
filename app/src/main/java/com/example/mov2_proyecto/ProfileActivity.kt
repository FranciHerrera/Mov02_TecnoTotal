package com.example.mov2_proyecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {
    private lateinit var textViewName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewDate: TextView
    private lateinit var btnRestablecer:Button
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(findViewById(R.id.barra_profile))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        textViewName = findViewById(R.id.textView10)
        textViewEmail = findViewById(R.id.textView11)
        textViewDate = findViewById(R.id.textView12)
        btnRestablecer = findViewById(R.id.btnRestablecer)
        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val email = document.getString("email")
                        val creationDate = document.getLong("fecha_creacion")


                        textViewName.text = it.displayName ?: "Usuario"
                        textViewEmail.text = email

                        creationDate?.let { millis ->
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            textViewDate.text = sdf.format(Date(millis))
                        }
                    } else {
                        Toast.makeText(this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al obtener datos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        btnRestablecer.setOnClickListener {
            val user = auth.currentUser

            user?.let {
                val nuevaContraseña = "123456"
                user.updatePassword(nuevaContraseña)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Contraseña restablecida a: $nuevaContraseña", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error al restablecer la contraseña: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } ?: run {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}