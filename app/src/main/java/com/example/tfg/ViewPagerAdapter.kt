package com.example.tfg


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ViewPagerAdapter (fm: FragmentManager, lf : Lifecycle) : FragmentStateAdapter(fm, lf){

    var emailUsuario : String = ""
    var idAsignat : String = ""
    var idGrupo : String = ""
    var fragmentoLlamador : String = ""

    override fun getItemCount(): Int {
        return 2
    }

    public fun setEmail(correo : String){
        emailUsuario = correo
    }

    fun setidGrupo(id : String){
        idGrupo = id
    }

    fun setfragmentoLlamador (frag : String){
        fragmentoLlamador = frag
    }

    fun setidAsignatura(idAsig : String){
        idAsignat = idAsig
    }

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        if(fragmentoLlamador=="PerfilGrupo"){
            bundle.putString("email", emailUsuario)
            bundle.putString("idGrupo", idGrupo)
            bundle.putString("idAsignatura", idAsignat)
            val fragCompetencias = Competencias_Grupo_Fragment()
            val fragIntegrantes = Integrantes_Grupo_Fragment()
            fragCompetencias.arguments = bundle
            fragIntegrantes.arguments = bundle

            if(position == 0) return fragCompetencias
            else if (position == 1) return fragIntegrantes

        }else{
            bundle.putString("email", emailUsuario)
            bundle.putString("idAsignatura", idAsignat)
            val fragGrupos = Grupos_Asignatura_Fragment()
            val fragIntegrantes = Integrantes_Asignatura_Fragment()
            fragGrupos.arguments = bundle
            fragIntegrantes.arguments = bundle

            if(position == 0) return fragGrupos
            else if (position == 1) return fragIntegrantes
        }
        return Fragment()
    }
}