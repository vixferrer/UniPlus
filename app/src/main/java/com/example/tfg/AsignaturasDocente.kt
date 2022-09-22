package com.example.tfg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_competencias_docente.*
import kotlinx.android.synthetic.main.header.*

class AsignaturasDocente : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var asignaturaAdapter: AsignaturaAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val inf = inflater.inflate(R.layout.fragment_asignaturas_docente, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Asignaturas"
        setup(email ?: "", inf)
        return inf
    }

    private fun setup(email: String, inf: View) {
        val query = db.collection("Asignatura")
        val options = FirestoreRecyclerOptions.Builder<Asignatura>().setQuery(query, Asignatura::class.java).build()
        val tvSinAsig : TextView = inf.findViewById(R.id.tvSinAsignaturas)
        val listaAsignaturas: RecyclerView = inf.findViewById(R.id.lista_asignaturas)

        asignaturaAdapter = AsignaturaAdapter(options)
        asignaturaAdapter!!.notifyDataSetChanged()
        asignaturaAdapter!!.setEmail(email)

        db.collection("Usuario").document(email).get().addOnSuccessListener {
            val asignaturas = it.get("Asignaturas") as ArrayList<String>?
            if(asignaturas!=null && asignaturas.isNotEmpty()){
                tvSinAsig.visibility = GONE
                listaAsignaturas.visibility = VISIBLE
                asignaturaAdapter!!.setAsignaturasGuardadas(asignaturas!!)
                listaAsignaturas.layoutManager = LinearLayoutManager(this.context)
                listaAsignaturas.setHasFixedSize(true)
                listaAsignaturas.adapter = asignaturaAdapter
            }
            else{
                listaAsignaturas.visibility = GONE
                tvSinAsig.visibility = VISIBLE
            }
        }

        listaAsignaturas.recycledViewPool.setMaxRecycledViews(0, 0)

        val bNuevaAsignatura: FloatingActionButton = inf.findViewById(R.id.bAddAsignatura)
        if(email.contains("alumnos")){
            bNuevaAsignatura.hide()
        }
        bNuevaAsignatura.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("email", email)
           val fragNuevaAsignatura = NuevaAsignatura()
            fragNuevaAsignatura.arguments = bundle
            val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
            transaction.replace(this.id, fragNuevaAsignatura).addToBackStack(null)
            transaction.commit()
        }
    }

    override fun onStart() {
        super.onStart()
        asignaturaAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        asignaturaAdapter!!.stopListening()
    }

}