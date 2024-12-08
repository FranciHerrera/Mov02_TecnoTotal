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
import com.google.zxing.integration.android.IntentIntegrator

class PromoActivity : AppCompatActivity() {

    private lateinit var edtCodigoRefund: TextView
    private lateinit var btnEscanearRefund: Button
    private lateinit var txtMisPromociones: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promo)
        setSupportActionBar(findViewById(R.id.barra_promo))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        edtCodigoRefund = findViewById(R.id.txtPromocion)
        btnEscanearRefund = findViewById(R.id.btnEscanearPromo)
        val btnRegistrarPromo: Button = findViewById(R.id.btnRegistrarPromo)
        txtMisPromociones = findViewById(R.id.txtmisPromociones)
        btnEscanearRefund.setOnClickListener {
            iniciarEscaneoCodigo()
        }
        btnRegistrarPromo.setOnClickListener {
            registrarPromocion()
        }
        cargarPromociones()
    }

    private fun iniciarEscaneoCodigo() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Escanea el código de barras del producto")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.initiateScan()
    }

    private fun registrarPromocion() {
        val promocionId = edtCodigoRefund.text.toString()

        if (promocionId != "") {
            val userId = auth.currentUser?.uid

            if (userId != null) {
                val promocion = hashMapOf(
                    "id" to promocionId,
                    "descripcion" to "Promocion Especial",
                    "valor" to 50,
                    "userId" to userId,
                    "aplicado" to "no"
                )
                db.collection("promocion").document(promocionId.toString())
                    .set(promocion)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Promoción registrada exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        edtCodigoRefund.text = ""
                        cargarPromociones()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error al registrar la promoción: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                this,
                "Por favor, ingresa un ID de promoción válido.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun cargarPromociones() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            db.collection("promocion")
                .whereEqualTo("userId", userId)
                .whereEqualTo("aplicado", "no")
                .get()
                .addOnSuccessListener { documents ->
                    txtMisPromociones.text = ""
                    for (document in documents) {
                        val promocionId = document.getString("id")?:"no se encontro"
                        val descripcion = document.getString("descripcion") ?: "Sin descripción"
                        val valor = document.getLong("valor") ?: 0
                        val promocionInfo =
                            "ID: $promocionId, Descripción: $descripcion, Valor: $valor, Aplicado: no\n"
                        txtMisPromociones.append(promocionInfo)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error al cargar promociones: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                edtCodigoRefund.text = result.contents
                Toast.makeText(this, "Código escaneado: ${result.contents}", Toast.LENGTH_SHORT).show()
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
