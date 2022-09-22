package com.example.tfg

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.inicio_sesion.*
import kotlinx.android.synthetic.main.inicio_sesion.bContinuar
import kotlinx.android.synthetic.main.inicio_sesion.editTextTextEmailAddress
import kotlinx.android.synthetic.main.inicio_sesion.editTextTextPassword2

class InicioSesionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inicio_sesion)
        setup()
    }

    private fun setup() {
        bContraseniaOlvidada.setOnClickListener {
            val email = editTextTextEmailAddress.text.toString()
            if (email.isEmpty() || !email.contains("@upm.es") || !email.contains("@alumnos.upm.es")){
                showAlert("Por favor introduzca un correo válido al que enviar el correo para restablecer la contraseña")
            } else{
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                showAlert("Se le ha enviado un correo para restablecer la contraseña")
            }
        }

        bRegistro.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
        }

        bContinuar.setOnClickListener{
            FirebaseAuth.getInstance().signInWithEmailAndPassword(editTextTextEmailAddress.text.toString(),
                    editTextTextPassword2.text.toString()).addOnCompleteListener {
                if (it.isSuccessful)
                    startActivity(Intent(this, SeccionPrincipalActivity::class.java)
                            .apply { putExtra("email", editTextTextEmailAddress.text.toString()) })

                else { showAlert("Correo o contraseña incorrectos, si no recuerda la contraseña haga click en 'He olvidado mi contraseña'") }
            }
        }
    }

    private fun showAlert(mensaje: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(mensaje)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}