package com.example.tfg

import android.os.Bundle
import android.text.TextUtils.lastIndexOf
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_nueva_competencia.*


class Perfil_Competencia_Fragment : Fragment() {
    val db = FirebaseFirestore.getInstance()
    private var asignaturaAdapter: AsignaturaAdapter ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val idCompetencia = arguments?.getString("idCompetencia")
        val inf = inflater.inflate(R.layout.fragment_perfil_competencia, container, false)
        setup(email ?: "", idCompetencia ?: "", inf)
        return inf
    }

    private fun setup(email: String, idCompetencia: String, inf: View) {
        val bEditar: FloatingActionButton = inf.findViewById(R.id.bEditarComp)

        //Cargamos los datos de la competencia:
        val tvCodigoComp : TextView = inf.findViewById(R.id.etCodigo)
        val tvTituloComp : TextView = inf.findViewById(R.id.etTitulo)
        val tvDescripcionComp : TextView = inf.findViewById(R.id.etDescripcion)
        val tvDes : TextView = inf.findViewById(R.id.textView19)
        val listaAsignaturas: RecyclerView = inf.findViewById(R.id.lista_asignaturas)
        val tvAsignaturas : TextView = inf.findViewById(R.id.textView21)
        val historialDocentes: TextView = inf.findViewById(R.id.historialDocentes)
        val historialEstudiantes : TextView = inf.findViewById(R.id.historialEstudiantes)
        val iconoHistDoc : ImageView = inf.findViewById(R.id.imageViewDocentes)
        val iconoHistEst: ImageView = inf.findViewById(R.id.imageViewEstudiantes)
        val tvEstudiantes : TextView = inf.findViewById(R.id.estudiantes)
        val tvDocentes : TextView = inf.findViewById(R.id.docentes)

        db.collection("Competencia").document(idCompetencia).get().addOnSuccessListener {
            tvCodigoComp.text = it.get("Codigo") as String?
            val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            (activity as SeccionPrincipalActivity).supportActionBar?.title = "Competencia "+ tvCodigoComp.text
            tvTituloComp.text = it.get("Titulo") as String?
            tvDescripcionComp.text = it.get("Descripcion") as String?
            if(tvTituloComp.text.isNullOrEmpty()){
                val tvTitulo : TextView = inf.findViewById(R.id.textView18)
                tvTituloComp.visibility = GONE
                tvTitulo.visibility = GONE
                //Subimos lo que viene por debajo:
                tvDes.y = tvDes.y - 160.toFloat()
                tvDescripcionComp.y = tvDescripcionComp.y - 160.toFloat()
                tvAsignaturas.y = tvAsignaturas.y - 160.toFloat()
                iconoHistDoc.y = iconoHistDoc.y - 160.toFloat()
                tvDocentes.y = tvDocentes.y - 160.toFloat()
                iconoHistEst.y = iconoHistEst.y - 160.toFloat()
                tvEstudiantes.y = tvEstudiantes.y - 160.toFloat()
                listaAsignaturas.y = listaAsignaturas.y - 160.toFloat()
                historialDocentes.y = historialDocentes.y - 160.toFloat()
                historialEstudiantes.y = historialEstudiantes.y - 160.toFloat()
            }
        }

        if(email.contains("alumnos")) {
            bEditar.visibility = GONE
            listaAsignaturas.visibility = GONE
            tvAsignaturas.text = "Puntuaciones recibidas:"

            db.collection("Usuario").document(email).collection("Puntuaciones").document(idCompetencia).get().addOnSuccessListener {
                val historialD = it.get("HistorialDocentes") as ArrayList<String>?
                val historialE = it.get("HistorialEstudiantes") as ArrayList<String>?
                if(historialD!=null){
                    historialDocentes.text = ""
                    for(item in historialD){
                        db.collection("Usuario").document(item).get().addOnSuccessListener { it3: DocumentSnapshot ->
                            val nombre = it3.get("NombreCompleto") as String?
                            if(nombre!=null){
                                if(item == historialD[historialD.lastIndex])
                                    historialDocentes.text = historialDocentes.text.toString() + nombre
                                else{ historialDocentes.text = historialDocentes.text.toString() + "$nombre, " }
                            }
                        }
                    }
                }
                if(historialE!=null){
                    historialEstudiantes.text = ""
                    for(item in historialE){
                        db.collection("Usuario").document(item).get().addOnSuccessListener { it2: DocumentSnapshot ->
                            val nombre = it2.get("NombreCompleto") as String?
                            if(nombre!=null){
                                if(item == historialE[historialE.lastIndex])
                                    historialEstudiantes.text = historialEstudiantes.text.toString() + nombre
                                else{ historialEstudiantes.text = historialEstudiantes.text.toString() + "$nombre, " }
                            }
                        }
                    }
                }
            }

        }else{
            historialDocentes.visibility = GONE
            historialEstudiantes.visibility = GONE
            iconoHistDoc.visibility = GONE
            iconoHistEst.visibility = GONE
            tvEstudiantes.visibility = GONE
            tvDocentes.visibility = GONE
        }

        //Para el listado de asignaturas donde se encuentra esta competencia:
        val query = db.collection("Asignatura").whereArrayContains("Competencias", idCompetencia)
        val options = FirestoreRecyclerOptions.Builder<Asignatura>().setQuery(query, Asignatura::class.java).build()

        asignaturaAdapter = AsignaturaAdapter(options)
        asignaturaAdapter!!.setEmail(email)
        asignaturaAdapter!!.setFragmentLlamador("Perfil-Competencia")

        db.collection("Asignatura").whereArrayContains("Competencias", idCompetencia).get().addOnSuccessListener{
            val asignaturas = ArrayList<String>()
            for (doc in it.documents)
                asignaturas.add(doc.id)
            asignaturaAdapter!!.setAsignaturasGuardadas(asignaturas)
            listaAsignaturas.layoutManager = LinearLayoutManager(this.context)
            listaAsignaturas.setHasFixedSize(true)
            listaAsignaturas.adapter = asignaturaAdapter
        }
        listaAsignaturas.recycledViewPool.setMaxRecycledViews(0, 0)

        bEditar.setOnClickListener{
            val bundle = Bundle()
            bundle.putString("idCompetencia", idCompetencia)
            bundle.putString("email", email)
            val fragEditarComp = EditarCompetencia()
            fragEditarComp.arguments = bundle
            val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
            transaction.replace(this.id, fragEditarComp).addToBackStack(null)
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