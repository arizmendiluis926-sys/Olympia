package com.example.olympia_

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.olympia_.DatabaseHelper.DatabaseHelper
import com.example.olympia_.model.ExerciseDetail
import com.example.olympia_.network.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DetailsActivity : AppCompatActivity() {

    companion object{
        private const val CANAL_ID = "canal_general"
        private const val NOTIFICACION_ID = 1
        private const val CANAL_ALERTA_ID = "canal_alertas"
        private const val NOTIFICACION_ALERTA_ID = 2
        private const val CODIGO_PERMISO = 100
    }

    private lateinit var imgDetalle: ImageView
    private lateinit var tvNombreDetalle: TextView
    private lateinit var tvGrupoDetalle: TextView
    private lateinit var tvEquipoDetalle: TextView
    private lateinit var btnFavorito: ImageButton

    private lateinit var cardMusculos: View
    private lateinit var tvTituloPrincipales: TextView
    private lateinit var chipsPrincipales: ChipGroup
    private lateinit var tvTituloSecundarios: TextView
    private lateinit var chipsSecundarios: ChipGroup

    private lateinit var cardInstrucciones: View
    private lateinit var contenedorInstrucciones: LinearLayout
    private lateinit var cardErrores: View
    private lateinit var contenedorErrores: LinearLayout
    private lateinit var cardAlternativas: View
    private lateinit var contenedorAlternativas: LinearLayout

    private lateinit var db: DatabaseHelper
    private var ejercicioFavorito = false

    // Colores de la app
    private val verde = Color.parseColor("#25B82B")
    private val rojo = Color.parseColor("#D9534F")
    private val turquesa = Color.parseColor("#4DB6AC")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgDetalle = findViewById(R.id.imgDetalle)
        tvNombreDetalle = findViewById(R.id.tvNombreDetalle)
        tvGrupoDetalle = findViewById(R.id.tvGrupoDetalle)
        tvEquipoDetalle = findViewById(R.id.tvEquipoDetalle)
        btnFavorito = findViewById(R.id.btnFavorito)
        db = DatabaseHelper(this)

        cardMusculos = findViewById(R.id.cardMusculos)
        tvTituloPrincipales = findViewById(R.id.tvTituloPrincipales)
        chipsPrincipales = findViewById(R.id.chipsPrincipales)
        tvTituloSecundarios = findViewById(R.id.tvTituloSecundarios)
        chipsSecundarios = findViewById(R.id.chipsSecundarios)

        cardInstrucciones = findViewById(R.id.cardInstrucciones)
        contenedorInstrucciones = findViewById(R.id.contenedorInstrucciones)
        cardErrores = findViewById(R.id.cardErrores)
        contenedorErrores = findViewById(R.id.contenedorErrores)
        cardAlternativas = findViewById(R.id.cardAlternativas)
        contenedorAlternativas = findViewById(R.id.contenedorAlternativas)

        mostrarInformacionEjercicio()

        crearCanalNoti()

    }

    // Metodo para mostrar la informacion de un ejercicio
    private fun mostrarInformacionEjercicio() {

        val slug = intent.getStringExtra("SLUG_EJERCICIO")
        val nombre = intent.getStringExtra("NOMBRE_EJERCICIO")
        val grupo = intent.getStringExtra("GRUPO_EJERCICIO")
        val equipo = intent.getStringExtra("EQUIPO_EJERCICIO")
        val imagen = intent.getStringExtra("IMAGEN_EJERCICIO")

        tvNombreDetalle.text = nombre ?: "Sin nombre"
        tvGrupoDetalle.text = "Grupo muscular: ${grupo ?: "No disponible"}"
        tvEquipoDetalle.text = "Equipamiento: ${equipo ?: "No disponible"}"

        Glide.with(this)
            .load(imagen)
            .error(android.R.drawable.ic_menu_gallery)
            .into(imgDetalle)

        // Verificar si el ejercicio ya esta guardado como favorito
        if (!slug.isNullOrEmpty()) {
            ejercicioFavorito = db.existeEjercicioFavorito(slug)
        }
        actualizarIconoFavorito()

        // Evento del boton de favorito
        btnFavorito.setOnClickListener {

            // Sin slug no se puede identificar el ejercicio en la BD
            if (slug.isNullOrEmpty()) {
                Toast.makeText(this, "No se pudo identificar el ejercicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nombreEjercicio = nombre ?: "Sin nombre"

            if (ejercicioFavorito) {
                // Eliminar de la base de datos
                val eliminado = db.eliminarEjercicioFavorito(slug)

                if (eliminado) {
                    ejercicioFavorito = false
                    actualizarIconoFavorito()
                    Toast.makeText(this, "Ejercicio $nombreEjercicio eliminado de favoritos", Toast.LENGTH_SHORT).show()
                    mostrarNotiEliminado(nombreEjercicio)
                }
            } else {
                // Guardar en la base de datos
                val guardado = db.guardarEjercicioFavorito(
                    slug = slug,
                    nombre = nombreEjercicio,
                    grupo = grupo ?: "",
                    equipo = equipo ?: "",
                    imagen = imagen ?: ""
                )

                if (guardado) {
                    ejercicioFavorito = true
                    actualizarIconoFavorito()
                    Toast.makeText(this, "Ejercicio $nombreEjercicio agregado a favoritos", Toast.LENGTH_SHORT).show()
                    mostrarNoti()
                } else {
                    Toast.makeText(this, "Ejercicio $nombreEjercicio ya se encuentra en favoritos", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Pedir a la API musculos, instrucciones, errores y alternativas
        cargarDetalle(slug)
    }

    // Metodo para cambiar el icono segun el estado de favorito
    private fun actualizarIconoFavorito() {
        if (ejercicioFavorito) {
            btnFavorito.setImageResource(R.drawable.favorito)
        } else {
            btnFavorito.setImageResource(R.drawable.no_favorito)
        }
    }

    // Metodo para obtener el detalle completo del ejercicio
    private fun cargarDetalle(slug: String?) {

        if (slug.isNullOrEmpty()) return

        RetrofitClient.instance.fetchExerciseDetail(slug)
            .enqueue(object : Callback<ExerciseDetail> {

                override fun onResponse(
                    call: Call<ExerciseDetail>,
                    response: Response<ExerciseDetail>
                ) {
                    val detalle = response.body()

                    if (!response.isSuccessful || detalle == null) {
                        Toast.makeText(this@DetailsActivity, "No se pudo cargar el detalle", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Musculos en chips (ya vienen en espanol)
                    val principales = detalle.primaryMuscles?.mapNotNull { it.es } ?: emptyList()
                    val secundarios = detalle.secondaryMuscles?.mapNotNull { it.es } ?: emptyList()

                    if (principales.isNotEmpty() || secundarios.isNotEmpty()) {
                        cardMusculos.visibility = View.VISIBLE
                    }

                    mostrarChips(
                        tvTituloPrincipales, chipsPrincipales, principales,
                        verde, Color.WHITE
                    )
                    mostrarChips(
                        tvTituloSecundarios, chipsSecundarios, secundarios,
                        Color.parseColor("#2E3F3F"), Color.parseColor("#CFD8D8")
                    )

                    // Tarjetas con pasos numerados
                    mostrarPasos(cardInstrucciones, contenedorInstrucciones, detalle.instructions?.get("es"), verde, true)
                    mostrarPasos(cardErrores, contenedorErrores, detalle.mistakes?.get("es"), rojo, false)
                    mostrarPasos(cardAlternativas, contenedorAlternativas, detalle.alternatives?.get("es"), turquesa, true)
                }

                override fun onFailure(call: Call<ExerciseDetail>, t: Throwable) {
                    Toast.makeText(this@DetailsActivity, "Error en la API", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Crea los chips de una lista de musculos
    private fun mostrarChips(
        titulo: TextView,
        grupo: ChipGroup,
        items: List<String>,
        colorFondo: Int,
        colorTexto: Int
    ) {
        if (items.isEmpty()) return

        grupo.removeAllViews()

        for (item in items) {
            val chip = Chip(this)
            chip.text = item
            chip.isClickable = false
            chip.isCheckable = false
            chip.setTextColor(colorTexto)
            chip.chipBackgroundColor = ColorStateList.valueOf(colorFondo)
            chip.chipStrokeWidth = 0f
            grupo.addView(chip)
        }

        titulo.visibility = View.VISIBLE
        grupo.visibility = View.VISIBLE
    }

    // Crea las filas con circulo de color (numero o "!") dentro de una tarjeta
    private fun mostrarPasos(
        tarjeta: View,
        contenedor: LinearLayout,
        items: List<String>?,
        color: Int,
        numerado: Boolean
    ) {
        if (items.isNullOrEmpty()) return

        contenedor.removeAllViews()

        items.forEachIndexed { i, texto ->
            val fila = layoutInflater.inflate(R.layout.item_paso, contenedor, false)

            val tvNumero = fila.findViewById<TextView>(R.id.tvNumero)
            val tvTexto = fila.findViewById<TextView>(R.id.tvTextoPaso)

            tvNumero.text = if (numerado) "${i + 1}" else "!"
            tvNumero.backgroundTintList = ColorStateList.valueOf(color)
            tvTexto.text = texto

            contenedor.addView(fila)
        }

        tarjeta.visibility = View.VISIBLE
    }

    //Metodo para crear los canales de notificaciones
    private fun crearCanalNoti(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val adminNotificaciones = getSystemService(NotificationManager::class.java)

            // Canal 1: notificaciones generales
            val canal = NotificationChannel(
                CANAL_ID,
                "Notificaciones generales",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            canal.description = "Canal de envio de notificaciones sobre la BD"
            adminNotificaciones.createNotificationChannel(canal)

            // Canal 2: alertas con prioridad alta, vibracion y luz
            val canalAlerta = NotificationChannel(
                CANAL_ALERTA_ID,
                "Alertas de favoritos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos cuando eliminas ejercicios de tus favoritos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                enableLights(true)
                lightColor = Color.RED
            }
            adminNotificaciones.createNotificationChannel(canalAlerta)
        }

    }

    //METODO PARA MOSTRAR LA NOTIFICACION 1 (ejercicio agregado)
    private fun mostrarNoti(){

        //Verificar los permisos por el usuario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ){

            //VOLVER A PEDIR PERMISO
            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),CODIGO_PERMISO)
            Toast.makeText(this, "Autoriza el envio de notificaciones", Toast.LENGTH_SHORT).show()

            return
        }

        val notificacion = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(R.drawable.olympia_logo)
            .setContentTitle("Olympia_")
            .setContentText("Un ejercicio fue añadido a la base de datos")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat
            .from(this)
            .notify(
                NOTIFICACION_ID,
                notificacion
            )

    }

    //METODO PARA MOSTRAR LA NOTIFICACION 2
    private fun mostrarNotiEliminado(nombre: String){

        //Verificar los permisos por el usuario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ){

            ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),CODIGO_PERMISO)
            Toast.makeText(this, "Autoriza el envio de notificaciones", Toast.LENGTH_SHORT).show()

            return
        }

        val intentFavoritos = Intent(this, FavoritesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intentFavoritos,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, CANAL_ALERTA_ID)
            .setSmallIcon(R.drawable.olympia_logo)
            .setContentTitle("Favorito eliminado")
            .setContentText("Quitaste $nombre de tus favoritos")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Quitaste \"$nombre\" de tus favoritos. Puedes volver a agregarlo cuando quieras desde el detalle del ejercicio.")
            )
            .setColor(Color.parseColor("#D32F2F"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.favorito, "Ver favoritos", pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat
            .from(this)
            .notify(
                NOTIFICACION_ALERTA_ID,
                notificacion
            )

    }

}