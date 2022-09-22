package com.example.tfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(options: FirestoreRecyclerOptions<User>)  :
        FirestoreRecyclerAdapter<User,  UserAdapter.UserAdapterVH>(options){

    private val usuariosGuardados : ArrayList<String> = ArrayList<String>()
    private var integrantes : ArrayList<String> = ArrayList<String>()
    private var emailCurrentUser: String = ""
    private var nombreFragment: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAdapterVH {
        return UserAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false))
    }

    fun getUsuariosSeleccionados(): ArrayList<String> {
        return usuariosGuardados
    }

    fun setUsuariosSeleccionados(usuarios: ArrayList<String>){
        for(item in usuarios){
            if(item != emailCurrentUser) usuariosGuardados.add(item)
        }
        notifyDataSetChanged()
    }

    fun setEmail(email : String) {
        emailCurrentUser = email
        usuariosGuardados.add(email)
    }

    fun setFragmentLlamador(nomFragment : String){
        nombreFragment = nomFragment
    }

    fun setIntegrantes(integrantesAsig: ArrayList<String>){
        for(item in integrantesAsig){
            integrantes.add(item)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: UserAdapterVH, position: Int, model: User) {
        if(nombreFragment == "Integrantes") {
            holder.Checkbox.visibility = GONE
            if(integrantes.contains(snapshots.getSnapshot(holder.adapterPosition).id) && snapshots.getSnapshot(holder.adapterPosition).id != emailCurrentUser){
                Log.i("UserAdapter", "Se ha mostrado el integrante  ${snapshots.getSnapshot(holder.adapterPosition).id}")
                holder.itemView.visibility = View.VISIBLE
                holder.NombreCompleto.text = model.NombreCompleto
                holder.Rol.text = model.Rol
                Picasso.with(holder.UrlFotoPerfil.context).load(model.UrlFotoPerfil).into(holder.UrlFotoPerfil)
            }else{
                Log.i("UserAdapter", "Se ha skippeado un integrante  ${snapshots.getSnapshot(holder.adapterPosition).id}")
                holder.itemView.visibility = View.GONE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            }
            holder.itemView.setOnClickListener {
                val bundle = Bundle()
                val fragPerfil: Fragment
                if(snapshots.getSnapshot(holder.adapterPosition).id.contains("alumnos")){
                    bundle.putString("emailEstudiante", snapshots.getSnapshot(holder.adapterPosition).id)
                    bundle.putString("emailVisitante", emailCurrentUser)
                    fragPerfil = Perfil_Estudiante_Fragment()
                    fragPerfil.arguments = bundle
                }else{
                    bundle.putString("emailDocente", snapshots.getSnapshot(holder.adapterPosition).id)
                    bundle.putString("emailVisitante", emailCurrentUser)
                    fragPerfil = Perfil_Docente_Fragment()
                    fragPerfil.arguments = bundle
                }

                val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfil).addToBackStack(null).commit()
            }
        } else{
            if(nombreFragment == "NuevoGrupo"){
                    if(integrantes.contains(snapshots.getSnapshot(holder.adapterPosition).id) && snapshots.getSnapshot(holder.adapterPosition).id != emailCurrentUser){
                        holder.itemView.visibility = View.VISIBLE
                        holder.NombreCompleto.text = model.NombreCompleto
                        holder.Rol.text = model.Rol
                        Picasso.with(holder.UrlFotoPerfil.context).load(model.UrlFotoPerfil).into(holder.UrlFotoPerfil)
                        holder.Checkbox.isChecked = usuariosGuardados.contains(snapshots.getSnapshot(holder.adapterPosition).id)
                        holder.Checkbox.setOnClickListener{
                            if (holder.Checkbox.isChecked)
                                usuariosGuardados.add(snapshots.getSnapshot(holder.adapterPosition).id)
                            else
                                usuariosGuardados.remove(snapshots.getSnapshot(holder.adapterPosition).id)
                        }
                    }else{
                        Log.i("UserAdapter", "Se ha skippeado un integrante  ${snapshots.getSnapshot(holder.adapterPosition).id}")
                        holder.itemView.visibility = View.GONE
                        holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
                    }
            } else{
                if (snapshots.getSnapshot(holder.adapterPosition).id == emailCurrentUser){
                    holder.itemView.visibility = View.GONE
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
                }
                else{
                    holder.itemView.visibility = View.VISIBLE
                    holder.NombreCompleto.text = model.NombreCompleto
                    holder.Rol.text = model.Rol
                    Picasso.with(holder.UrlFotoPerfil.context).load(model.UrlFotoPerfil).into(holder.UrlFotoPerfil)
                    holder.Checkbox.isChecked = usuariosGuardados.contains(snapshots.getSnapshot(holder.adapterPosition).id)
                    holder.Checkbox.setOnClickListener{
                        if (holder.Checkbox.isChecked)
                            usuariosGuardados.add(snapshots.getSnapshot(holder.adapterPosition).id)
                        else{ usuariosGuardados.remove(snapshots.getSnapshot(holder.adapterPosition).id) }
                    }
                }
            }
        }
    }

    class UserAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val NombreCompleto : TextView = itemView.findViewById(R.id.nombreUsuario)
        val Rol : TextView = itemView.findViewById(R.id.rolUsuario)
        val UrlFotoPerfil : CircleImageView = itemView.findViewById(R.id.imgPerfilUsuario)
        val Checkbox : CheckBox = itemView.findViewById(R.id.cbSeleccionarUsuario)
    }

}