package com.example.mov2_proyecto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.barra_menu)
        setSupportActionBar(toolbar)

    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent?
        when(item.itemId){
            R.id.itmComprar -> {
                intent = Intent(applicationContext, PurchaseActivity::class.java)
                startActivity(intent)
            }
            R.id.itmContacto -> {
                intent = Intent(applicationContext, AboutActivity::class.java)
                startActivity(intent)
            }
            R.id.itmDevoluciones -> {
                intent = Intent(applicationContext, RefundActivity::class.java)
                startActivity(intent)
            }
            R.id.itmHistorial -> {
                intent = Intent(applicationContext, PurchaseRecordActivity::class.java)
                startActivity(intent)
            }
            R.id.itmNotificaciones -> {
                intent = Intent(applicationContext, NewsActivity::class.java)
                startActivity(intent)
            }
            R.id.itmInfoUsuario -> {
                intent = Intent(applicationContext, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.itmPromocion -> {
                intent = Intent(applicationContext, PromoActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}