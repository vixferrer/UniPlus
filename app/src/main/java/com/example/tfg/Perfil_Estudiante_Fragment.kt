package com.example.tfg

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_perfil__docente_.*

class Perfil_Estudiante_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var competenciaAdapter: CompetenciaAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val emailEstudiante = arguments?.getString("emailEstudiante")
        val emailVisitante = arguments?.getString("emailVisitante")
        val inf = inflater.inflate(R.layout.fragment_perfil_estudiante, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Perfil"
        setup(emailEstudiante ?: "", emailVisitante ?: "", inf)
        return inf
    }

    fun setup(emailEstudiante: String, emailVisitante: String, inf: View) {

        val tvNombre : TextView = inf.findViewById(R.id.NombreCompleto_Perfil)
        val tvRol : TextView = inf.findViewById(R.id.Rol_Perfil)
        val tvMatricula : TextView = inf.findViewById(R.id.NumMatricula)
        val tvCorreo : TextView = inf.findViewById(R.id.TextViewEmail)
        val tvTextoSinCompetencias : TextView = inf.findViewById(R.id.TextoSinCompetencias)
        val tvTextoMasDetalles : TextView = inf.findViewById(R.id.TextoMasDetalles)

        //Cargamos los datos del estudiante:
        tvCorreo.text = emailEstudiante
        db.collection("Usuario").document(emailEstudiante).get().addOnSuccessListener {
            tvNombre.text = it.get("NombreCompleto") as String?
            tvMatricula.text = it.get("NumMatricula") as String?
            tvRol.text = it.get("Rol") as String?
            Picasso.with(imgPerfil.context).load(it.get("UrlFotoPerfil") as String?).into(imgPerfil)
        }

        //Cargamos competencias:
        val query = db.collection("Competencia")
        val asignaturas = ArrayList<String>()
        val options = FirestoreRecyclerOptions.Builder<Competencia>().setQuery(query, Competencia::class.java).build()
        competenciaAdapter = CompetenciaAdapter(options)
        competenciaAdapter!!.setEmail(emailEstudiante)
        competenciaAdapter!!.setEmailVisitante(emailVisitante)
        competenciaAdapter!!.setFragmentLlamador("Perfil-Estudiante")
        competenciaAdapter!!.setFragment(this)
        val listaCompetencias: RecyclerView = inf.findViewById(R.id.lista_competencias)

        db.collection("Usuario").document(emailEstudiante).get().addOnSuccessListener {
            val asignaturasEstudiante = it.get("Asignaturas") as ArrayList<String>
            for(item in asignaturasEstudiante){
                db.collection("Asignatura").document(item).get().addOnSuccessListener { it2: DocumentSnapshot ->
                    val nombreAsignatura = it2.get("Nombre") as String
                    asignaturas.add(nombreAsignatura)
                }
            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, asignaturas)
        val autoComplete: AutoCompleteTextView = inf.findViewById(R.id.autoCompleteTextView)
        autoComplete.setAdapter(arrayAdapter)
        listaCompetencias.visibility = GONE
        tvTextoSinCompetencias.visibility = GONE


        autoComplete.setOnItemClickListener { parent, view, position, id ->
            db.collection("Asignatura").whereEqualTo("Nombre", asignaturas[position]).get()
                    .addOnSuccessListener {documentos ->
                        for(documento in documentos){
                            val competencias = documento.get("Competencias") as ArrayList<String>?
                            if(!competencias.isNullOrEmpty()){
                                competenciaAdapter!!.setSeleccionadas(competencias)
                                listaCompetencias.visibility = View.VISIBLE
                                tvTextoSinCompetencias.visibility = GONE
                                tvTextoMasDetalles.visibility = View.VISIBLE
                            }
                            else{
                                listaCompetencias.visibility = GONE
                                tvTextoSinCompetencias.visibility = View.VISIBLE
                                tvTextoMasDetalles.visibility = GONE
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