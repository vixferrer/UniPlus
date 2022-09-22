package com.example.tfg

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.SignInMethodQueryResult
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setup()
        session()
    }

    private fun session() {
        val prefs: SharedPreferences = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)

        if(email != null ) {
           startActivity(Intent(this, SeccionPrincipalActivity::class.java)
                        .apply { putExtra("email", email) })
        }
    }

    private fun setup() {
        title = "Autenticación"
        val auth = FirebaseAuth.getInstance()

        bInicioSesion.setOnClickListener{
            startActivity(Intent(this, InicioSesionActivity::class.java))
        }

        bContinuar.setOnClickListener{
            auth.fetchSignInMethodsForEmail(editTextTextEmailAddress.text.toString())
                    .addOnCompleteListener(OnCompleteListener<SignInMethodQueryResult> { task ->
                        val isNewUser = task.result!!.signInMethods.isEmpty()
                        if (isNewUser) {
                            //Si el correo contiene upm.es y la contraseña tiene como minimo 8 caracteres:
                            if (editTextTextEmailAddress.text.toString().contains("upm.es") && editTextTextPassword2.text.toString().length > 7)
                            {
                                //Llevamos a la página donde el usuario introduzca más datos (foto de perfil, su nombre y su matrícula en caso de ser estudiante)
                                if(editTextTextEmailAddress.text.toString().contains("alumnos"))
                                    startActivity(Intent(this, PostRegistroEstudianteActivity::class.java)
                                            .apply {
                                                putExtra("email", editTextTextEmailAddress.text.toString())
                                                putExtra( "password", editTextTextPassword2.text.toString())
                                            })
                                else {
                                    startActivity(Intent(this, PostRegistroDocenteActivity::class.java)
                                            .apply {
                                                putExtra("email", editTextTextEmailAddress.text.toString())
                                                putExtra( "password", editTextTextPassword2.text.toString())
                                            })
                                }
                            }
                            //el correo no pertenece a la upm o la contraseña es demasiado corta:
                            else{
                                showAlert("Asegúrese de usar su correo universitario y una contraseña de al menos 8 caracteres")
                            }
                        } else {
                            showAlert("Esta cuenta ya existe en el sistema, por favor inicie sesión")
                        }
                    })
        }
    }

    private fun showAlert(mensaje: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Datos incorrectos")
        builder.setMessage(mensaje)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}