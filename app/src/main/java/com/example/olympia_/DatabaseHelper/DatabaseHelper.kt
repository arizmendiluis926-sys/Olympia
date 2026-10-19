
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
        private const val DATABASE_VERSION = 2

        //Informacion de las tablas en la bd
        private const val TABLE_USUARIOS = "usuarios"
        private const val COL_ID = "id"
        private const val COL_NOMBRE = "nombre"
        private const val COL_CORREO = "correo"
        private const val COL_PASSWORD = "password"

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

    }

    //METODO ONUPGRADE
    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
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

}
