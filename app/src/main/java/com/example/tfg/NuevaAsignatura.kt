package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
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
import kotlinx.android.synthetic.main.fragment_nueva_asignatura.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class NuevaAsignatura : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var userAdapter: UserAdapter ?= null
    private var downloadUri : String = "https://www.definicionabc.com/wp-content/uploads/Asignatura.jpg"

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
                    if (task.isSuccessful) {
                        downloadUri = task.result.toString()
                        Picasso.with(profile_imageNuevaAsig.context).load(downloadUri).into(profile_imageNuevaAsig)
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
        val inf = inflater.inflate(R.layout.fragment_nueva_asignatura, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Nueva Asignatura"
        setup(email ?: "", inf)
        return inf
    }

    private fun setup(email: String, inf : View){
        val query = db.collection("Usuario")
        var options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()
        val listaUsuarios: RecyclerView = inf.findViewById(R.id.userlist)
        val busquedaUsuarios: SearchView = inf.findViewById(R.id.searchView)

        userAdapter = UserAdapter(options)
        userAdapter!!.setEmail(email)
        userAdapter!!.setFragmentLlamador("NuevoGrupo")

        db.collection("Usuario").get().addOnSuccessListener {
            val usuarios = ArrayList<String>()
            for(document in it.documents){
                usuarios.add(document.id)
            }
            userAdapter!!.setIntegrantes(usuarios)
            listaUsuarios.layoutManager = LinearLayoutManager(this.context)
            listaUsuarios.setHasFixedSize(true)
            listaUsuarios.adapter = userAdapter
        }
        listaUsuarios.recycledViewPool.setMaxRecycledViews(0, 0)

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
                    db.collection("Usuario").get().addOnSuccessListener {
                        val usuarios = ArrayList<String>()
                        for(document in it.documents){
                            usuarios.add(document.id)
                        }
                        userAdapter!!.setIntegrantes(usuarios)
                        listaUsuarios.setHasFixedSize(true)
                        listaUsuarios.adapter = userAdapter
                    }
                }
                else{
                    userAdapter!!.updateOptions(filteredListOptions)
                    db.collection("Usuario").get().addOnSuccessListener {
                        val usuarios = ArrayList<String>()
                        for(document in it.documents){
                            val nombre = document.get("NombreCompleto") as String?
                            if(!nombre.isNullOrEmpty()){
                                if(!nombre.contains(newText)){
                                    usuarios.remove(document.id)
                                    userAdapter!!.setIntegrantes(usuarios!!)
                                    listaUsuarios.setHasFixedSize(true)
                                    listaUsuarios.adapter = userAdapter
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

        val tvNombre : TextView = inf.findViewById(R.id.tvNombreAsignatura)
        tvNombre.bringToFront()
        val bFoto : Button = inf.findViewById(R.id.bElegirFotoAsignatura)
        val bGuardar : FloatingActionButton = inf.findViewById(R.id.bGuardarAsignatura)

        bFoto.setOnClickListener {
            val galleryIntent : Intent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            registrarImagenSeleccionada.launch(galleryIntent)
        }

        bGuardar.setOnClickListener{
            val nombreAsignatura : String = editTextNombreAsignatura.text.toString()
            val bundle = Bundle()
            val fragAsignaturasDoc = AsignaturasDocente()
            val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
            val usuariosGuardados : ArrayList<String> = userAdapter!!.getUsuariosSeleccionados()
            val gruposGuardados = ArrayList<String>()
            val competencias = ArrayList<String>()
            val idAutomatico : String = db.collection("Asignatura").document().id

            if(nombreAsignatura.isEmpty())
                showAlert("Por favor, asegúrese de introducir el nombre de la asignatura")
            else{
                for(item in usuariosGuardados){
                    db.collection("Usuario").document(item).get().addOnSuccessListener{
                        val asignaturasApuntadas = it.get("Asignaturas") as ArrayList<String>?
                        asignaturasApuntadas?.add(idAutomatico)
                        if(asignaturasApuntadas!= null && asignaturasApuntadas.size>0) db.collection("Usuario").document(item).update("Asignaturas", asignaturasApuntadas)
                    }
                }

                db.collection("Asignatura").document(idAutomatico).set(
                        hashMapOf("Nombre" to nombreAsignatura,
                                "UrlFoto" to downloadUri,
                                "Integrantes" to usuariosGuardados,
                                "Grupos" to gruposGuardados,
                                "Competencias" to competencias)
                )

                bundle.putString("email", email)
                fragAsignaturasDoc.arguments = bundle
                transaction.replace(this.id, fragAsignaturasDoc).addToBackStack("NUEVO")
                transaction.commit()
            }
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

    private fun showAlert(mensaje: String) {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setMessage(mensaje)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}
