package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_grupos_asignatura.*

class Grupos_Asignatura_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var grupoAdapter: GrupoAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val email = arguments?.getString("email")
        val idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_grupos_asignatura, container, false)
        setup(email ?: "", idAsignatura ?: "", inf)
        return inf
    }

    fun setup(email: String, idAsignatura: String, inf : View){
        val bAddGrupo : FloatingActionButton = inf.findViewById(R.id.bNuevoGrupo)
        if(email.contains("alumnos"))
            bAddGrupo.visibility = GONE

        val query = db.collection("Grupo")
        val options = FirestoreRecyclerOptions.Builder<Grupo>().setQuery(query, Grupo::class.java).build()
        val listaGrupos: RecyclerView = inf.findViewById(R.id.gruposList)
        val busquedaGrupos: SearchView = inf.findViewById(R.id.searchViewGrupos)

        grupoAdapter = GrupoAdapter(options)
        grupoAdapter!!.setEmail(email)
        grupoAdapter!!.setidAsignatura(idAsignatura)

        db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
            val grupos = it.get("Grupos") as ArrayList<String>?
            if(grupos!=null && grupos.isNotEmpty())
                textView15.visibility = GONE
            else{ busquedaGrupos.visibility = GONE }
            grupoAdapter!!.setGruposGuardados(grupos!!)
            listaGrupos.layoutManager = LinearLayoutManager(this.context)
            listaGrupos.setHasFixedSize(true)
            listaGrupos.adapter = grupoAdapter
        }

        listaGrupos.recycledViewPool.setMaxRecycledViews(0, 0)

        busquedaGrupos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                val originalListOptions = FirestoreRecyclerOptions.Builder<Grupo>()
                        .setQuery(query,Grupo::class.java).build()
                val filteredListQuery = query.whereGreaterThanOrEqualTo("Nombre", newText)
                        .whereLessThanOrEqualTo("Nombre",newText+"\uf8ff")
                val filteredListOptions = FirestoreRecyclerOptions.Builder<Grupo>()
                        .setQuery(filteredListQuery, Grupo::class.java).build()
                if(newText == ""){
                    grupoAdapter!!.updateOptions(originalListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                        listaGrupos.setHasFixedSize(true)
                        listaGrupos.adapter = grupoAdapter
                    }
                }
                else
                    grupoAdapter!!.updateOptions(filteredListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
                        val grupos = it.get("Grupos") as ArrayList<String>?
                        if(!grupos.isNullOrEmpty()) {
                            for(grupo in grupos){
                                db.collection("Grupo").document(grupo).get().addOnSuccessListener {it2: DocumentSnapshot ->
                                    val nombre = it2.get("Nombre") as String?
                                    if(!nombre.isNullOrEmpty()){
                                        if(!nombre.contains(newText)){
                                            grupos.remove(grupo)
                                            grupoAdapter!!.setGruposGuardados(grupos!!)
                                            listaGrupos.setHasFixedSize(true)
                                            listaGrupos.adapter = grupoAdapter
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

        //Si la persona accediendo a esta seccion es un docente podrá añadir un nuevo grupo:
        bAddGrupo.setOnClickListener{
            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("idAsignatura", idAsignatura)
            val fragNuevoGrupo = NuevoGrupoFragment()
            fragNuevoGrupo.arguments = bundle
            val activity : AppCompatActivity = this.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragNuevoGrupo).addToBackStack(null).commit()
       }
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