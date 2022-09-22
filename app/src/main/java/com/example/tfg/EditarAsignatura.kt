package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_editar_asignatura.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class EditarAsignatura : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    var userAdapter: UserAdapter ?= null
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
                        Picasso.with(profile_imageNuevaAsig.context).load(downloadUri).into(profile_imageNuevaAsig)
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
        val idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_editar_asignatura, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Editar Asignatura"
        setup(email ?: "", idAsignatura ?: "", inf)
        return inf
    }

    private fun setup(email: String, idAsignatura: String, inf: View){
        val bCambiarFoto: Button = inf.findViewById(R.id.bCambiarFotoAsignatura)
        val bEliminar: Button = inf.findViewById(R.id.bEliminarAsignatura)
        val bGuardar: FloatingActionButton = inf.findViewById(R.id.bGuardarAsignatura)
        val nombreAsig : EditText = inf.findViewById(R.id.editTextNombreAsignatura)
        val NomAsignatura : TextView = inf.findViewById(R.id.tvNombreAsignatura)
        var integrantesFinales = ArrayList<String>()
        val query = db.collection("Usuario")
        var options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()
        val listaUsuarios: RecyclerView = inf.findViewById(R.id.userlist)
        val busquedaUsuarios: SearchView = inf.findViewById(R.id.searchViewIntegrantes)

        NomAsignatura.bringToFront()

        userAdapter = UserAdapter(options)
        userAdapter!!.setEmail(email)

        //cargamos los datos iniciales (foto, nombre y usuarios dentro de la asignatura)
        db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
            Picasso.with(profile_imageNuevaAsig.context).load(it.get("UrlFoto") as String?).into(profile_imageNuevaAsig)
            nombreAsig.setText(it.get("Nombre") as String?)
            val integrantesIniciales = it.get("Integrantes") as ArrayList<String>?
            userAdapter!!.setUsuariosSeleccionados(integrantesIniciales!!)
            listaUsuarios.layoutManager = LinearLayoutManager(this.context)
            listaUsuarios.setHasFixedSize(true)
            listaUsuarios.adapter = userAdapter
        }

        //Para buscar entre los usurios:
        busquedaUsuarios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                val originalListOptions = FirestoreRecyclerOptions.Builder<User>()
                    .setQuery(query,User::class.java).build()
                val filteredListQuery = query.whereGreaterThanOrEqualTo("NombreCompleto", newText)
                    .whereLessThanOrEqualTo("NombreCompleto",newText+"\uf8ff")
                val filteredListOptions = FirestoreRecyclerOptions.Builder<User>()
                    .setQuery(filteredListQuery, User::class.java).build()
                if(newText == ""){
                    userAdapter!!.updateOptions(originalListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                        listaUsuarios.setHasFixedSize(true)
                        listaUsuarios.adapter = userAdapter
                    }
                }
                else{
                    userAdapter!!.updateOptions(filteredListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                        val integrantes = it.get("Integrantes") as ArrayList<String>?
                        if(!integrantes.isNullOrEmpty()) {
                            for(integrante in integrantes){
                                db.collection("Usuario").document(integrante).get().addOnSuccessListener {it2: DocumentSnapshot ->
                                    val nombre = it2.get("NombreCompleto") as String?
                                    if(!nombre.isNullOrEmpty()){
                                        if(!nombre.contains(newText)){
                                            integrantes.remove(integrante)
                                            userAdapter!!.setIntegrantes(integrantes!!)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return false
            }

            override fun onQueryTextSubmit(text: String): Boolean {
                return false
            }
        })

        listaUsuarios.recycledViewPool.setMaxRecycledViews(0, 0)
        //Cambiar la foto:
        bCambiarFoto.setOnClickListener {
            val galleryIntent : Intent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            registrarImagenSeleccionada.launch(galleryIntent)
        }

        //Eliminar el grupo:
        bEliminar.setOnClickListener {
            val builder = AlertDialog.Builder(this.requireContext())
            builder.setMessage("¿Desea eliminar esta asignatura del sistema? Se borrarán todos sus datos")
                .setCancelable(false)
                .setPositiveButton("Sí, deseo borrarla") { dialog, id ->
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                        val integrantes = it.get("Integrantes") as ArrayList<String>?
                        val grupos = it.get("Grupos") as ArrayList<String>?
                        for(grupo in grupos!!){
                            db.collection("Grupo").document(grupo).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                val integrantesGrupo = it2.get("Integrantes") as ArrayList<String>?
                                for(integranteG in integrantesGrupo!!){
                                    db.collection("Usuario").document(integranteG).get().addOnSuccessListener {it3: DocumentSnapshot ->
                                        val gruposUsuario = it3.get("Grupos") as ArrayList<String>?
                                        gruposUsuario!!.remove(grupo)
                                        db.collection("Usuario").document(integranteG).update("Grupos", gruposUsuario!!)
                                    }
                                }
                                db.collection("Grupo").document(grupo).delete()
                            }
                        }
                        for(integrante in integrantes!!){
                            db.collection("Usuario").document(integrante).get().addOnSuccessListener {it4: DocumentSnapshot ->
                                val asignaturas = it4.get("Asignaturas") as ArrayList<String>?
                                asignaturas!!.remove(idAsignatura)
                                db.collection("Usuario").document(integrante).update("Asignaturas", asignaturas!!)
                            }
                        }
                        db.collection("Asignatura").document(idAsignatura).delete()
                    }

                    val bundle = Bundle()
                    bundle.putString("email", email)
                    val fragAsignaturas = AsignaturasDocente()
                    fragAsignaturas.arguments = bundle
                    val activity : AppCompatActivity = this.context as AppCompatActivity
                    activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragAsignaturas).addToBackStack("ELIMINACION").commit()
                }.setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        //Guardamos los nuevos datos
        bGuardar.setOnClickListener{
            db.collection("Asignatura").document(idAsignatura).update("Nombre", nombreAsig.text.toString())
            if (downloadUri != "") db.collection("Asignatura").document(idAsignatura).update("UrlFoto", downloadUri)

            integrantesFinales = userAdapter!!.getUsuariosSeleccionados()

            db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                val integrantesIniciales = it.get("Integrantes") as ArrayList<String>?
                if(integrantesIniciales!=null){
                    val usuariosFuera = integrantesIniciales.minus(integrantesFinales)
                    for (usuario in usuariosFuera){
                        db.collection("Usuario").document(usuario).get().addOnSuccessListener {it4: DocumentSnapshot ->
                            val asignaturas = it4.get("Asignaturas") as ArrayList<String>?
                            val grupos = it4.get("Grupos") as ArrayList<String>?
                            if(grupos != null && grupos.isNotEmpty()){
                                db.collection("Grupo").whereEqualTo("Asignatura", idAsignatura).get().addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        val integrantes = document.get("Integrantes") as ArrayList<String>?
                                        if(grupos.contains(document.id)){
                                            grupos.remove(document.id)
                                            if(integrantes!=null && integrantes.isNotEmpty()){
                                                integrantes.remove(usuario)
                                                db.collection("Grupo").document(document.id).update("Integrantes", integrantes)
                                            }
                                        }
                                    }
                                    db.collection("Usuario").document(usuario).update("Grupos", grupos)
                                }
                            }
                            asignaturas!!.remove(idAsignatura)
                            db.collection("Usuario").document(usuario).update("Asignaturas", asignaturas!!)
                        }
                    }
                    val usuariosNuevos = integrantesFinales.minus(integrantesIniciales)
                    for(usuario in usuariosNuevos){
                        db.collection("Usuario").document(usuario).get().addOnSuccessListener {it4: DocumentSnapshot ->
                            val asignaturas = it4.get("Asignaturas") as ArrayList<String>?
                            asignaturas!!.add(idAsignatura)
                            db.collection("Usuario").document(usuario).update("Asignaturas", asignaturas!!)
                        }
                    }
                }

                db.collection("Asignatura").document(idAsignatura).update("Integrantes", integrantesFinales)
            }

            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("idAsignatura", idAsignatura)
            val fragPerfilAsig = Perfil_Asignatura_Fragment()
            fragPerfilAsig.arguments = bundle
            val activity : AppCompatActivity = this.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilAsig).addToBackStack("NUEVO").commit()
        }
    }

    override fun onStart() {
        super.onStart()
        userAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        userAdapter!!.stopListening()
    }

}