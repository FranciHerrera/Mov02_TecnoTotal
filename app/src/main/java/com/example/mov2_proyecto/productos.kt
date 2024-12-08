package com.example.mov2_proyecto

data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: Float = 0f,
    val fotoUrl: String
)