package com.example.mov2_proyecto

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import java.io.File
import java.text.SimpleDateFormat

class RegistrarProducto : AppCompatActivity() {
    private lateinit var etNombre: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecio: EditText
    private lateinit var ivFoto: ImageView
    private lateinit var btnFoto: Button
    private lateinit var btnEnviar: Button
    private lateinit var tvIdProducto: TextView
    private lateinit var btnEscanearCodigo: Button

    private var imageUri: Uri? = null
    private val storage = FirebaseStorage.getInstance()
    private var productoId: String? = null
    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_producto) // Inicializar vistas
        etNombre = findViewById(R.id.etNombre)
        etDescripcion = findViewById(R.id.etDescripcion)
        etPrecio = findViewById(R.id.etPrecio)
        ivFoto = findViewById(R.id.ivFoto)
        btnFoto = findViewById(R.id.btnFoto)
        btnEnviar = findViewById(R.id.btnEnviar)
        tvIdProducto = findViewById(R.id.tvIdProducto)
        btnEscanearCodigo = findViewById(R.id.btnEscanearCodigo)

        btnFoto.setOnClickListener {
            checkPermissions()
            showImagePickerDialog()
        }

        btnEscanearCodigo.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            integrator.setPrompt("Escanea el código de barras del producto")
            integrator.setCameraId(0)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(false)
            integrator.initiateScan()
        }
        btnEnviar.setOnClickListener {
            enviarDatosAFirebase()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data?.data != null) {
                imageUri = data.data
            } else if (imageUri != null) {
                Toast.makeText(this, "Foto tomada correctamente", Toast.LENGTH_SHORT).show()
            }
            ivFoto.setImageURI(imageUri)
        } else {

            val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents != null) {
                    productoId = result.contents
                    tvIdProducto.text = productoId // Mostrar el ID en el TextView
                } else {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun enviarDatosAFirebase() {
        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val precio = etPrecio.text.toString().trim()

        if (nombre.isEmpty() || descripcion.isEmpty() || precio.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }
        val id = productoId ?: UUID.randomUUID().toString()
        val imageRef = storage.reference.child("productos/${UUID.randomUUID()}.jpg")
        imageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { url ->
                        val producto = Producto(id, nombre, descripcion, precio.toFloat(), url.toString())
                        guardarEnFirestore(producto)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al subir la imagen.", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timestamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun showImagePickerDialog() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val imageFile = createImageFile()
        imageUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            imageFile
        )
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        val chooserIntent = Intent.createChooser(galleryIntent, "Seleccionar Imagen")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        startActivityForResult(chooserIntent, REQUEST_IMAGE_PICK)
    }
    private fun guardarEnFirestore(producto: Producto) {
        val db = FirebaseFirestore.getInstance()

        db.collection("productos")
            .document(producto.id)  // Usamos el ID generado
            .set(producto) // Guardamos el producto con su ID
            .addOnSuccessListener {
                limpiarCampos()
                Toast.makeText(this, "Producto registrado correctamente con ID: ${producto.id}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar producto: ${e.message}", Toast.LENGTH_SHORT).show()
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
    private fun limpiarCampos() {
        productoId=""
        etNombre.text.clear()
        etDescripcion.text.clear()
        etPrecio.text.clear()
        ivFoto.setImageResource(0)
        imageUri = null
    }
}