package com.example.tfg

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_perfil__grupo.*

class Competencias_Grupo_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var competenciaAdapter: CompetenciaAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val email = arguments?.getString("email")
        val idGrupo = arguments?.getString("idGrupo")
        var idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_competencias_grupo, container, false)
        db.collection("Grupo").document(idGrupo!!).get().addOnSuccessListener {
            val nombreGrupo = it.get("Nombre") as String
            idAsignatura = it.get("Asignatura") as String
            val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            (activity as SeccionPrincipalActivity).supportActionBar?.title = nombreGrupo
        }
        setup(email ?: "", idGrupo ?: "", idAsignatura?: "" ,inf)
        return inf
    }

    fun setup(email: String, idGrupo: String, idAsignatura: String, inf: View){
        val query = db.collection("Competencia")
        val options = FirestoreRecyclerOptions.Builder<Competencia>().setQuery(query, Competencia::class.java).build()
        val listaCompetencias: RecyclerView = inf.findViewById(R.id.competenciasList)
        val tvTextoSinCompetencias : TextView = inf.findViewById(R.id.TextoSinCompetencias)
        val tvTextoMasDetalles : TextView = inf.findViewById(R.id.TextoMasDetalles)

        competenciaAdapter = CompetenciaAdapter(options)
        competenciaAdapter!!.setFragment(this)
        competenciaAdapter!!.setEmailVisitante(email)
        competenciaAdapter!!.setidGrupo(idGrupo)
        competenciaAdapter!!.setFragmentLlamador("Perfil-Grupo")

        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener {
            val idAsig = it.get("Asignatura") as String
            if(idAsig!=null){
                db.collection("Asignatura").document(idAsig).get().addOnSuccessListener {
                    val competencias = it.get("Competencias") as ArrayList<String>
                    if(!competencias.isNullOrEmpty()){
                        competenciaAdapter!!.setSeleccionadas(competencias)
                        listaCompetencias.visibility = View.VISIBLE
                        tvTextoSinCompetencias.visibility = View.GONE
                        tvTextoMasDetalles.visibility = View.VISIBLE
                    }else{
                        listaCompetencias.visibility = View.GONE
                        tvTextoSinCompetencias.visibility = View.VISIBLE
                        tvTextoMasDetalles.visibility = View.GONE
                    }
                }
            }
        }

        listaCompetencias.layoutManager = LinearLayoutManager(this.context)
        listaCompetencias.setHasFixedSize(true)
        listaCompetencias.adapter = competenciaAdapter
        listaCompetencias.recycledViewPool.setMaxRecycledViews(0, 0)
    }

    override fun onStart() {
        super.onStart()
        competenciaAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        competenciaAdapter!!.stopListening()
    }

}