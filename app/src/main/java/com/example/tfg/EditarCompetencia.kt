package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_editar_grupo.*
import kotlinx.android.synthetic.main.fragment_nueva_competencia.*

class EditarCompetencia : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    var asignaturaAdapter: AsignaturaAdapterVP ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val idCompetencia = arguments?.getString("idCompetencia")
        val email = arguments?.getString("email")
        val inf = inflater.inflate(R.layout.fragment_editar_competencia, container, false)
        val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        (activity as SeccionPrincipalActivity).supportActionBar?.title = "Editar Competencia"
        setup(idCompetencia ?: "", email ?: "", inf)
        return inf
    }

    private fun setup(idCompetencia: String, email: String, inf: View) {
        val bGuardar : FloatingActionButton = inf.findViewById(R.id.bGuardarCompetencia)
        val bEliminar : Button = inf.findViewById(R.id.bEliminarCompetencia)
        val query = db.collection("Asignatura")
        val options = FirestoreRecyclerOptions.Builder<Asignatura>().setQuery(query, Asignatura::class.java).build()
        val listaAsignaturas: RecyclerView = inf.findViewById(R.id.lista_asignaturas_seleccionar)
        val codCompetencia : EditText = inf.findViewById(R.id.editTextCodigo)
        val tituloCompetencia : EditText = inf.findViewById(R.id.editTextTitulo)
        val desCompetencia : EditText = inf.findViewById(R.id.editTextDescripcion)
        var asignaturasFinales = ArrayList<String>()

        asignaturaAdapter = AsignaturaAdapterVP(options)
        asignaturaAdapter!!.setEmail(email)

        //cargamos los datos iniciales (codigo, titulo, descripcion...)
        db.collection("Competencia").document(idCompetencia).get().addOnSuccessListener{
            codCompetencia.setText(it.get("Codigo") as String?)
            tituloCompetencia.setText(it.get("Titulo") as String?)
            desCompetencia.setText(it.get("Descripcion") as String?)
            db.collection("Asignatura").whereArrayContains("Competencias", idCompetencia).get().addOnSuccessListener {it2: QuerySnapshot ->
                val asignaturas = ArrayList<String>()
                for (doc in it2.documents)
                    asignaturas.add(doc.id)
                asignaturaAdapter!!.setAsignaturasSeleccionadas(asignaturas)
                listaAsignaturas.layoutManager = LinearLayoutManager(this.context)
                listaAsignaturas.setHasFixedSize(true)
                listaAsignaturas.adapter = asignaturaAdapter
                db.collection("Usuario").document(email).get().addOnSuccessListener { it3: DocumentSnapshot ->
                    val asignaturasEsteDocente = it3.get("Asignaturas") as ArrayList<String>?
                    if (!asignaturasEsteDocente.isNullOrEmpty()){
                        val asignaturasTotales = ArrayList<String>()
                        asignaturasTotales.addAll(asignaturas)
                        asignaturasTotales.addAll(asignaturasEsteDocente)
                        asignaturaAdapter!!.setAsignaturasAMostrar(asignaturasTotales)
                    }
                }
            }
        }

        listaAsignaturas.recycledViewPool.setMaxRecycledViews(0, 0)

        val codigo : TextView = inf.findViewById(R.id.tvCodigo)
        codigo.bringToFront()

        bGuardar.setOnClickListener {
            asignaturasFinales = asignaturaAdapter!!.getAsignaturasGuardadas()
            if(codCompetencia.text.toString().isEmpty() || desCompetencia.text.toString().isEmpty() || asignaturasFinales.isEmpty())
                showAlert("Por favor, asegúrese de introducir el código de la competencia, una descripción, y al menos una asignatura donde evaluarla")
            else{
                db.collection("Asignatura").whereArrayContains("Competencias", idCompetencia).get().addOnSuccessListener {
                    val asignaturasIniciales = ArrayList<String>()
                    for(doc in it.documents)
                        asignaturasIniciales.add(doc.id)
                    if(!asignaturasIniciales.isNullOrEmpty()){
                        val asignaturasFuera = asignaturasIniciales.minus(asignaturasFinales)
                        for(asignatura in asignaturasFuera){
                            db.collection("Asignatura").document(asignatura).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                val competencias = it2.get("Competencias") as ArrayList<String>?
                                if(!competencias.isNullOrEmpty()){
                                    competencias.remove(idCompetencia)
                                    db.collection("Asignatura").document(asignatura).update("Competencias", competencias)
                                }
                            }
                        }
                        val asignaturasNuevas = asignaturasFinales.minus(asignaturasIniciales)
                        for(asignatura in asignaturasNuevas){
                            db.collection("Asignatura").document(asignatura).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                val competencias = it2.get("Competencias") as ArrayList<String>?
                                if(!competencias.isNullOrEmpty()){
                                    competencias.add(idCompetencia)
                                    db.collection("Asignatura").document(asignatura).update("Competencias", competencias)
                                }
                            }
                        }
                    }
                }
                db.collection("Competencia").document(idCompetencia).update(
                        "Codigo", codCompetencia.text.toString(),
                        "Titulo", tituloCompetencia.text.toString(),
                        "Descripcion", desCompetencia.text.toString()
                )

                //Volvemos a la página anterior:
                val bundle = Bundle()
                val fragPerfilCompetencia = Perfil_Competencia_Fragment()
                val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
                bundle.putString("email", email)
                bundle.putString("idCompetencia", idCompetencia)
                fragPerfilCompetencia.arguments = bundle
                transaction.replace(this.id, fragPerfilCompetencia).addToBackStack("NUEVO")
                transaction.commit()
            }
        }

        bEliminar.setOnClickListener{
            val builder = AlertDialog.Builder(this.requireContext())
            builder.setMessage("¿Desea eliminar esta competencia del sistema? Se borrarán todas sus puntuaciones")
                .setCancelable(false)
                .setPositiveButton("Sí, deseo borrarla") { dialog, id ->
                    db.collection("Asignatura").whereArrayContains("Competencias", idCompetencia).get().addOnSuccessListener {
                        for(doc in it.documents){
                            val competencias = doc.get("Competencias") as java.util.ArrayList<String>?
                            if(!competencias.isNullOrEmpty())
                                competencias.remove(idCompetencia)
                            db.collection("Asignatura").document(doc.id).update("Competencias", competencias)
                        }
                    }
                    //Eliminamos las notificaciones de usuarios de esta competencia
                    db.collection("Usuario").get().addOnSuccessListener {
                        for(doc in it.documents){
                            db.collection("Usuario").document(doc.id).collection("Notificaciones").whereEqualTo("CodCompetencia", codCompetencia.text.toString()).get().addOnSuccessListener {it2: QuerySnapshot ->
                                for(subDoc in it2.documents)
                                    db.collection("Usuario").document(doc.id).collection("Notificaciones").document(subDoc.id).delete()
                            }
                            db.collection("Usuario").document(doc.id).collection("Puntuaciones").get().addOnSuccessListener {
                                db.collection("Usuario").document(doc.id).collection("Puntuaciones").document(idCompetencia).get().addOnSuccessListener {
                                    db.collection("Usuario").document(doc.id).collection("Puntuaciones").document(idCompetencia).delete()
                                }
                            }
                        }
                    }

                    //Eliminamos tambien las puntaciones de este grupo:
                    db.collection("Grupo").get().addOnSuccessListener {
                        for(doc in it.documents){
                            db.collection("Grupo").document(doc.id).collection("Puntuaciones").get().addOnSuccessListener {
                                db.collection("Grupo").document(doc.id).collection("Puntuaciones").document(idCompetencia).get().addOnSuccessListener {
                                    db.collection("Grupo").document(doc.id).collection("Puntuaciones").document(idCompetencia).delete()
                                }
                            }
                        }
                    }

                    //Finalmente eliminamos el documento de esta competencia:
                    db.collection("Competencia").document(idCompetencia).delete()

                    val bundle = Bundle()
                    bundle.putString("email", email)
                    val frag : Fragment
                    if(email.contains("alumnos")){
                        frag = Home_Docente_Fragment()
                    }else{
                        frag = competencias_Docente_Fragment()
                    }
                    frag.arguments = bundle
                    val activity : AppCompatActivity = this.context as AppCompatActivity
                    activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, frag).addToBackStack("ELIMINACION").commit()
                }.setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun showAlert(mensaje: String) {
        val builder = AlertDialog.Builder(this.requireContext())
        builder.setMessage(mensaje)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
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