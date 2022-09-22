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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore


class Integrantes_Asignatura_Fragment : Fragment() {

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
        val idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_integrantes__asignatura_, container, false)
        setup(email ?: "", idAsignatura ?: "", inf)
        return inf
    }

    fun setup(email: String, idAsignatura: String, inf : View){
        val query = db.collection("Usuario")
        val listaUsuarios: RecyclerView = inf.findViewById(R.id.integrantesList)
        var options = FirestoreRecyclerOptions.Builder<User>().setQuery(query, User::class.java).build()
        userAdapter = UserAdapter(options)
        userAdapter!!.setEmail(email)
        userAdapter!!.setFragmentLlamador("Integrantes")
        db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
            val integrantes = it.get("Integrantes") as ArrayList<String>?
            userAdapter!!.setIntegrantes(integrantes!!)
            listaUsuarios.layoutManager = LinearLayoutManager(this.context)
            listaUsuarios.setHasFixedSize(true)
            listaUsuarios.adapter = userAdapter
        }

        listaUsuarios.recycledViewPool.setMaxRecycledViews(0, 0)

        val busquedaUsuarios: SearchView = inf.findViewById(R.id.searchView)
        busquedaUsuarios.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                val originalListOptions = FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(query,User::class.java).build()
                val filteredListQuery = query.whereGreaterThanOrEqualTo("NombreCompleto", newText)
                        .whereLessThanOrEqualTo("NombreCompleto",newText+"\uf8ff")
                val filteredListOptions = FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(filteredListQuery, User::class.java).build()
                if(newText == ""){
                    userAdapter!!.updateOptions(originalListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
                        listaUsuarios.setHasFixedSize(true)
                        listaUsuarios.adapter = userAdapter
                    }
                } else{
                    userAdapter!!.updateOptions(filteredListOptions)
                    db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener{
                        val integrantes = it.get("Integrantes") as ArrayList<String>?
                        if(!integrantes.isNullOrEmpty()) {
                            for(integrante in integrantes){
                                db.collection("Usuario").document(integrante).get().addOnSuccessListener {it2: DocumentSnapshot ->
                                    val nombre = it2.get("NombreCompleto") as String?
                                    if(!nombre.isNullOrEmpty()){
                                        if(!nombre.contains(newText)){
                                            integrantes.remove(integrante)
                                            userAdapter!!.setIntegrantes(integrantes!!)
                                            listaUsuarios.setHasFixedSize(true)
                                            listaUsuarios.adapter = userAdapter
                                        }
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