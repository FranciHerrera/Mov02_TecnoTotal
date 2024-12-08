package com.example.mov2_proyecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class PurchaseRecordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var listHistorial: ListView
    private lateinit var purchasesList: MutableList<Purchase> // Lista para almacenar los productos
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_record)

        setSupportActionBar(findViewById(R.id.barra_purchase_record))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        listHistorial = findViewById(R.id.listHistorial)
        purchasesList = mutableListOf()

        loadPurchaseHistory()
    }
    private fun loadPurchaseHistory() {
        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            firestore.collection("carrito")
                .whereEqualTo("userId", userId)
                .whereEqualTo("pago", "si")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {

                        purchasesList.clear()

                        for (document in querySnapshot.documents) {
                            val nombre = document.getString("nombre") ?: "Sin nombre"
                            val precio = document.getDouble("precio") ?: 0.0
                            val imagenUrl = document.getString("fotoUrl") ?: ""
                            val devolucion = document.getString("devolucion") ?: "no"
                            val purchase = Purchase(nombre, precio, imagenUrl,devolucion)
                            purchasesList.add(purchase)
                        }

                        val adapter = PurchaseAdapter(this, purchasesList)
                        listHistorial.adapter = adapter
                    } else {
                        Toast.makeText(this, "No hay productos pagados.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar el historial: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    class PurchaseAdapter(
        context: PurchaseRecordActivity,
        private val purchases: List<Purchase>
    ) : ArrayAdapter<Purchase>(context, 0, purchases) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val purchase = getItem(position)
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_purchase, parent, false)

            val imageView: ImageView = view.findViewById(R.id.imageView)
            val textViewNombre: TextView = view.findViewById(R.id.textViewNombre)
            val textViewPrecio: TextView = view.findViewById(R.id.textViewPrecio)
            val textViewDevolucion: TextView = view.findViewById(R.id.textViewDevolucion)

            Picasso.get().load(purchase?.imagenUrl).into(imageView)

            textViewNombre.text = purchase?.nombre
            textViewPrecio.text = "$${purchase?.precio}"

            if (purchase?.devolucion == "si") {
                textViewDevolucion.visibility = View.VISIBLE
                textViewDevolucion.text = "SE HA DEVUELTO"
            } else {
                textViewDevolucion.visibility = View.GONE
            }

            return view
        }
    }
}