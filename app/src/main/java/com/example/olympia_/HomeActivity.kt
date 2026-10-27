package com.example.olympia_

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
import com.bumptech.glide.Glide
import com.example.olympia_.model.ExerciseItem
import com.example.olympia_.model.ExercisesResponse
import com.example.olympia_.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var tvResultado: TextView
    private lateinit var contenedorPiernas: LinearLayout
    private lateinit var contenedorPecho: LinearLayout
    private lateinit var contenedorEspalda: LinearLayout

    // Maximo de tarjetas por seccion (la API no trae parametro de limite)
    private val limiteResultados = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvResultado = findViewById(R.id.tvResultado)
        contenedorPiernas = findViewById(R.id.contenedorPiernas)
        contenedorPecho = findViewById(R.id.contenedorPecho)
        contenedorEspalda = findViewById(R.id.contenedorEspalda)

        // Un llamado a la API por cada grupo muscular
        obtenerEjercicios("legs", contenedorPiernas)
        obtenerEjercicios("chest", contenedorPecho)
        obtenerEjercicios("back", contenedorEspalda)
    }

    // Metodo para obtener los ejercicios de un grupo muscular
    private fun obtenerEjercicios(grupo: String, contenedor: LinearLayout) {

        RetrofitClient.instance.fetchExercisesByGroup(grupo)
            .enqueue(object : Callback<ExercisesResponse> {

                override fun onResponse(
                    call: Call<ExercisesResponse>,
                    response: Response<ExercisesResponse>
                ) {
                    val ejercicios = response.body()?.exercises ?: emptyList()

                    if (response.isSuccessful && ejercicios.isNotEmpty()) {
                        tvResultado.visibility = View.GONE
                        // Limitar a 10 resultados
                        mostrarEjercicios(ejercicios.take(limiteResultados), contenedor)
                    } else {
                        tvResultado.text = "Ejercicios no encontrados"
                        Toast.makeText(
                            this@HomeActivity,
                            "Ejercicios no encontrados ($grupo)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ExercisesResponse>, t: Throwable) {
                    tvResultado.text = "Error en la API"
                    Toast.makeText(this@HomeActivity, "Error en la API", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Metodo para mostrar las tarjetas de ejercicios
    private fun mostrarEjercicios(ejercicios: List<ExerciseItem>, contenedor: LinearLayout) {

        // Evitar tarjetas duplicadas
        contenedor.removeAllViews()

        for (ejercicio in ejercicios) {

            val tarjeta = layoutInflater.inflate(R.layout.item_exercise, contenedor, false)

            val imgEjercicio = tarjeta.findViewById<ImageView>(R.id.imgEjercicio)
            val tvNombre = tarjeta.findViewById<TextView>(R.id.tvNombreEjercicio)
            val tvEquipamiento = tarjeta.findViewById<TextView>(R.id.tvEquipamiento)

            tvNombre.text = ejercicio.name?.get("es") ?: "Sin nombre"
            tvEquipamiento.text = ejercicio.equipment?.get("es") ?: "Sin equipo"

            // Imagen (ilustracion masculina; si no existe, la femenina)
            val urlImagen = ejercicio.image?.get("male") ?: ejercicio.image?.get("female")

            Glide.with(this@HomeActivity)
                .load(urlImagen)
                .error(android.R.drawable.ic_menu_gallery)
                .into(imgEjercicio)

            contenedor.addView(tarjeta)
        }
    }
}