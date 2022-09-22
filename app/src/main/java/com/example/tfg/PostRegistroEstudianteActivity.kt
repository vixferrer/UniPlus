package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import de.hdodenhof.circleimageview.CircleImageView
import id.zelory.compressor.Compressor
import kotlinx.android.synthetic.main.post_registro_docente.*
import kotlinx.android.synthetic.main.post_registro_docente.bContinuarHome
import kotlinx.android.synthetic.main.post_registro_docente.editTextNombreCompleto
import kotlinx.android.synthetic.main.post_registro_estudiante.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class PostRegistroEstudianteActivity : AppCompatActivity()  {
    var downloadUri: String = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_640.png"

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_registro_estudiante)

        //Setup:
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val password = bundle?.getString("password")
        setup(email ?:"")
        uploadToDB( email?:"", password?:"")
    }

    private fun setup(email: String) {
        val db = FirebaseFirestore.getInstance()

        //Secion para foto de perfil:
        bElegirFotoEstudiante.setOnClickListener {
            CropImage.startPickImageActivity(this)
        }
    }

    private fun uploadToDB(email: String, password: String) {
        val db = FirebaseFirestore.getInstance()

        bContinuarHome.setOnClickListener{
            if(editTextNombreCompleto.text.toString().isEmpty())
                showAlert("Por favor introduzca su nombre y apellidos")
            else if (tieneCaracteresExtranios(editTextNombreCompleto.text.toString())){
                showAlert("Nombre incorrecto. Ha usado caracteres no permitidos")
            }
            else if (editTextMatricula.text.toString().length < 6) {
                showAlert("Por favor introduzca su número de matrícula (6 caracteres)")
            }
            else { //Hay un nombre rellenado y la matricula es de 6 caracteres
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        db.collection("Usuario").document(email).set(
                                hashMapOf("NombreCompleto" to editTextNombreCompleto.text.toString(),
                                        "NumMatricula" to editTextMatricula.text.toString(),
                                        "UrlFotoPerfil" to downloadUri,
                                        "Rol" to "Estudiante",
                                        "Asignaturas" to ArrayList<String>(),
                                        "Grupos" to ArrayList<String>())
                        )
                        startActivity(Intent(this, SeccionPrincipalActivity::class.java)
                                .apply { putExtra("email", email) }
                        )
                    }
                    else {
                        //el usuario ya existe en la base de datos
                        showAlert("Este usuario ya existe en el sistema")
                        }
                     }
                }
        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Si el código de solicitud es relativo a la actividad de escoger imagen de nuestro dispositivo
        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageuri = CropImage.getPickImageResultUri(this, data)

            //Recortar imagen:
            CropImage.activity(imageuri).setGuidelines(CropImageView.Guidelines.ON)
                    .setRequestedSize(640,640)
                    .setAspectRatio(2,2).start(this)
        }

        //Si el código de solicitud es relativo a la actividad de recortar la imagen elegida
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            val res = CropImage.getActivityResult(data)
            if(resultCode == Activity.RESULT_OK) {
                if (res.uri.path.isNullOrEmpty()) {
                    val url = File(res.uri.path)
                    val foto: CircleImageView = findViewById(R.id.profile_image)
                    Picasso.with(this).load(url).noFade().into(foto)
                }else{
                val url = File(res.uri.path)
                val foto: CircleImageView = findViewById(R.id.profile_image)
                Picasso.with(this).load(url).into(foto)

                //comprimiendo imagen:
                val thumb_bitmap = Compressor(this)
                        .setMaxWidth(640)
                        .setMaxHeight(640)
                        .setQuality(90)
                        .compressToBitmap(url)
                val stream = ByteArrayOutputStream()
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val thumb_byte = stream.toByteArray()
                //...fin del compresor...

                //Pasamos la foto de perfil a la base de datos
                //Para obtener el nombre del archivo:
                val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())
                val nombreFoto = currentDate.toString() + "fotoPerfil_comprimida.jpg"
                val storageRef = FirebaseStorage.getInstance().reference.child(nombreFoto)
                val uploadTask = storageRef.putBytes(thumb_byte)

                val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    return@Continuation storageRef.downloadUrl
                }).addOnCompleteListener { task ->
                    if (task.isSuccessful)
                        downloadUri = task.result.toString()
                }
            }
            }
        }
    }

    private fun tieneCaracteresExtranios(cadena : String) : Boolean{
        val p = Pattern.compile("/^[a-zA-ZÀ-ÿ\\u00f1\\u00d1]+(\\s*[a-zA-ZÀ-ÿ\\u00f1\\u00d1]*)*[a-zA-ZÀ-ÿ\\u00f1\\u00d1]+\$/g", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(cadena)
        return m.find()
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