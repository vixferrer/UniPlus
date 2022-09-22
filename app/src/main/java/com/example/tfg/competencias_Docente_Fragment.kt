package com.example.tfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_competencias_docente.*

class competencias_Docente_Fragment : Fragment() {

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
        val inf = inflater.inflate(R.layout.fragment_competencias_docente, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Competencias"
        setup(email ?: "", inf)
        return inf
    }

    fun setup(email: String, inf: View){
        val query = db.collection("Competencia")
        val asignaturas = ArrayList<String>()
        val options = FirestoreRecyclerOptions.Builder<Competencia>().setQuery(query, Competencia::class.java).build()
        competenciaAdapter = CompetenciaAdapter(options)
        competenciaAdapter!!.setEmail(email)
        competenciaAdapter!!.setEmailVisitante(email)
        competenciaAdapter!!.setFragmentLlamador("Competencias-Docente")
        competenciaAdapter!!.setFragment(this)
        val listaCompetencias: RecyclerView = inf.findViewById(R.id.lista_competencias)

        db.collection("Usuario").document(email).get().addOnSuccessListener {
            val asignaturasDocente = it.get("Asignaturas") as ArrayList<String>
            for(item in asignaturasDocente){
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

        autoComplete.setOnItemClickListener { parent, view, position, id ->
            db.collection("Asignatura").whereEqualTo("Nombre", asignaturas[position]).get()
                .addOnSuccessListener {documentos ->
                    for(documento in documentos){
                        val competencias = documento.get("Competencias") as ArrayList<String>?
                        if(competencias!=null && competencias.isNotEmpty()){
                            competenciaAdapter!!.setSeleccionadas(competencias)
                            listaCompetencias.visibility = VISIBLE
                            textoListaVacia.visibility = GONE
                        }
                        else{
                            listaCompetencias.visibility = GONE
                            textoListaVacia.visibility = VISIBLE
                            textoListaVacia.text = "Esta asignatura aún no tiene competencias, haz click en '+' para añadir una"
                        }
                    }
                }
        }

        listaCompetencias.layoutManager = LinearLayoutManager(this.context)
        listaCompetencias.setHasFixedSize(true)
        listaCompetencias.adapter = competenciaAdapter

        listaCompetencias.recycledViewPool.setMaxRecycledViews(0, 0)

        val bNuevaCompetencia: FloatingActionButton = inf.findViewById(R.id.bAddCompetencia)
        bNuevaCompetencia.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("email", email)
            val fragNuevaCompetencia = NuevaCompetencia()
            fragNuevaCompetencia.arguments = bundle
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.add((requireView().parent as ViewGroup).id, fragNuevaCompetencia)
            transaction.hide(this)
            transaction.addToBackStack(null)
            transaction.commit()
        }
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