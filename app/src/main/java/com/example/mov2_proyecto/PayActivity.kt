package com.example.mov2_proyecto
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PayActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var textResumenCompra: TextView
    private lateinit var swPromocion: Switch
    private lateinit var btnPagar: Button
    private val CHANNEL_ID = "payment_channel"
    private val REQUEST_CODE_POST_NOTIFICATIONS = 1
    private var valorDescuento = 0.0  // Valor del descuento de la promoción
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textResumenCompra = findViewById(R.id.textResumenCompra)
        swPromocion = findViewById(R.id.swPromocion)
        btnPagar = findViewById(R.id.btnPagar)
        loadCartData()
        createNotificationChannel()

        btnPagar.setOnClickListener {
            verificarYSolicitarPermisoNotificaciones()
        }
    }
    private fun loadCartData() {
        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            firestore.collection("carrito")
                .whereEqualTo("userId", userId) // Filtrar por el usuario
                .whereEqualTo("pago", "no")    // Filtrar solo los productos que no se han pagado
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val resumen = StringBuilder()
                        var total = 0.0

                        for (document in querySnapshot.documents) {
                            val nombre = document.getString("nombre") ?: "Sin nombre"
                            val precio = document.getDouble("precio") ?: 0.0
                            resumen.append("$nombre - $ $precio\n")
                            total += precio
                        }

                        // Obtener el valor de la promoción
                        if (swPromocion.isChecked) {
                            obtenerPromocion { valor ->
                                valorDescuento = valor
                                val totalConDescuento = total - valorDescuento
                                textResumenCompra.text = resumen.toString()
                                findViewById<TextView>(R.id.textView15).text = "Total: $ $totalConDescuento"
                            }
                        } else {
                            textResumenCompra.text = resumen.toString()
                            findViewById<TextView>(R.id.textView15).text = "Total: $ $total"
                        }
                    } else {
                        textResumenCompra.text = "No hay productos pendientes de pago"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar el carrito: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
        }
    }
    private fun obtenerPromocion(callback: (Double) -> Unit) {
        firestore.collection("promociones")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val promocion = querySnapshot.documents.firstOrNull()
                    val valor = promocion?.getDouble("valor") ?: 0.0
                    callback(valor)
                } else {
                    callback(0.0)  // Si no hay promoción, no se aplica descuento
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener promoción: ${e.message}", Toast.LENGTH_SHORT).show()
                callback(0.0)  // En caso de error, no aplicar descuento
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    procesarPagoYEnviarNotificacion()
                } catch (e: SecurityException) {
                    Toast.makeText(this, "No se pueden enviar notificaciones.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Permiso para notificaciones denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun verificarYSolicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS)
            } else {
                try {
                    procesarPagoYEnviarNotificacion()
                } catch (e: SecurityException) {
                    Toast.makeText(this, "No se pueden enviar notificaciones sin permiso.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            procesarPagoYEnviarNotificacion()
        }
    }
    private fun procesarPagoYEnviarNotificacion() {
        val user = auth.currentUser
        user?.let {
            val userId = it.uid
            firestore.collection("carrito")
                .whereEqualTo("userId", userId)
                .whereEqualTo("pago", "no")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val productosComprados = StringBuilder()
                        var totalFinal = 0.0

                        // Verificamos si hay una promoción activa
                        if (swPromocion.isChecked) {
                            verificarPromocion(userId) { descuento ->
                                for (document in querySnapshot.documents) {
                                    val documentId = document.id
                                    val nombre = document.getString("nombre") ?: "Sin nombre"
                                    val precio = document.getDouble("precio") ?: 0.0
                                    productosComprados.append("$nombre\n")
                                    totalFinal += precio
                                }

                                val totalConDescuento = if (descuento > 0.0) {
                                    totalFinal - descuento
                                } else {
                                    totalFinal
                                }

                                querySnapshot.documents.forEach { document ->
                                    val documentId = document.id
                                    firestore.collection("carrito")
                                        .document(documentId)
                                        .update("pago", "si")
                                }

                                actualizarPromocionAplicada()

                                mostrarNotificacion(productosComprados.toString(), totalConDescuento)

                                val intent = Intent(this, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            for (document in querySnapshot.documents) {
                                val nombre = document.getString("nombre") ?: "Sin nombre"
                                productosComprados.append("$nombre\n")
                                totalFinal += document.getDouble("precio") ?: 0.0
                            }
                            mostrarNotificacion(productosComprados.toString(), totalFinal)
                            querySnapshot.documents.forEach { document ->
                                val documentId = document.id
                                firestore.collection("carrito")
                                    .document(documentId)
                                    .update("pago", "si")
                            }

                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "No hay productos en el carrito para pagar", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al procesar el pago: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun verificarPromocion(userId: String, callback: (Double) -> Unit) {
        firestore.collection("promocion")
            .whereEqualTo("userId", userId) // Filtramos por el userId
            .whereEqualTo("aplicado", "no") // Filtramos por promociones no aplicadas
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    // Suponemos que tomamos la primera promoción que cumple con los requisitos
                    val promocion = querySnapshot.documents.firstOrNull()
                    promocion?.let {
                        val valorDescuento = it.getDouble("valor") ?: 0.0
                        callback(valorDescuento) // Llamamos al callback pasando el descuento
                    }
                } else {
                    // Si no se encuentra una promoción válida
                    callback(0.0)
                    Toast.makeText(this, "No tienes promociones válidas", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar promoción: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarPromocionAplicada() {
        firestore.collection("promocion")
            .whereEqualTo("userId", auth.currentUser?.uid)
            .whereEqualTo("aplicado", "no")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.documents.isNotEmpty()) {
                    val promocion = querySnapshot.documents.firstOrNull()
                    promocion?.let {
                        val promocionId = it.id
                        firestore.collection("promocion")
                            .document(promocionId)
                            .update("aplicado", "sí")
                            .addOnSuccessListener {
                                Log.d("Promocion", "Promoción aplicada exitosamente")
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al aplicar promoción: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener promociones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun mostrarNotificacion(productos: String, total: Double) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logototal)
            .setContentTitle("Compra realizada")
            .setContentText("Has comprado: $productos por un total de $total")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Has comprado:\n$productos por un total de $total"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
        Toast.makeText(this, "Compra realizada exitosamente", Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones de Pago"
            val descriptionText = "Canal para notificaciones de pago"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}