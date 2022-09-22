package com.example.tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_ajustes_docente.*
import kotlinx.android.synthetic.main.fragment_perfil__docente_.*
import kotlinx.android.synthetic.main.fragment_perfil_asignatura.*


class Perfil_Asignatura_Fragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val email = arguments?.getString("email")
        val idAsignatura = arguments?.getString("idAsignatura")
        val inf = inflater.inflate(R.layout.fragment_perfil_asignatura, container, false)
        setup(email ?: "", idAsignatura ?: "", inf)
        return inf
    }

    private fun setup(email: String, idAsignatura: String, inf: View){
        val db = FirebaseFirestore.getInstance()

        //Cargamos los datos de la asignatura:
        val etNombreAsignatura : TextView = inf.findViewById(R.id.NombreAsig)
        db.collection("Asignatura").document(idAsignatura).get().addOnSuccessListener {
            etNombreAsignatura.text = it.get("Nombre") as String?
            val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            (activity as SeccionPrincipalActivity).supportActionBar?.title = etNombreAsignatura.text
            Picasso.with(imgPerfilAsig.context).load(it.get("UrlFoto") as String?).into(imgPerfilAsig)
        }
        val bEditar : Button = inf.findViewById(R.id.bModoEdicion)

        //si es un alumno no podrá editar los datos de la asignatura
        if(email.contains("alumnos"))
            bEditar.visibility = GONE
        else{ bEditar.visibility = VISIBLE}

        val adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
        val viewPager2: ViewPager2 = inf.findViewById(R.id.viewPager)
        val tabLayout: TabLayout = inf.findViewById(R.id.tab_layout)
        adapter.setidAsignatura(idAsignatura)
        adapter.setEmail(email)
        viewPager2.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when(position){
                0->{
                    tab.text = "Grupos"
                }
                1->{
                    tab.text = "Integrantes"
                }
            }
        }.attach()

        bEditar.setOnClickListener{
            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("idAsignatura", idAsignatura)
            val fragEditarAsignatura = EditarAsignatura()
            fragEditarAsignatura.arguments = bundle
            val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
            transaction.replace(this.id, fragEditarAsignatura).addToBackStack(null)
            transaction.commit()
        }
    }
}