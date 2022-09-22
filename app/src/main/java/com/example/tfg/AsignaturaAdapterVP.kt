package com.example.tfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class AsignaturaAdapterVP(options: FirestoreRecyclerOptions<Asignatura>) :
        FirestoreRecyclerAdapter<Asignatura, AsignaturaAdapterVP.AsignaturaAdapterVH>(options) {

    private var emailCurrentUser: String = ""
    private var asignaturasAMostrar = ArrayList<String>()
    private var asignaturasGuardadas : ArrayList<String> = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsignaturaAdapterVH {
        return AsignaturaAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.asignatura_nombre_checkbox_item, parent, false))
    }

    fun setEmail(email : String) {
        emailCurrentUser = email
    }

    fun setAsignaturasSeleccionadas(asignaturas: ArrayList<String>){
        for(item in asignaturas){
            asignaturasGuardadas.add(item)
        }
        notifyDataSetChanged()
    }

    fun setAsignaturasAMostrar(asignaturas : ArrayList<String>){
        for(item in asignaturas)
            asignaturasAMostrar.add(item)
        notifyDataSetChanged()
    }

    fun getAsignaturasGuardadas(): ArrayList<String>{
        return asignaturasGuardadas
    }

    override fun onBindViewHolder(holder: AsignaturaAdapterVH, position: Int, model: Asignatura) {
        if(!asignaturasAMostrar.contains(snapshots.getSnapshot(holder.adapterPosition).id))
            hideAsignatura(holder.itemView)
        else{
            holder.itemView.visibility = VISIBLE
            holder.Nombre.text = model.Nombre
            holder.Checkbox.isChecked = asignaturasGuardadas.contains(snapshots.getSnapshot(holder.adapterPosition).id)
            holder.Checkbox.setOnClickListener{
                if (holder.Checkbox.isChecked)
                    asignaturasGuardadas.add(snapshots.getSnapshot(holder.adapterPosition).id)
                else{ asignaturasGuardadas.remove(snapshots.getSnapshot(holder.adapterPosition).id) }
            }
        }
    }

    private fun hideAsignatura(itemView: View){
        itemView.visibility = View.GONE
        itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
    }

    class AsignaturaAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView){
        val Nombre : TextView = itemView.findViewById(R.id.nombreAsignaturaVP)
        val Checkbox : CheckBox = itemView.findViewById(R.id.cbSeleccionarAsignatura)
    }
}