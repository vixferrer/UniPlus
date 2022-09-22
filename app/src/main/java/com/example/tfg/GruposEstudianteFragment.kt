package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_grupos_asignatura.*


class GruposEstudianteFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var grupoAdapter: GrupoAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val inf = inflater.inflate(R.layout.fragment_grupos_estudiante, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Grupos"
        setup(email ?: "", inf)
        return inf
    }

    fun setup(email: String, inf : View){
        val query = db.collection("Grupo")
        val options = FirestoreRecyclerOptions.Builder<Grupo>().setQuery(query, Grupo::class.java).build()
        val listaGrupos: RecyclerView = inf.findViewById(R.id.gruposList)

        grupoAdapter = GrupoAdapter(options)
        grupoAdapter!!.setEmail(email)
        db.collection("Usuario").document(email).get().addOnSuccessListener {
            val grupos = it.get("Grupos") as ArrayList<String>?
            if(grupos!=null && grupos.isNotEmpty())
                grupoAdapter!!.setGruposGuardados(grupos!!)
            listaGrupos.layoutManager = LinearLayoutManager(this.context)
            listaGrupos.setHasFixedSize(true)
            listaGrupos.adapter = grupoAdapter
        }
        listaGrupos.recycledViewPool.setMaxRecycledViews(0, 0)
    }

    override fun onStart() {
        super.onStart()
        grupoAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        grupoAdapter!!.stopListening()
    }

}