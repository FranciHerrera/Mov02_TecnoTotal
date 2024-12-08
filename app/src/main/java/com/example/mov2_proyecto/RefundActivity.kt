package com.example.mov2_proyecto

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RefundActivity : AppCompatActivity() {
    private lateinit var edtCodigoRefund: EditText
    private lateinit var btnEscanearRefund: Button
    private lateinit var btnVerificar: Button
    private lateinit var btnEvidencias: Button
    private lateinit var btnRegresarProducto:Button
    private lateinit var textRefund: TextView
    private lateinit var edtMotivo:TextView
    private lateinit var imgMotivo: ImageView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var photoUri: Uri? = null
    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refund)

        setSupportActionBar(findViewById(R.id.barra_refund))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        edtCodigoRefund = findViewById(R.id.edtCodigoRefund)
        btnEscanearRefund = findViewById(R.id.btnEscanearRefund)
        btnVerificar = findViewById(R.id.btnVerificar)
        textRefund = findViewById(R.id.textRefund)
        edtMotivo=findViewById(R.id.editTextText2)
        btnEvidencias = findViewById(R.id.btnEvidencias)
        imgMotivo = findViewById(R.id.imgMotivo)
        btnRegresarProducto=findViewById(R.id.btnDevolver)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        btnEscanearRefund.setOnClickListener {
            iniciarEscaneoCodigo()
        }

        btnVerificar.setOnClickListener {
            verificarCodigo()
        }
        btnEvidencias.setOnClickListener {
            checkPermissions()
            showImagePickerDialog()
        }
        btnRegresarProducto.setOnClickListener {
            val codigo = edtCodigoRefund.text.toString().trim()
            val motivo = edtMotivo.text.toString().trim()

            if (codigo.isEmpty() || motivo.isEmpty() || photoUri == null) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            user?.let {
                val userId = it.uid

                // Buscar el producto en la colección "carrito"
                db.collection("carrito")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("id", codigo)
                    .whereEqualTo("pago","si")
                    .whereEqualTo("devolucion", "no")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            Toast.makeText(this, "El producto no existe o ya fue devuelto.", Toast.LENGTH_SHORT).show()
                        } else {

                            val producto = querySnapshot.documents.first()
                            val productoId = producto.id


                            db.collection("carrito").document(productoId)
                                .update("devolucion", "si")
                                .addOnSuccessListener {
                                    val storageReference = FirebaseStorage.getInstance().reference
                                    val imageRef = storageReference.child("devoluciones/${productoId}_${System.currentTimeMillis()}.jpg")

                                    imageRef.putFile(photoUri!!)
                                        .addOnSuccessListener {
                                            // Obtener la URL de la imagen subida
                                            imageRef.downloadUrl.addOnSuccessListener { uri ->
                                                val devolucionData = hashMapOf(
                                                    "userId" to userId,
                                                    "codigoProducto" to codigo,
                                                    "motivo" to motivo,
                                                    "fotoUrl" to uri.toString(),
                                                    "fechaDevolucion" to FieldValue.serverTimestamp()
                                                )

                                                db.collection("devoluciones")
                                                    .add(devolucionData)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(this, "Producto regresado exitosamente", Toast.LENGTH_SHORT).show()
                                                        limpiarCampos() // Opcional, para limpiar los campos después del registro
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(this, "Error al registrar la devolución: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Error al subir la foto: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al actualizar la devolución: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al verificar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun checkPermissions() {
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.CAMERA
                ), 100
            )
        }
    }
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timestamp}_", /* prefijo */
            ".jpg",             /* sufijo */
            storageDir          /* directorio */
        )
    }
    private fun showImagePickerDialog() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val imageFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            imageFile
        )
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        val chooserIntent = Intent.createChooser(galleryIntent, "Seleccionar Imagen")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, RefundActivity.REQUEST_IMAGE_PICK)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            } else {
                edtCodigoRefund.setText(result.contents)
                Toast.makeText(this, "Código escaneado: ${result.contents}", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            if (data != null && data.data != null) {
                val selectedImageUri: Uri? = data.data
                imgMotivo.setImageURI(selectedImageUri)
            } else if (photoUri != null) {
                imgMotivo.setImageURI(photoUri)
            }
        } else {
            Toast.makeText(this, "Error al tomar la foto o seleccionar la imagen", Toast.LENGTH_SHORT).show()
        }
    }


    private fun verificarCodigo() {
        val user = auth.currentUser
        val codigo = edtCodigoRefund.text.toString().trim()

        if (codigo.isEmpty()) {
            Toast.makeText(this, "Por favor, escanea un código de barras primero.", Toast.LENGTH_SHORT).show()
            return
        }
        user?.let {
            val userId = it.uid

            db.collection("carrito")
                .whereEqualTo("userId", userId)
                .whereEqualTo("pago", "si")
                .whereEqualTo("devolucion", "no")
                .whereEqualTo("id", codigo)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        textRefund.text = "No se encontró el producto con el código: $codigo"
                    } else {
                        val producto = querySnapshot.documents.first()
                        val nombre = producto.getString("nombre") ?: "Producto no encontrado"
                        textRefund.text = "Producto encontrado: $nombre"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al verificar el producto.", Toast.LENGTH_SHORT).show()
                }
        }?:run{
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
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
    private fun limpiarCampos() {
        edtCodigoRefund.text.clear()
        textRefund.text = ""
        imgMotivo.setImageURI(null)
        photoUri = null
    }
}