package com.example.mov2_proyecto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
class PurchaseActivity : AppCompatActivity() {
    private lateinit var productosList: MutableList<Producto>
    private lateinit var productosAdapter: ArrayAdapter<Producto>
    private lateinit var carritoList: MutableList<Producto>
    private lateinit var carritoAdapter: ArrayAdapter<Producto>
    private lateinit var listViewProductos: ListView
    private lateinit var listViewCarrito: ListView
    private lateinit var btnIrAPagar:Button
    private lateinit var edtInicioRuta:EditText
    private lateinit var edtFinRuta:EditText
    private lateinit var btnRevisarRuta:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase)

        setSupportActionBar(findViewById(R.id.barra_purchase))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        edtInicioRuta=findViewById(R.id.edtRutaInicio)
        edtFinRuta=findViewById(R.id.edtRutaFin)
        btnIrAPagar=findViewById(R.id.btnComprar)
        btnRevisarRuta=findViewById(R.id.btnRuta)
        productosList = mutableListOf()
        carritoList = mutableListOf()

        listViewProductos = findViewById(R.id.listProductos)
        listViewCarrito = findViewById(R.id.listCarrito)

        productosAdapter = object : ArrayAdapter<Producto>(this, R.layout.item_producto, productosList) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_producto, parent, false)
                val producto = productosList[position]
                val textViewName = view.findViewById<TextView>(R.id.productName)
                val textViewDetails = view.findViewById<TextView>(R.id.productDetails)
                val imageView = view.findViewById<ImageView>(R.id.productImage)
                textViewName.text = producto.nombre
                textViewDetails.text = "${producto.precio} - ${producto.descripcion}"
                Picasso.get().load(producto.fotoUrl).into(imageView)

                return view
            }
        }


        carritoAdapter = object : ArrayAdapter<Producto>(this, R.layout.item_carrito, carritoList) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_carrito, parent, false)
                val producto = carritoList[position]
                val textViewName = view.findViewById<TextView>(R.id.productName2)
                val textViewDetails = view.findViewById<TextView>(R.id.productDetails2)
                val imageView = view.findViewById<ImageView>(R.id.productImage2)
                val btnBorrar = view.findViewById<Button>(R.id.btnBorrar)
                textViewName.text = producto.nombre
                textViewDetails.text = "${producto.precio} - ${producto.descripcion}"
                Picasso.get().load(producto.fotoUrl).into(imageView)
                btnBorrar.setOnClickListener {
                    carritoList.removeAt(position)
                    notifyDataSetChanged()
                    Toast.makeText(context, "${producto.nombre} eliminado del carrito", Toast.LENGTH_SHORT).show()
                }

                return view
            }
        }


        listViewProductos.adapter = productosAdapter
        listViewCarrito.adapter = carritoAdapter

        listViewProductos.setOnItemClickListener { _, _, position, _ ->
            val selectedProducto = productosList[position]
            carritoList.add(selectedProducto)
            carritoAdapter.notifyDataSetChanged()
            Toast.makeText(this, "${selectedProducto.nombre} agregado al carrito", Toast.LENGTH_SHORT).show()
        }

        loadProductos()
        btnIrAPagar.setOnClickListener {
            // Obtener los campos de inicio y fin de ruta
            val inicioRuta = edtInicioRuta.text.toString().trim()
            val finRuta = edtFinRuta.text.toString().trim()

            // Validar campos
            if (inicioRuta.isEmpty()) {
                Toast.makeText(this, "El campo de inicio de ruta está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (finRuta.isEmpty()) {
                Toast.makeText(this, "El campo de fin de ruta está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (carritoList.isEmpty()) {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                val userId = it.uid

                val db = FirebaseFirestore.getInstance()
                val carritoRef = db.collection("carrito")

                val batch = db.batch()

                carritoList.forEach { producto ->
                    val docRef = carritoRef.document()
                    val data = hashMapOf(
                        "id" to producto.id,
                        "nombre" to producto.nombre,
                        "descripcion" to producto.descripcion,
                        "precio" to producto.precio,
                        "fotoUrl" to producto.fotoUrl,
                        "userId" to userId,
                        "inicioRuta" to inicioRuta,
                        "finRuta" to finRuta,
                        "pago" to "no",
                        "devolucion" to "no",
                    )
                    batch.set(docRef, data) // Agregar al batch
                }

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Productos añadidos al carrito", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, PayActivity::class.java)
                        intent.putExtra("userId", userId) // Pasar el userId
                        startActivity(intent)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al añadir productos: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            }
        }
        btnRevisarRuta.setOnClickListener {
            val inicioRuta = edtInicioRuta.text.toString().trim()
            val finRuta = edtFinRuta.text.toString().trim()

            if (inicioRuta.isEmpty() || finRuta.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa ambas ubicaciones", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("inicioRuta", inicioRuta)
                intent.putExtra("finRuta", finRuta)
                startActivity(intent)
            }
        }

    }
    private fun loadProductos() {
        val db = FirebaseFirestore.getInstance()
        val productosRef = db.collection("productos")

        productosRef.get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        val id = document.getString("id") ?: "no se encontró"  // Obtén el ID del documento
                        val nombre = document.getString("nombre") ?: "Sin nombre"
                        val descripcion = document.getString("descripcion") ?: "Sin descripción"
                        val precio = document.get("precio") as? Number ?: 0f
                        val fotoUrl = document.getString("fotoUrl") ?: ""

                        val producto = Producto(id, nombre, descripcion, precio.toFloat(), fotoUrl)
                        productosList.add(producto)
                    }
                    productosAdapter.notifyDataSetChanged()
                } else {
                    Log.w("Firestore", "Error getting documents.", task.exception)
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