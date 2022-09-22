package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_editar_asignatura.*
import kotlinx.android.synthetic.main.fragment_editar_grupo.*
import java.text.SimpleDateFormat
import java.util.*

class EditarGrupo : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    var userAdapter: UserAdapter ?= null
    private var downloadUri : String = ""
    private val registrarImagenSeleccionada = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            var imageUri = result.data!!.data
            if(imageUri != null) {
                val currentDate = SimpleDateFormat("dd/M/yyyy hh:mm:ss").format(Date())
                val nombreFoto = currentDate.toString() + "fotoGrupo.jpg"
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
                        Picasso.with(profile_imageGrupo.context).load(downloadUri).into(profile_imageGrupo)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val email = arguments?.getString("email")
        val idGrupo = arguments?.getString("idGrupo")
        val idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_editar_grupo, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Editar Grupo"
        setup(email ?: "", idGrupo ?: "", idAsignatura?: "", inf)
        return inf
    }

    private fun setup(email: String, idGrupo: String, idAsignatura: String, inf: View){
        val bCambiarFoto: Button = inf.findViewById(R.id.bCambiarFotoGrupo)
        val bEliminar: Button = inf.findViewById(R.id.bEliminarGrupo)
        val bGuardar: FloatingActionButton = inf.findViewById(R.id.bGuardarGrupo)
        val nombreGrupo : EditText = inf.findViewById(R.id.editTextNombreGrupo)
        val NomGrupo : TextView = inf.findViewById(R.id.tvNombreGrupo)
        var integrantesFinales = ArrayList<String>()
        val query = db.collection("Usuario")
        var options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()
        val listaUsuarios: RecyclerView = inf.findViewById(R.id.userlist)
        val busquedaUsuarios: SearchView = inf.findViewById(R.id.searchViewIntegrantes)

        NomGrupo.bringToFront()
        userAdapter = UserAdapter(options)
        userAdapter!!.setEmail(email)
        userAdapter!!.setFragmentLlamador("NuevoGrupo")

        //cargamos los datos iniciales (foto, nombre y usuarios dentro de la asignatura)
        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener{
            Picasso.with(profile_imageGrupo.context).load(it.get("UrlFoto") as String?).into(profile_imageGrupo)
            val integrantes = it.get("Integrantes") as ArrayList<String>?
            userAdapter!!.setUsuariosSeleccionados(integrantes!!)
            listaUsuarios.layoutManager = LinearLayoutManager(this.context)
            listaUsuarios.setHasFixedSize(true)
            listaUsuarios.adapter = userAdapter
            nombreGrupo.setText(it.get("Nombre") as String?)
            val idAsignatura = it.get("Asignatura") as String?
            if (idAsignatura!=null){
                db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{it2 : DocumentSnapshot ->
                    val integrantesAsig = it2.get("Integrantes") as ArrayList<String>?
                    userAdapter!!.setIntegrantes(integrantesAsig!!)
                }
            }
        }

       listaUsuarios.recycledViewPool.setMaxRecycledViews(0, 0)

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
                    db.collection("Grupo").document(idGrupo).get().addOnSuccessListener{
                        val idAsignatura = it.get("Asignatura") as String?
                        if (idAsignatura!=null){
                            db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{it2 : DocumentSnapshot ->
                                val integrantesAsig = it2.get("Integrantes") as ArrayList<String>?
                                userAdapter!!.setIntegrantes(integrantesAsig!!)
                                listaUsuarios.setHasFixedSize(true)
                                listaUsuarios.adapter = userAdapter
                            }
                        }
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
                                            listaUsuarios.setHasFixedSize(true)
                                            listaUsuarios.adapter = userAdapter
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

        bCambiarFoto.setOnClickListener{
            val galleryIntent : Intent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            registrarImagenSeleccionada.launch(galleryIntent)
        }

        bGuardar.setOnClickListener{
            db.collection("Grupo").document(idGrupo).update("Nombre", nombreGrupo.text.toString())
            if (downloadUri != "") db.collection("Grupo").document(idGrupo).update("UrlFoto", downloadUri)

            integrantesFinales = userAdapter!!.getUsuariosSeleccionados()
            db.collection("Grupo").document(idGrupo).get().addOnSuccessListener{
                val integrantesIniciales = it.get("Integrantes") as ArrayList<String>?
                if(integrantesIniciales!=null && integrantesIniciales.isNotEmpty()){
                    val usuariosFuera = integrantesIniciales.minus(integrantesFinales)
                    for (usuario in usuariosFuera){
                        db.collection("Usuario").document(usuario).get().addOnSuccessListener {it4: DocumentSnapshot ->
                            val grupos = it4.get("Grupos") as ArrayList<String>?
                            if(grupos!=null && grupos.isNotEmpty()){
                                grupos.remove(idGrupo)
                                db.collection("Usuario").document(usuario).update("Grupos", grupos)
                            }
                        }
                    }
                    val usuariosNuevos = integrantesFinales.minus(integrantesIniciales)
                    for(usuario in usuariosNuevos){
                        db.collection("Usuario").document(usuario).get().addOnSuccessListener {it4: DocumentSnapshot ->
                            val grupos = it4.get("Grupos") as ArrayList<String>?
                            if(grupos!=null && grupos.isNotEmpty()){
                                grupos.add(idGrupo)
                                db.collection("Usuario").document(usuario).update("Grupos", grupos)
                            }
                        }
                    }
                }
                db.collection("Grupo").document(idGrupo).update("Integrantes", integrantesFinales)
            }

            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("idGrupo", idGrupo)
            bundle.putString("idAsignatura", idAsignatura)
            val fragPerfilGrupo = Perfil_Grupo_Fragment()
            fragPerfilGrupo.arguments = bundle
            val activity : AppCompatActivity = this.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilGrupo).addToBackStack("NUEVO").commit()
        }

        bEliminar.setOnClickListener{
            val builder = AlertDialog.Builder(this.requireContext())
            builder.setMessage("¿Desea eliminar este grupo del sistema? Se borrarán todos sus datos")
                    .setCancelable(false)
                    .setPositiveButton("Sí, deseo borrarlo") { dialog, id ->
                        db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                            val grupos = it.get("Grupos") as ArrayList<String>?
                            grupos!!.remove(idGrupo)
                            db.collection("Asignatura").document(idAsignatura).update("Grupos", grupos!!)
                        }
                        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener { it2: DocumentSnapshot ->
                            val integrantesGrupo = it2.get("Integrantes") as ArrayList<String>?
                            for(integranteG in integrantesGrupo!!) {
                                db.collection("Usuario").document(integranteG).get().addOnSuccessListener {it3: DocumentSnapshot ->
                                    val gruposUsuario = it3.get("Grupos") as ArrayList<String>?
                                    gruposUsuario!!.remove(idGrupo)
                                    db.collection("Usuario").document(integranteG).update("Grupos", gruposUsuario!!)
                                }
                            }
                            db.collection("Grupo").document(idGrupo).delete()
                        }

                        val bundle = Bundle()
                        bundle.putString("email", email)
                        bundle.putString("idAsignatura", idAsignatura)
                        val fragPerfilAsignatura = Perfil_Asignatura_Fragment()
                        fragPerfilAsignatura.arguments = bundle
                        val activity : AppCompatActivity = this.context as AppCompatActivity
                        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilAsignatura).addToBackStack("ELIMINACION").commit()
                    }.setNegativeButton("No") { dialog, id ->
                        dialog.dismiss()
                    }
            val alert = builder.create()
            alert.show()
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