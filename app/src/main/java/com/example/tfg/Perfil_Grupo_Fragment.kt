package com.example.tfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_perfil__grupo.*


class Perfil_Grupo_Fragment : Fragment() {

    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val email = arguments?.getString("email")
        val idGrupo = arguments?.getString("idGrupo")
        var idAsignatura = arguments?.getString("idAsignatura")!!
        val inf = inflater.inflate(R.layout.fragment_perfil__grupo, container, false)
        setup(email ?: "", idGrupo ?: "", idAsignatura?:"", inf)
        return inf
    }

    private fun setup(email: String, idGrupo: String, idAsignatura: String, inf: View){

        //Cargamos los datos del grupo:
        val etNombreGrupo : TextView = inf.findViewById(R.id.NombreGrupo)
        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener {
            etNombreGrupo.text = it.get("Nombre") as String?
            val toolbar = inf.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            (activity as SeccionPrincipalActivity).supportActionBar?.title = etNombreGrupo.text
            Picasso.with(imgPerfilGrupo.context).load(it.get("UrlFoto") as String?).into(imgPerfilGrupo)
        }

        val adapter = ViewPagerAdapter(childFragmentManager, lifecycle)
        val viewPager2: ViewPager2 = inf.findViewById(R.id.viewPagerGrupos)
        val tabLayout: TabLayout = inf.findViewById(R.id.tab_layoutGrupos)

        adapter.setidGrupo(idGrupo)
        adapter.setEmail(email)
        adapter.setfragmentoLlamador("PerfilGrupo")
        adapter.setidAsignatura(idAsignatura)
        viewPager2.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when(position){
                0->{
                    tab.text = "Competencias"
                }
                1->{
                    tab.text = "Integrantes"
                }
            }
        }.attach()

        val bEditar : Button = inf.findViewById(R.id.bModoEdicionG)

        //si es un alumno no podrá editar los datos del grupo
        if(email.contains("alumnos"))
            bEditar.visibility = View.GONE

        bEditar.setOnClickListener{
            val bundle = Bundle()
            bundle.putString("email", email)
            bundle.putString("idGrupo", idGrupo)
            bundle.putString("idAsignatura", idAsignatura)
            val fragEditarGrupo = EditarGrupo()
            fragEditarGrupo.arguments = bundle
            val transaction : FragmentTransaction =  parentFragmentManager.beginTransaction()
            transaction.replace(this.id, fragEditarGrupo).addToBackStack(null)
            transaction.commit()
        }
    }

}