package com.example.olympia_

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.olympia_.DatabaseHelper.DatabaseHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var etCorreo: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnIngresar: Button
    private lateinit var btnGoogle: Button

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Auth y Credential Manager
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Si ya hay una sesión activa en Firebase, dirigir a HomeActivity
        if (auth.currentUser != null) {
            irAHome()
            return
        }

        val tvCrearCuenta = findViewById<TextView>(R.id.tvCrearCuenta)

        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPass)
        btnIngresar = findViewById(R.id.btnIngresar)
        btnGoogle = findViewById(R.id.btnGoogle)

        dbHelper = DatabaseHelper(this)

        // Evento para iniciar sesión con BD local
        btnIngresar.setOnClickListener {
            iniciarSesion()
        }

        // Evento para iniciar sesión con Google mediante Firebase
        btnGoogle.setOnClickListener {
            iniciarSesionConGoogle()
        }

        tvCrearCuenta.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    // Método de inicio de sesión local
    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val pass = etPassword.text.toString().trim()

        if (correo.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val resultado = dbHelper.iniciarSesion(correo, pass)

        if (resultado) {
            Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
            irAHome()
        } else {
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    // Método de inicio de sesión con Google mediante Credential Manager y Firebase Auth
    private fun iniciarSesionConGoogle() {
        val webClientId = getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts = false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                Log.e("MainActivity", "Error en CredentialManager", e)
                Toast.makeText(
                    this@MainActivity,
                    "Inicio de sesión cancelado o fallido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        if ((credential is CustomCredential) && (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val nombre = user?.displayName ?: user?.email ?: "Usuario"
                            Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()
                            irAHome()
                        } else {
                            Log.e("MainActivity", "Error autenticación Firebase", task.exception)
                            Toast.makeText(
                                this,
                                "Error al conectar con Firebase: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("MainActivity", "Error al procesar token de Google", e)
                Toast.makeText(this, "Error al procesar credenciales", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Tipo de credencial no reconocido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun irAHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}