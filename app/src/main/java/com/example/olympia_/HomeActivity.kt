package com.example.olympia_

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.olympia_.model.ExercisesResponse
import com.example.olympia_.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity() {

    private lateinit var tvResultado: TextView

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
        loadGroupData("legs")
    }

    private fun loadGroupData(categoryGroup: String) {
        tvResultado.text = "Cargando..."

        RetrofitClient.instance.fetchExercisesByGroup(categoryGroup)
            .enqueue(object : Callback<ExercisesResponse> {
                override fun onResponse(
                    call: Call<ExercisesResponse>,
                    response: Response<ExercisesResponse>
                ) {
                    val apiBody = response.body()

                    if (response.isSuccessful && apiBody != null) {
                        val textBuilder = StringBuilder()
                        val exerciseList = apiBody.exercises ?: emptyList()

                        for (item in exerciseList) {
                            val exerciseName = item.name?.get("es") ?: "Sin nombre"
                            val equipmentType = item.equipment?.get("es") ?: "Sin equipo"
                            textBuilder.append("• ").append(exerciseName)
                                .append("\n  Equipamiento: ").append(equipmentType)
                                .append("\n\n")
                        }

                        val groupLabel = apiBody.group?.get("es") ?: ""
                        val totalItems = apiBody.count ?: 0

                        tvResultado.text =
                            "Categoría: $groupLabel\nTotal: $totalItems\n\n$textBuilder"
                    } else {
                        Toast.makeText(this@HomeActivity, "Ejercicios no encontrados", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ExercisesResponse>, t: Throwable) {
                    Toast.makeText(this@HomeActivity, "Error en la API", Toast.LENGTH_SHORT).show()
                }
            })
    }
}