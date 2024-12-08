package com.example.mov2_proyecto

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class ViewRefund : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var listView: ListView
    private val refunds = mutableListOf<Refund>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_refund)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        listView = findViewById(R.id.listHistorialRefund)

        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            // Cargar devoluciones del usuario actual
            db.collection("devoluciones")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    refunds.clear()
                    for (document in querySnapshot.documents) {

                        val codigoProducto = document.getString("codigoProducto") ?: ""
                        val motivo = document.getString("motivo") ?: ""
                        val fotoUrl = document.getString("fotoUrl") ?: ""
                        val fechaDevolucionTimestamp = document.getTimestamp("fechaDevolucion")
                        val fechaDevolucion = fechaDevolucionTimestamp?.toDate()?.toString() ?: "Fecha no disponible"

                        val refund = Refund(
                            codigoProducto = codigoProducto,
                            motivo = motivo,
                            fotoUrl = fotoUrl,
                            fechaDevolucion = fechaDevolucion
                        )
                        refunds.add(refund)
                    }
                    val adapter = RefundAdapter(this, refunds)
                    listView.adapter = adapter
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar devoluciones: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }
    }
    class RefundAdapter(
        private val context: Context,
        private val refunds: List<Refund>
    ) : ArrayAdapter<Refund>(context, 0, refunds) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.refund_item, parent, false)
            val refund = refunds[position]

            val txtCodigoProducto = view.findViewById<TextView>(R.id.txtCodigoProducto)
            val txtMotivo = view.findViewById<TextView>(R.id.txtMotivo)
            val imgFotoRefund = view.findViewById<ImageView>(R.id.imgFotoRefund)
            val txtFechaDevolucion = view.findViewById<TextView>(R.id.txtFechaDevolucion)

            txtCodigoProducto.text = "Código: ${refund.codigoProducto}"
            txtMotivo.text = "Motivo: ${refund.motivo}"
            txtFechaDevolucion.text = "Fecha: ${refund.fechaDevolucion}"

            Picasso.get()
                .load(refund.fotoUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.logototal)
                .fit()
                .centerCrop()
                .into(imgFotoRefund)

            return view
        }
    }

}
