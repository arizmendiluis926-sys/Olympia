package com.example.olympia_.DatabaseHelper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

//Crear clase DatabaseHelper que hereda la clase SQLite
class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    //Datos de la base de datos
    companion object {

        //Informacion de la BD
        private const val DATABASE_NAME = "olympia.db"
        private const val DATABASE_VERSION = 3

        //Informacion de las tablas en la bd
        private const val TABLE_USUARIOS = "usuarios"
        private const val COL_ID = "id"
        private const val COL_NOMBRE = "nombre"
        private const val COL_CORREO = "correo"
        private const val COL_PASSWORD = "password"

        //Informacion de la tabla de favoritos
        private const val TABLE_FAVORITOS = "favoritos"
        private const val COL_ID_FAVORITO = "id"
        private const val COL_SLUG = "slug"
        private const val COL_NOMBRE_EJERCICIO = "nombre_ejercicio"
        private const val COL_GRUPO = "grupo"
        private const val COL_EQUIPO = "equipo"
        private const val COL_IMAGEN = "imagen"

        //Sentencia para crear la tabla de favoritos
        private const val CREATE_TABLE_FAVORITOS = """
            CREATE TABLE $TABLE_FAVORITOS (
                $COL_ID_FAVORITO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SLUG TEXT UNIQUE NOT NULL,
                $COL_NOMBRE_EJERCICIO TEXT NOT NULL,
                $COL_GRUPO TEXT,
                $COL_EQUIPO TEXT,
                $COL_IMAGEN TEXT
            )
        """

    }

    //METODO ONCREATE - Para crear la base de datos
    override fun onCreate(db: SQLiteDatabase?) {

        //Crear tabla
        val crearTablaUsuarios = """
            CREATE TABLE $TABLE_USUARIOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_CORREO TEXT NOT NULL,
                $COL_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()

        //Crear tabla de usuarios
        db?.execSQL(crearTablaUsuarios)

        //Crear tabla de favoritos
        db?.execSQL(CREATE_TABLE_FAVORITOS)

    }

    //METODO ONUPGRADE
    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        //Verificar la version de la BD para no borrar los datos existentes
        if (oldVersion < 3) {
            db?.execSQL(CREATE_TABLE_FAVORITOS)
        }
    }

    //Metodo para registrar un usuario
    fun registrarUsuario(nombre: String, correo: String, password: String): Boolean {

        //Crear variable para escribir en la BD
        val db = writableDatabase

        //Insertar los datos
        val datos = ContentValues().apply {
            put(COL_NOMBRE, nombre)
            put(COL_CORREO, correo)
            put(COL_PASSWORD, password)
        }

        //Ejecutar insercion de datos
        val resultado = db.insert(TABLE_USUARIOS, null, datos)

        //Cerrar conexion de la BD
        db.close()

        //Regresar el valor obtenido de la insercion (Verdadero o falso)
        return resultado != -1L

    }

    //Metodo para iniciar sesion (Consulta)
    fun iniciarSesion(correo : String, password: String) : Boolean{

        //Variable que lee la base de datos
        val db = readableDatabase

        //Crear consulta
        val query_sql = """
            SELECT * FROM $TABLE_USUARIOS
            WHERE $COL_CORREO = ? AND $COL_PASSWORD = ?
        """.trimIndent()

        // Crear un cursor que permita recorrer todos los registros
        val cursor = db.rawQuery(
            query_sql, arrayOf(correo, password)
        )

        // Verificar cuantos resultados encuentra el cursor
        val usuarioEncontrado = cursor.count > 0

        cursor.close()
        db.close()

        return usuarioEncontrado

    }

    //Metodo para guardar un ejercicio en favoritos
    fun guardarEjercicioFavorito(
        slug: String,
        nombre: String,
        grupo: String,
        equipo: String,
        imagen: String
    ): Boolean {

        //Crear variable para escribir en la BD
        val db = writableDatabase

        //Valores que vienen de la API
        val valores = ContentValues().apply {
            put(COL_SLUG, slug)
            put(COL_NOMBRE_EJERCICIO, nombre)
            put(COL_GRUPO, grupo)
            put(COL_EQUIPO, equipo)
            put(COL_IMAGEN, imagen)
        }

        //Si el ejercicio ya existe, se ignora (slug es UNIQUE)
        val resultado = db.insertWithOnConflict(
            TABLE_FAVORITOS,
            null,
            valores,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        return resultado != -1L
    }

    //Metodo para eliminar un ejercicio de favoritos
    fun eliminarEjercicioFavorito(slug: String): Boolean {

        val db = writableDatabase

        val filasAfectadas = db.delete(
            TABLE_FAVORITOS,
            "$COL_SLUG = ?",
            arrayOf(slug)
        )

        return filasAfectadas > 0
    }

    //Metodo para verificar si un ejercicio ya esta en favoritos
    fun existeEjercicioFavorito(slug: String): Boolean {

        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT $COL_SLUG FROM $TABLE_FAVORITOS WHERE $COL_SLUG = ?",
            arrayOf(slug)
        )

        val existe = cursor.moveToFirst()
        cursor.close()

        return existe
    }

}