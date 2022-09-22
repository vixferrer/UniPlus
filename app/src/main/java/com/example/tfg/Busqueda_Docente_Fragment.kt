package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class Busqueda_Docente_Fragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var asignaturaAdapter: AsignaturaAdapter ?= null
    var usuarioAdapter: UserAdapter ?= null
    var comAdapter: CompetenciaAdapter ?= null
    var grupoAdapter: GrupoAdapter ?= null
    var concatAdapter: ConcatAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val inf = inflater.inflate(R.layout.fragment_busqueda__docente_, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Búsqueda"
        setup(email ?: "", inf)
        return inf
    }

    private fun setup(email: String, inf: View) {
        val lista: RecyclerView = inf.findViewById(R.id.lista_mixta)
        val busquedaTodo: SearchView = inf.findViewById(R.id.searchViewTodo)
        val optionsAsignatura = FirestoreRecyclerOptions.Builder<Asignatura>().setQuery(db.collection("Asignatura"), Asignatura::class.java).build()
        val optionsUsuario = FirestoreRecyclerOptions.Builder<User>().setQuery(db.collection("Usuario"), User::class.java).build()
        val optionsCom = FirestoreRecyclerOptions.Builder<Competencia>().setQuery(db.collection("Competencia"), Competencia::class.java).build()
        val optionsGrupo = FirestoreRecyclerOptions.Builder<Grupo>().setQuery(db.collection("Grupo"), Grupo::class.java).build()

        asignaturaAdapter = AsignaturaAdapter(optionsAsignatura)
        usuarioAdapter = UserAdapter(optionsUsuario)
        comAdapter = CompetenciaAdapter(optionsCom)
        grupoAdapter = GrupoAdapter(optionsGrupo)

        db.collection("Asignatura").get().addOnSuccessListener {
            val asignaturas = ArrayList<String>()
            for(doc in it.documents)
                asignaturas.add(doc.id)
            asignaturaAdapter!!.setAsignaturasGuardadas(asignaturas)
            asignaturaAdapter!!.setEmail(email)
            asignaturaAdapter!!.setFragmentLlamador("Perfil")
        }
        db.collection("Usuario").get().addOnSuccessListener {
            val usuarios = ArrayList<String>()
            for(doc in it.documents)
                usuarios.add(doc.id)
            usuarioAdapter!!.setIntegrantes(usuarios)
            usuarioAdapter!!.setEmail(email)
            usuarioAdapter!!.setFragmentLlamador("Integrantes")
        }
        db.collection("Competencia").get().addOnSuccessListener {
            val competencias = ArrayList<String>()
            for(doc in it.documents)
                competencias.add(doc.id)
            comAdapter!!.setSeleccionadas(competencias)
            comAdapter!!.setEmail(email)
            comAdapter!!.setEmailVisitante(email)
            comAdapter!!.setFragmentLlamador("Competencias-Docente")
            comAdapter!!.setFragment(this)
        }
        db.collection("Grupo").get().addOnSuccessListener {
            val grupos = ArrayList<String>()
            for(doc in it.documents)
                grupos.add(doc.id)
            grupoAdapter!!.setGruposGuardados(grupos)
            grupoAdapter!!.setEmail(email)
        }

        lista.layoutManager = LinearLayoutManager(this.context)
        lista.setHasFixedSize(true)
        concatAdapter = ConcatAdapter(asignaturaAdapter, usuarioAdapter, comAdapter, grupoAdapter)
        lista.adapter = concatAdapter

        busquedaTodo.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                val originalListOptionsUser = FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(db.collection("Usuario"), User::class.java).build()
                val originalListOptionsAsignatura = FirestoreRecyclerOptions.Builder<Asignatura>()
                        .setQuery(db.collection("Asignatura"), Asignatura::class.java).build()
                val originalListOptionsCompetencia = FirestoreRecyclerOptions.Builder<Competencia>()
                        .setQuery(db.collection("Competencia"), Competencia::class.java).build()
                val originalListOptionsGrupo = FirestoreRecyclerOptions.Builder<Grupo>()
                        .setQuery(db.collection("Grupo"), Grupo::class.java).build()

                val filteredListQueryUser = db.collection("Usuario").whereGreaterThanOrEqualTo("NombreCompleto", newText)
                        .whereLessThanOrEqualTo("NombreCompleto",newText+"\uf8ff")
                val filteredListQueryAsignatura = db.collection("Asignatura").whereGreaterThanOrEqualTo("Nombre", newText)
                        .whereLessThanOrEqualTo("Nombre",newText+"\uf8ff")
                val filteredListQueryCompetencia = db.collection("Competencia").whereGreaterThanOrEqualTo("Codigo", newText)
                        .whereLessThanOrEqualTo("Codigo",newText+"\uf8ff")
                val filteredListQueryGrupo = db.collection("Grupo").whereGreaterThanOrEqualTo("Nombre", newText)
                        .whereLessThanOrEqualTo("Nombre",newText+"\uf8ff")

                val filteredListOptionsUser = FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(filteredListQueryUser, User::class.java).build()
                val filteredListOptionsAsignatura = FirestoreRecyclerOptions.Builder<Asignatura>()
                        .setQuery(filteredListQueryAsignatura, Asignatura::class.java).build()
                val filteredListOptionsCompetencia = FirestoreRecyclerOptions.Builder<Competencia>()
                        .setQuery(filteredListQueryCompetencia, Competencia::class.java).build()
                val filteredListOptionsGrupo = FirestoreRecyclerOptions.Builder<Grupo>()
                        .setQuery(filteredListQueryGrupo, Grupo::class.java).build()

                if(newText == ""){
                    usuarioAdapter!!.updateOptions(originalListOptionsUser)
                    asignaturaAdapter!!.updateOptions(originalListOptionsAsignatura)
                    comAdapter!!.updateOptions(originalListOptionsCompetencia)
                    grupoAdapter!!.updateOptions(originalListOptionsGrupo)

                    db.collection("Usuario").get().addOnSuccessListener{
                        lista.setHasFixedSize(true)
                        lista.adapter = concatAdapter
                    }
                    db.collection("Asignatura").get().addOnSuccessListener{
                        lista.setHasFixedSize(true)
                        lista.adapter = concatAdapter
                    }
                    db.collection("Competencia").get().addOnSuccessListener{
                        lista.setHasFixedSize(true)
                        lista.adapter = concatAdapter
                    }
                    db.collection("Grupo").get().addOnSuccessListener{
                        lista.setHasFixedSize(true)
                        lista.adapter = concatAdapter
                    }

                }
                else{
                    usuarioAdapter!!.updateOptions(filteredListOptionsUser)
                    asignaturaAdapter!!.updateOptions(filteredListOptionsAsignatura)
                    comAdapter!!.updateOptions(filteredListOptionsCompetencia)
                    grupoAdapter!!.updateOptions(filteredListOptionsGrupo)

                    db.collection("Usuario").get().addOnSuccessListener{
                        val usuarios = ArrayList<String>()
                        for(doc in it.documents){
                            usuarios.add(doc.id)
                           val nombre = doc.get("NombreCompleto") as String?
                            if(!nombre.isNullOrEmpty()){
                                if(!nombre.contains(newText)){
                                    usuarios.remove(doc.id)
                                    usuarioAdapter!!.setIntegrantes(usuarios!!)
                                    lista.setHasFixedSize(true)
                                    lista.adapter = concatAdapter
                                }
                            }
                        }
                    }
                    db.collection("Asignatura").get().addOnSuccessListener{
                        val asignaturas = ArrayList<String>()
                        for(doc in it.documents){
                            asignaturas.add(doc.id)
                            val nombre = doc.get("Nombre") as String?
                            if(!nombre.isNullOrEmpty()){
                                if(!nombre.contains(newText)){
                                    asignaturas.remove(doc.id)
                                    asignaturaAdapter!!.setAsignaturasGuardadas(asignaturas)
                                    lista.setHasFixedSize(true)
                                    lista.adapter = concatAdapter
                                }
                            }
                        }
                    }
                    db.collection("Competencia").get().addOnSuccessListener{
                        val competencias = ArrayList<String>()
                        for(doc in it.documents){
                            competencias.add(doc.id)
                            val codigo = doc.get("Codigo") as String?
                            if(!codigo.isNullOrEmpty()){
                                if(!codigo.contains(newText)){
                                    competencias.remove(doc.id)
                                    comAdapter!!.setSeleccionadas(competencias)
                                    lista.setHasFixedSize(true)
                                    lista.adapter = concatAdapter
                                }
                            }
                        }
                    }
                    db.collection("Grupo").get().addOnSuccessListener {
                        val grupos = ArrayList<String>()
                        for(doc in it.documents){
                            grupos.add(doc.id)
                            val nombre = doc.get("Nombre") as String?
                            if(!nombre.isNullOrEmpty()){
                                if(!nombre.contains(newText)){
                                    grupos.remove(doc.id)
                                    grupoAdapter!!.setGruposGuardados(grupos)
                                    lista.setHasFixedSize(true)
                                    lista.adapter = concatAdapter
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

        lista.recycledViewPool.setMaxRecycledViews(0, 0)
    }

    override fun onStart() {
        super.onStart()
        asignaturaAdapter!!.startListening()
        usuarioAdapter!!.startListening()
        comAdapter!!.startListening()
        grupoAdapter!!.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        asignaturaAdapter!!.stopListening()
        usuarioAdapter!!.stopListening()
        comAdapter!!.stopListening()
        grupoAdapter!!.stopListening()
    }

}