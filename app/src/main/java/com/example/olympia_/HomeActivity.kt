
package com.example.olympia_

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.olympia_.model.ExerciseItem
import com.example.olympia_.model.ExercisesResponse
import com.example.olympia_.network.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var tvResultado: TextView
    private lateinit var tvUsuario: TextView

    private lateinit var contenedorPiernas: LinearLayout
    private lateinit var contenedorPecho: LinearLayout
    private lateinit var contenedorEspalda: LinearLayout

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    // Máximo de tarjetas por sección
    private val limiteResultados = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        // Inicializar componentes
        tvResultado = findViewById(R.id.tvResultado)
        tvUsuario = findViewById(R.id.tvUsuario)

        contenedorPiernas = findViewById(R.id.contenedorPiernas)
        contenedorPecho = findViewById(R.id.contenedorPecho)
        contenedorEspalda = findViewById(R.id.contenedorEspalda)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Mostrar usuario
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val nombre = currentUser.displayName
                ?: currentUser.email
                ?: "Usuario"

            tvUsuario.text = "¡Hola, $nombre!"
        } else {
            tvUsuario.text = "¡Hola!"
        }

        // Configurar menú inferior
        menuNavegacion()

        // Consultar ejercicios en la API
        obtenerEjercicios("legs", contenedorPiernas)
        obtenerEjercicios("chest", contenedorPecho)
        obtenerEjercicios("back", contenedorEspalda)
    }

    // MENÚ DE NAVEGACIÓN
    private fun menuNavegacion() {

        bottomNavigation.selectedItemId = R.id.nav_inicio

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_inicio -> {
                    true
                }

                R.id.nav_favoritos -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_salir -> {
                    cerrarSesion()
                    true
                }

                else -> false
            }
        }
    }

    // CERRAR SESIÓN
    private fun cerrarSesion() {

        auth.signOut()

        lifecycleScope.launch {

            try {
                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@HomeActivity,
                    "Error al cerrar sesión",
                    Toast.LENGTH_SHORT
                ).show()
            }

            val intent = Intent(
                this@HomeActivity,
                MainActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
        }
    }

    // OBTENER EJERCICIOS DE LA API
    private fun obtenerEjercicios(
        grupo: String,
        contenedor: LinearLayout
    ) {

        RetrofitClient.instance.fetchExercisesByGroup(grupo)
            .enqueue(object : Callback<ExercisesResponse> {

                override fun onResponse(
                    call: Call<ExercisesResponse>,
                    response: Response<ExercisesResponse>
                ) {

                    val ejercicios =
                        response.body()?.exercises ?: emptyList()

                    if (response.isSuccessful && ejercicios.isNotEmpty()) {

                        tvResultado.visibility = View.GONE

                        mostrarEjercicios(
                            ejercicios.take(limiteResultados),
                            contenedor
                        )

                    } else {

                        tvResultado.text = "Ejercicios no encontrados"

                        Toast.makeText(
                            this@HomeActivity,
                            "Ejercicios no encontrados ($grupo)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<ExercisesResponse>,
                    t: Throwable
                ) {

                    tvResultado.text = "Error en la API"

                    Toast.makeText(
                        this@HomeActivity,
                        "Error en la API",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // MOSTRAR TARJETAS DE EJERCICIOS
    private fun mostrarEjercicios(
        ejercicios: List<ExerciseItem>,
        contenedor: LinearLayout
    ) {

        contenedor.removeAllViews()

        for (ejercicio in ejercicios) {

            val tarjeta = layoutInflater.inflate(
                R.layout.item_exercise,
                contenedor,
                false
            )

            val imgEjercicio = tarjeta.findViewById<ImageView>(
                R.id.imgEjercicio
            )

            val tvNombre = tarjeta.findViewById<TextView>(
                R.id.tvNombreEjercicio
            )

            val tvEquipamiento = tarjeta.findViewById<TextView>(
                R.id.tvEquipamiento
            )

            tvNombre.text = ejercicio.name?.get("es") ?: "Sin nombre"

            tvEquipamiento.text =
                ejercicio.equipment?.get("es") ?: "Sin equipo"

            // Cargar imagen
            val urlImagen =
                ejercicio.image?.get("male")
                    ?: ejercicio.image?.get("female")

            Glide.with(this@HomeActivity)
                .load(urlImagen)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imgEjercicio)

            // Abrir detalles del ejercicio
            tarjeta.setOnClickListener {

                val slug = ejercicio.slug

                if (slug != null) {

                    val intent = Intent(
                        this@HomeActivity,
                        DetailsActivity::class.java
                    )

                    intent.putExtra("SLUG_EJERCICIO", slug)
                    intent.putExtra(
                        "NOMBRE_EJERCICIO",
                        ejercicio.name?.get("es")
                    )
                    intent.putExtra(
                        "GRUPO_EJERCICIO",
                        ejercicio.group?.get("es")
                    )
                    intent.putExtra(
                        "EQUIPO_EJERCICIO",
                        ejercicio.equipment?.get("es")
                    )
                    intent.putExtra("IMAGEN_EJERCICIO", urlImagen)

                    startActivity(intent)
                }
            }

            contenedor.addView(tarjeta)
        }
    }
}
