package com.example.tfg

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.fragment_nueva_asignatura.*
import kotlinx.android.synthetic.main.fragment_nuevo_grupo.*
import java.text.SimpleDateFormat
import java.util.*

class NuevoGrupoFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var userAdapter: UserAdapter ?= null
    private var downloadUri : String = "https://png.pngtree.com/png-vector/20190927/ourlarge/pngtree-group-icon-png-image_1757498.jpg"

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
                        Picasso.with(profile_imageNuevoGrupo.context).load(downloadUri).into(profile_imageNuevoGrupo)
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
        val idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_nuevo_grupo, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Nuevo Grupo"
        setup(email ?: "", idAsignatura ?:"", inf)
        return inf
    }

    private fun setup(email: String, idAsignatura: String, inf : View) {
        val query = db.collection("Usuario")
        var options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()
        val listaUsuarios: RecyclerView = inf.findViewById(R.id.userlist)

        userAdapter = UserAdapter(options)
        userAdapter!!.setEmail(email)
        userAdapter!!.setFragmentLlamador("NuevoGrupo")

        db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
            val integrantes = it.get("Integrantes") as ArrayList<String>?
            userAdapter!!.setIntegrantes(integrantes!!)
            listaUsuarios.layoutManager = LinearLayoutManager(this.context)
            listaUsuarios.setHasFixedSize(true)
            listaUsuarios.adapter = userAdapter
        }

        listaUsuarios.recycledViewPool.setMaxRecycledViews(0, 0)

        val busquedaUsuarios: SearchView = inf.findViewById(R.id.searchView)
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
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
                        val integrantes = it.get("Integrantes") as ArrayList<String>?
                        userAdapter!!.setIntegrantes(integrantes!!)
                        listaUsuarios.setHasFixedSize(true)
                        listaUsuarios.adapter = userAdapter
                    }
                } else{
                    userAdapter!!.updateOptions(filteredListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
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

        val tvNombre : TextView = inf.findViewById(R.id.tvNombreAsignatura)
        tvNombre.bringToFront()
        val bFoto : Button = inf.findViewById(R.id.bElegirFotoGrupo)
        val bGuardar : FloatingActionButton = inf.findViewById(R.id.bGuardarGrupo)

        bFoto.setOnClickListener {
            val galleryIntent : Intent = Intent()
            galleryIntent.action = Intent.ACTION_GET_CONTENT
            galleryIntent.type = "image/*"
            registrarImagenSeleccionada.launch(galleryIntent)
        }

        bGuardar.setOnClickListener {
            val nombreGrupo : String = editTextNombreGrupo.text.toString()
            val usuariosGuardados : ArrayList<String> = userAdapter!!.getUsuariosSeleccionados()
            val idAutomatico : String = db.collection("Grupo").document().id

            if(nombreGrupo.isEmpty())
                showAlert("Por favor, asegúrese de introducir el nombre del grupo")
            else{
                //Guardamos este grupo en la lista de grupos de cada usuario:
                for(item in usuariosGuardados){
                    db.collection("Usuario").document(item).get().addOnSuccessListener{
                        val gruposApuntados = it.get("Grupos") as ArrayList<String>?
                        gruposApuntados?.add(idAutomatico)
                        if(gruposApuntados!= null && gruposApuntados.size>0) db.collection("Usuario").document(item).update("Grupos", gruposApuntados)
                    }
                }

                //Guardamos este grupo en la lista de grupos de esta asignatura:
                db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
                    val gruposApuntados = it.get("Grupos") as ArrayList<String>?
                    gruposApuntados?.add(idAutomatico)
                    if(gruposApuntados!= null && gruposApuntados.size>0) db.collection("Asignatura").document(idAsignatura).update("Grupos", gruposApuntados)
                }

                db.collection("Grupo").document(idAutomatico).set(
                        hashMapOf("Nombre" to nombreGrupo,
                                "UrlFoto" to downloadUri,
                                "Integrantes" to usuariosGuardados,
                                "Asignatura" to idAsignatura)
                )

                val bundle = Bundle()
                bundle.putString("email", email)
                bundle.putString("idAsignatura", idAsignatura)
                val fragPerfilAsignatura = Perfil_Asignatura_Fragment()
                fragPerfilAsignatura.arguments = bundle
                val activity : AppCompatActivity = this.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilAsignatura).addToBackStack("NUEVO").commit()
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