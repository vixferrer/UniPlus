package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_nueva_asignatura.*
import kotlinx.android.synthetic.main.fragment_nueva_competencia.*
import kotlinx.android.synthetic.main.fragment_perfil__docente_.*

class NuevaCompetencia : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var asignaturaAdapter: AsignaturaAdapterVP ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val inf = inflater.inflate(R.layout.fragment_nueva_competencia, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Nueva Competencia"
        setup(email ?: "", inf)
        return inf
    }

    private fun setup(email: String, inf : View){
        val bGuardar : FloatingActionButton = inf.findViewById(R.id.bGuardarCompetencia)
        val query = db.collection("Asignatura")
        val options = FirestoreRecyclerOptions.Builder<Asignatura>().setQuery(query, Asignatura::class.java).build()
        val listaAsignaturas: RecyclerView = inf.findViewById(R.id.lista_asignaturas_seleccionar)

        asignaturaAdapter = AsignaturaAdapterVP(options)
        asignaturaAdapter!!.setEmail(email)

        db.collection("Usuario").document(email).get().addOnSuccessListener {
            val asignaturas = it.get("Asignaturas") as ArrayList<String>?
            if(asignaturas!=null && asignaturas.isNotEmpty()){
                asignaturaAdapter!!.setAsignaturasAMostrar(asignaturas!!)
                listaAsignaturas.layoutManager = LinearLayoutManager(this.context)
                listaAsignaturas.setHasFixedSize(true)
                listaAsignaturas.adapter = asignaturaAdapter
            }
        }
        listaAsignaturas.recycledViewPool.setMaxRecycledViews(0, 0)

        val asignaturas  : ArrayList<String> = asignaturaAdapter!!.getAsignaturasGuardadas()
        val codigo : TextView = inf.findViewById(R.id.tvCodigo)
        codigo.bringToFront()

        bGuardar.setOnClickListener {
            val idAutomatico : String = db.collection("Competencia").document().id
            val codCompetencia : String = editTextCodigo.text.toString()
            val tituloCompetencia : String = editTextTitulo.text.toString()
            val desCompetencia : String = editTextDescripcion.text.toString()
            if(codCompetencia.isEmpty() || desCompetencia.isEmpty() || asignaturas.isEmpty())
                showAlert("Por favor, asegúrese de introducir el código de la competencia, una descripción, y al menos una asignatura donde evaluarla")
            else{
                for(item in asignaturas){
                    db.collection("Asignatura").document(item).get().addOnSuccessListener {
                        val competenciasGuardadas = it.get("Competencias") as ArrayList<String>?
                        competenciasGuardadas?.add(idAutomatico)
                        db.collection("Asignatura").document(item).update("Competencias", competenciasGuardadas)
                    }
                }
                db.collection("Competencia").document(idAutomatico).set(
                        hashMapOf("Codigo" to codCompetencia,
                                "Titulo" to tituloCompetencia,
                                "Descripcion" to desCompetencia)
                )

                //Volvemos a la página anterior:
                val bundle = Bundle()
                val fragCompetenciasDoc = competencias_Docente_Fragment()
                val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
                bundle.putString("email", email)
                fragCompetenciasDoc.arguments = bundle
                transaction.replace(this.id, fragCompetenciasDoc).addToBackStack("NUEVO")
                transaction.commit()
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

    private fun showAlert(mensaje: String) {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setMessage(mensaje)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

}