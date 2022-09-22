package com.example.tfg

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_ajustes_docente.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Ajustes_Docente_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var downloadUri : String = ""
    private val registrarImagenSeleccionada = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            var imageUri = result.data!!.data
            if(imageUri != null) {
                val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())
                val nombreFoto = currentDate.toString() + "fotoPerfil_comprimida.jpg"
                val storageRef = FirebaseStorage.getInstance().reference.child(nombreFoto)
                val uploadTask = storageRef.putFile(imageUri)
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
                    if (downloadUri.isNotEmpty() && downloadUri.isNotBlank()){
                        val email = arguments?.getString("email")
                        db.collection("Usuario").document(email!!).update("UrlFotoPerfil", downloadUri)
                        Picasso.with(profile_image_Ajustes.context).load(downloadUri).into(profile_image_Ajustes)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val inf = inflater.inflate(R.layout.fragment_ajustes_docente, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Ajustes"
        setup(email ?: "", inf)
        return inf
    }

    private fun setup(email: String, inf: View) {
        val bCambiarC: Button = inf.findViewById(R.id.bCambiarContra)
        val bCambiarNombre: Button = inf.findViewById(R.id.bCambiarNombreCompleto)
        val bCambiarFoto: Button = inf.findViewById(R.id.bElegirFotoAjustes)
        val bEliminar: Button = inf.findViewById(R.id.bEliminarCuenta)
        val tv: TextView = inf.findViewById(R.id.textView12)
        val et14 : EditText = inf.findViewById(R.id.textViewNombreCom)
        var esEditable = false

        //Obtenemos los datos del usuario y los mostramos:
        tv.text = email
        db.collection("Usuario").document(email).get().addOnSuccessListener {
            et14.setText(it.get("NombreCompleto") as String?)
            Picasso.with(profile_image_Ajustes.context).load(it.get("UrlFotoPerfil") as String?).into(profile_image_Ajustes)
        }

        //Para cambiar el nombre completo:
        et14.isEnabled = false
        bCambiarNombre.setOnClickListener {
            et14.isEnabled = true
            et14.isFocusable = true
            if(esEditable) {
                esEditable = false
                bCambiarNombre.setBackgroundResource(R.drawable.ic_edit)
                val p = Pattern.compile("/^[a-zA-ZÀ-ÿ\\u00f1\\u00d1]+(\\s*[a-zA-ZÀ-ÿ\\u00f1\\u00d1]*)*[a-zA-ZÀ-ÿ\\u00f1\\u00d1]+\$/g", Pattern.CASE_INSENSITIVE)
                val m = p.matcher(textViewNombreCom.text.toString())
                if(m.find())
                    showAlert("Nombre incorrecto. Ha usado caracteres no permitidos")
                else{
                    db.collection("Usuario").document(email).update("NombreCompleto", textViewNombreCom.text.toString())
                    showAlert("Nombre actualizado correctamente")
                }
                et14.isEnabled = false
            } else {
                esEditable = true
                bCambiarNombre.setBackgroundResource(R.drawable.ic_save)
            }
        }

        //Para cambiar la foto del usuario:
        bCambiarFoto.setOnClickListener {
            val galleryIntent : Intent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            registrarImagenSeleccionada.launch(galleryIntent)
        }

        //Para cambiar la contraseña del usuario:
        bCambiarC.setOnClickListener {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            showAlert("Se le ha enviado un correo para cambiar su contraseña")
       }

        //Para eliminar la cuenta:
        bEliminar.setOnClickListener {
            //Are you sure window
            val builder = AlertDialog.Builder(this.requireContext())
            builder.setMessage("¿Desea eliminar su cuenta del sistema? Se borrarán todos sus datos y tendrá que volver a registrarse")
                    .setCancelable(false)
                    .setPositiveButton("Sí, deseo borrarla") { dialog, id ->
                        //El usuario ha decidido eliminarla asi que lo eliminamos de los integrantes de sus asignaturas y además borramos todos los grupos donde este aparece:
                        db.collection("Usuario").document(email).get().addOnSuccessListener {
                            val grupos = it.get("Grupos") as ArrayList<String>?
                            val asignaturas = it.get("Asignaturas") as ArrayList<String>?
                            if(!grupos.isNullOrEmpty()){
                                for(grupo in grupos){
                                    db.collection("Grupo").document(grupo).get().addOnSuccessListener {it2: DocumentSnapshot ->
                                        val integrantes =  it2.get("Integrantes") as ArrayList<String>?
                                        if(!integrantes.isNullOrEmpty()){
                                            var eliminar = true
                                            for(integrante in integrantes){
                                                if(integrante.contains("@upm"))
                                                    eliminar = false
                                            }
                                            if(eliminar){
                                                for(integrante in integrantes){
                                                   db.collection("Usuario").document(integrante).get().addOnSuccessListener { it3: DocumentSnapshot ->
                                                       val gruposEstudiante = it3.get("Grupos") as ArrayList<String>?
                                                       gruposEstudiante!!.remove(grupo)
                                                       db.collection("Usuario").document(integrante).update("Grupos", gruposEstudiante)
                                                   }
                                                }
                                                db.collection("Grupo").document(grupo).delete()
                                            }else{
                                                integrantes!!.remove(email)
                                                db.collection("Grupo").document(grupo).update("Integrantes", integrantes)
                                            }
                                        }
                                    }
                                }
                            }
                            if(!asignaturas.isNullOrEmpty()){
                                for(asignatura in asignaturas){
                                    db.collection("Asignatura").document(asignatura).get().addOnSuccessListener {it4: DocumentSnapshot ->
                                        val integrantes =  it4.get("Integrantes") as ArrayList<String>?
                                        if(!integrantes.isNullOrEmpty()){
                                            var eliminar = true
                                            for(integrante in integrantes){
                                                if(integrante.contains("@upm"))
                                                    eliminar = false
                                            }
                                            if(eliminar){
                                                db.collection("Asignatura").document(asignatura).get().addOnSuccessListener {it5: DocumentSnapshot ->
                                                    val integrantesAsig = it5.get("Integrantes") as ArrayList<String>?
                                                    val gruposAsig = it5.get("Grupos") as ArrayList<String>?
                                                    for(g in gruposAsig!!){
                                                        db.collection("Grupo").document(g).get().addOnSuccessListener { it6: DocumentSnapshot ->
                                                            val integrantesGrupo = it6.get("Integrantes") as ArrayList<String>?
                                                            for(integranteG in integrantesGrupo!!){
                                                                db.collection("Usuario").document(integranteG).get().addOnSuccessListener {it7: DocumentSnapshot ->
                                                                    val gruposUsuario = it7.get("Grupos") as ArrayList<String>?
                                                                    gruposUsuario!!.remove(g)
                                                                    db.collection("Usuario").document(integranteG).update("Grupos", gruposUsuario!!)
                                                                }
                                                            }
                                                            db.collection("Grupo").document(g).delete()
                                                        }
                                                    }
                                                    for(x in integrantesAsig!!){
                                                        db.collection("Usuario").document(x).get().addOnSuccessListener {it6: DocumentSnapshot ->
                                                            val asignaturasUsu = it6.get("Asignaturas") as ArrayList<String>?
                                                            asignaturasUsu!!.remove(asignatura)
                                                            db.collection("Usuario").document(x).update("Asignaturas", asignaturas!!)
                                                        }
                                                    }
                                                    db.collection("Asignatura").document(asignatura).delete()
                                                }
                                            }else{
                                                integrantes!!.remove(email)
                                                db.collection("Asignatura").document(asignatura).update("Integrantes", integrantes)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val user = FirebaseAuth.getInstance().currentUser!!
                        user.delete().addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val prefs: SharedPreferences?= activity?.getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
                                        val editor = prefs?.edit()
                                        editor?.remove("email")
                                        editor?.apply()
                                        db.collection("Usuario").document(email).delete()
                                        startActivity(Intent(this.context, MainActivity::class.java))
                                    }
                        }
                    }
                    .setNegativeButton("No") { dialog, id ->
                        dialog.dismiss()
                    }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun showAlert(mensaje: String) {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setMessage(mensaje)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

}