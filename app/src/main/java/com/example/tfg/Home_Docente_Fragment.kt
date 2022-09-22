package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Home_Docente_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var notificacionesEstudianteAdapter: NotificacionAdapter ?= null
    var notificacionesDocenteAdapter: NotificacionAdapterDocente ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val email = FirebaseAuth.getInstance().currentUser.email
        val inf = inflater.inflate(R.layout.fragment_home_docente, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Inicio"
        setup(email ?: "", inf)
        return inf
    }

    fun setup(email: String, inf: View){
        val query = db.collection("Usuario").document(email).collection("Notificaciones").orderBy("Created", Query.Direction.DESCENDING)
        val listaNotificaciones: RecyclerView = inf.findViewById(R.id.lista_notificaciones)
        val tvtextoNada : TextView = inf.findViewById(R.id.tvNada)
        val ivNada : ImageView = inf.findViewById(R.id.nadaNuevo)

        val optionsEst = FirestoreRecyclerOptions.Builder<Notificacion>().setQuery(query, Notificacion::class.java).build()
        val optionsDoc = FirestoreRecyclerOptions.Builder<NotificacionDocente>().setQuery(query, NotificacionDocente::class.java).build()
        notificacionesEstudianteAdapter = NotificacionAdapter(optionsEst)
        notificacionesEstudianteAdapter!!.setEmail(email)
        notificacionesEstudianteAdapter!!.setFragment(this)
        notificacionesDocenteAdapter = NotificacionAdapterDocente(optionsDoc)
        notificacionesDocenteAdapter!!.setEmail(email)
        notificacionesDocenteAdapter!!.setFragment(this)
        listaNotificaciones.layoutManager = LinearLayoutManager(this.context)
        listaNotificaciones.setHasFixedSize(true)

        if(email.contains("alumnos")){
            listaNotificaciones.adapter = notificacionesEstudianteAdapter
        }else{
            listaNotificaciones.adapter = notificacionesDocenteAdapter
        }
        listaNotificaciones.recycledViewPool.setMaxRecycledViews(0, 0)

        ivNada.visibility = GONE
        tvtextoNada.visibility = GONE

        db.collection("Usuario").document(email).collection("Notificaciones").get().addOnSuccessListener {
            if(it != null){
                if(it.size()>0){
                    ivNada.visibility = GONE
                    tvtextoNada.visibility = GONE
                    listaNotificaciones.visibility = VISIBLE
                    listaNotificaciones.bringToFront()
                }else{
                    listaNotificaciones.visibility = GONE
                    ivNada.visibility = VISIBLE
                    tvtextoNada.visibility = VISIBLE
                    ivNada.bringToFront()
                    tvtextoNada.bringToFront()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        notificacionesDocenteAdapter!!.startListening()
        notificacionesEstudianteAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        notificacionesEstudianteAdapter!!.stopListening()
        notificacionesDocenteAdapter!!.stopListening()
    }

}