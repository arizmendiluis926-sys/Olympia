package com.example.olympia_

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.olympia_.DatabaseHelper.DatabaseHelper
import com.example.olympia_.model.Favorito
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private lateinit var gridFavoritos: GridLayout
    private lateinit var tvEstadoFavoritos: TextView

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        gridFavoritos = findViewById(R.id.gridFavoritos)
        tvEstadoFavoritos = findViewById(R.id.tvEstadoFavoritos)

        dbHelper = DatabaseHelper(this)

        menuNavegacion()
        consultarFavoritos()
    }

    override fun onResume() {
        super.onResume()

        if (::dbHelper.isInitialized) {
            consultarFavoritos()
        }
    }

    // MENU DE NAVEGACION
    private fun menuNavegacion() {

        bottomNavigation.selectedItemId = R.id.nav_favoritos

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_favoritos -> {
                    true
                }

                R.id.nav_salir -> {
                    cerrarSesionGoogle()
                    true
                }

                else -> false
            }
        }
    }

    // CERRAR SESION
    private fun cerrarSesionGoogle() {

        auth.signOut()

        lifecycleScope.launch {

            try {
                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@FavoritesActivity,
                    "Error al cerrar sesión",
                    Toast.LENGTH_SHORT
                ).show()
            }

            val intent = Intent(
                this@FavoritesActivity,
                MainActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }
    }

    // MOSTRAR LOS EJERCICIOS FAVORITOS
    private fun mostrarEjerciciosFavoritos(favoritos: List<Favorito>) {

        for (ejercicio in favoritos) {

            val vistaEjercicio = layoutInflater.inflate(
                R.layout.item_favorito,
                gridFavoritos,
                false
            )

            val imgEjercicio = vistaEjercicio.findViewById<ImageView>(
                R.id.imgFavorito
            )

            val tvNombre = vistaEjercicio.findViewById<TextView>(
                R.id.tvNombreFavorito
            )

            val tvGrupo = vistaEjercicio.findViewById<TextView>(
                R.id.tvGrupoFavorito
            )

            // CARGAR IMAGEN DEL EJERCICIO
            Glide.with(this)
                .load(ejercicio.imagen)
                .centerCrop()
                .into(imgEjercicio)

            // MOSTRAR INFORMACION
            tvNombre.text = ejercicio.nombre
            tvGrupo.text = ejercicio.grupo

            // ABRIR DETALLES DEL EJERCICIO
            vistaEjercicio.setOnClickListener {

                val intent = Intent(
                    this,
                    DetailsActivity::class.java
                )

                intent.putExtra("SLUG_EJERCICIO", ejercicio.slug)
                intent.putExtra("NOMBRE_EJERCICIO", ejercicio.nombre)
                intent.putExtra("GRUPO_EJERCICIO", ejercicio.grupo)
                intent.putExtra("EQUIPO_EJERCICIO", ejercicio.equipo)
                intent.putExtra("IMAGEN_EJERCICIO", ejercicio.imagen)

                startActivity(intent)
            }

            // UNA TARJETA POR FILA, CON SEPARACION ENTRE ELLAS
            val margen = (12 * resources.displayMetrics.density).toInt()

            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f)
            ).apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setMargins(0, 0, 0, margen)
            }

            gridFavoritos.addView(vistaEjercicio, params)
        }
    }

    // CONSULTAR FAVORITOS EN LA BASE DE DATOS
    private fun consultarFavoritos() {

        val favoritos = dbHelper.obtenerEjerciciosFavoritos()

        gridFavoritos.removeAllViews()

        if (favoritos.isEmpty()) {

            tvEstadoFavoritos.visibility = View.VISIBLE
            tvEstadoFavoritos.text = "No tienes ningún ejercicio en Favoritos"

        } else {

            tvEstadoFavoritos.visibility = View.GONE

            mostrarEjerciciosFavoritos(favoritos)
        }
    }
}