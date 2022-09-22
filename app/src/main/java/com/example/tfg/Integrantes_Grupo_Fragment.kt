package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore

class Integrantes_Grupo_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var userAdapter: UserAdapter ?= null

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
        val inf = inflater.inflate(R.layout.fragment_integrantes_grupo, container, false)
        db.collection("Grupo").document(idGrupo!!).get().addOnSuccessListener {
            val nombreGrupo = it.get("Nombre") as String
            val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            (activity as SeccionPrincipalActivity).supportActionBar?.title = nombreGrupo
        }
        setup(email ?: "", idGrupo ?: "", idAsignatura ?: "", inf)
        return inf
    }

    fun setup(email: String, idGrupo: String, idAsignatura: String, inf : View){
        val query = db.collection("Usuario")
        var options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()
        userAdapter = UserAdapter(options)
        userAdapter!!.setEmail(email)
        userAdapter!!.setFragmentLlamador("Integrantes")
        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener{
            val integrantes = it.get("Integrantes") as ArrayList<String>?
            userAdapter!!.setIntegrantes(integrantes!!)
            val listaUsuarios: RecyclerView = inf.findViewById(R.id.integrantesList2)
            listaUsuarios.layoutManager = LinearLayoutManager(this.context)
            listaUsuarios.setHasFixedSize(true)
            listaUsuarios.adapter = userAdapter
            listaUsuarios.recycledViewPool.setMaxRecycledViews(0, 0)
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