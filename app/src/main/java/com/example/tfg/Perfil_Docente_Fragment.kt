package com.example.tfg

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_ajustes_docente.*
import kotlinx.android.synthetic.main.fragment_perfil__docente_.*

class Perfil_Docente_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private var asignaturaAdapter: AsignaturaAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val emailDocente = arguments?.getString("emailDocente")
        val emailVisitante = arguments?.getString("emailVisitante")
        val inf = inflater.inflate(R.layout.fragment_perfil__docente_, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Perfil"
        setup(emailDocente ?: "", emailVisitante ?: "",inf)
        return inf
    }

    fun setup(email: String, emailVisitante: String, inf: View) {
        val db = FirebaseFirestore.getInstance()

        val tvNombre : TextView = inf.findViewById(R.id.NombreCompleto_Perfil)
        val tvRol : TextView = inf.findViewById(R.id.Rol_Perfil)
        val tvCorreo : TextView = inf.findViewById(R.id.TextViewEmail)
        val tvPrevioRecycler : TextView = inf.findViewById(R.id.TextoAsignaturas)

        //Cargamos asignaturas:
        val query = db.collection("Asignatura")
        val options = FirestoreRecyclerOptions.Builder<Asignatura>().setQuery(query, Asignatura::class.java).build()
        val listaAsignaturas: RecyclerView = inf.findViewById(R.id.lista_asignaturas)

        asignaturaAdapter = AsignaturaAdapter(options)
        asignaturaAdapter!!.setEmail(emailVisitante)
        asignaturaAdapter!!.setFragmentLlamador("Perfil")

        tvCorreo.text = email

        db.collection("Usuario").document(email).get().addOnSuccessListener {
            tvNombre.text = it.get("NombreCompleto") as String?
            tvRol.text = it.get("Rol") as String?
            Picasso.with(imgPerfil.context).load(it.get("UrlFotoPerfil") as String?).into(imgPerfil)
            val asignaturas = it.get("Asignaturas") as ArrayList<String>?
            if(asignaturas!=null && asignaturas.isNotEmpty()){
                asignaturaAdapter!!.setAsignaturasGuardadas(asignaturas!!)
                listaAsignaturas.layoutManager = LinearLayoutManager(this.context)
                listaAsignaturas.setHasFixedSize(true)
                listaAsignaturas.adapter = asignaturaAdapter
                listaAsignaturas.recycledViewPool.setMaxRecycledViews(0, 0)
            }
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