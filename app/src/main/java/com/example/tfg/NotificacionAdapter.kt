package com.example.tfg

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class NotificacionAdapter(options: FirestoreRecyclerOptions<Notificacion>) :
        FirestoreRecyclerAdapter<Notificacion, NotificacionAdapter.NotificacionAdapterVH>(options) {

    private val db = FirebaseFirestore.getInstance()
    private var emailCurrentUser: String = ""
    private lateinit var frag: Fragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionAdapterVH {
        return NotificacionAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.notificacion_item, parent, false))
    }

    fun setEmail(email : String) {
        emailCurrentUser = email
    }

    fun setFragment(fragm : Fragment) {
        frag = fragm
    }

    override fun onBindViewHolder(holder: NotificacionAdapterVH, position: Int, model: Notificacion) {
        if(!model.Vista!!){
            holder.UrlFoto.setCircleBackgroundColorResource(R.color.lightpink)
            holder.itemView.setBackgroundColor(Color.parseColor("#fedfe1"))
        }

        var hora = model.Created!!.toDate().toLocaleString().substring(12, 18)
        if(hora.endsWith(':')) hora = hora.substring(0, hora.length-1)
        holder.Created.text = hora
        db.collection("Usuario").document(model.EmailUsuario!!).get().addOnSuccessListener {
            holder.NombrePuntuador.text = it.get("NombreCompleto") as String?
            Picasso.with(holder.UrlFoto.context).load(it.get("UrlFotoPerfil") as String?).into(holder.UrlFoto)
        }

        if(model.Tipo!!.contains("Union")){
            if(model.Tipo == "UnionA"){
                db.collection("Asignatura").document(model.CodDocumento!!).get().addOnSuccessListener {
                    val nombre = it.get("Nombre") as String?
                    if(!nombre.isNullOrEmpty())
                        holder.CodDocumento.text = nombre
                }
                holder.CodDocumento.setOnClickListener {
                    db.collection("Asignatura").document(model.CodDocumento!!).get().addOnSuccessListener {
                        val bundle = Bundle()
                        val fragPerfilAsignatura = Perfil_Asignatura_Fragment()
                        val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                        bundle.putString("email", emailCurrentUser)
                        bundle.putString("idAsignatura", model.CodDocumento!!)
                        fragPerfilAsignatura.arguments = bundle
                        activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilAsignatura).addToBackStack(null).commit()
                    }
                }
                holder.itemView.setOnClickListener {
                    db.collection("Asignatura").document(model.CodDocumento!!).get().addOnSuccessListener {
                        val bundle = Bundle()
                        bundle.putString("email", emailCurrentUser)
                        bundle.putString("idAsignatura", model.CodDocumento!!)
                        val fragPerfilAsignatura = Perfil_Asignatura_Fragment()
                        fragPerfilAsignatura.arguments = bundle
                        val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                        activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilAsignatura).addToBackStack(null).commit()
                    }
                }
            }
            else{ //Tipo = UnionG (union a grupo)
                db.collection("Grupo").document(model.CodDocumento!!).get().addOnSuccessListener {
                    val nombre = it.get("Nombre") as String?
                    if(!nombre.isNullOrEmpty())
                        holder.CodDocumento.text = nombre
                }
                holder.itemView.setOnClickListener {
                    db.collection("Grupo").document(model.CodDocumento!!).get().addOnSuccessListener {
                        val idAsignatura = it.get("Asignatura") as String?
                        if(!idAsignatura.isNullOrEmpty()){
                            val bundle = Bundle()
                            bundle.putString("email", emailCurrentUser)
                            bundle.putString("idGrupo", model.CodDocumento)
                            bundle.putString("idAsignatura", idAsignatura)
                            val fragPerfilGrupo = Perfil_Grupo_Fragment()
                            fragPerfilGrupo.arguments = bundle
                            val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                            activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilGrupo).addToBackStack(null).commit()
                        }
                    }
                }
                holder.CodDocumento.setOnClickListener {
                    db.collection("Grupo").document(model.CodDocumento!!).get().addOnSuccessListener {
                        val idAsignatura = it.get("Asignatura") as String?
                        if(!idAsignatura.isNullOrEmpty()){
                            val bundle = Bundle()
                            bundle.putString("email", emailCurrentUser)
                            bundle.putString("idGrupo", model.CodDocumento)
                            bundle.putString("idAsignatura", idAsignatura)
                            val fragPerfilGrupo = Perfil_Grupo_Fragment()
                            fragPerfilGrupo.arguments = bundle
                            val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                            activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilGrupo).addToBackStack(null).commit()
                        }
                    }
                }
            }

        }else{ //Tipo = Puntuacion
            holder.textoNotif.text = "te ha puntuado la competencia"
            db.collection("Competencia").document(model.CodDocumento!!).get().addOnSuccessListener {
                val codigo = it.get("Codigo") as String?
                if(!codigo.isNullOrEmpty()) {
                    holder.CodDocumento.text = codigo
                }
            }
            holder.CodDocumento.setOnClickListener {
                db.collection("Usuario").document(emailCurrentUser).collection("Notificaciones").document(snapshots.getSnapshot(holder.adapterPosition).id).update("Vista", true)
                val bundle = Bundle()
                bundle.putString("email", emailCurrentUser)
                bundle.putString("idCompetencia", model.CodDocumento!!)
                val fragPerfilCompetencia = Perfil_Competencia_Fragment()
                fragPerfilCompetencia.arguments = bundle
                val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilCompetencia).addToBackStack(null).commit()
            }
            holder.itemView.setOnClickListener {
                db.collection("Usuario").document(emailCurrentUser).collection("Notificaciones").document(snapshots.getSnapshot(holder.adapterPosition).id).update("Vista", true)
                val bundle = Bundle()
                bundle.putString("email", emailCurrentUser)
                bundle.putString("idCompetencia", model.CodDocumento)
                val fragPerfilCompetencia = Perfil_Competencia_Fragment()
                fragPerfilCompetencia.arguments = bundle
                val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilCompetencia).addToBackStack(null).commit()
            }
        }

        holder.UrlFoto.setOnClickListener {
            db.collection("Usuario").document(emailCurrentUser).collection("Notificaciones").document(snapshots.getSnapshot(holder.adapterPosition).id).update("Vista", true)

            val bundle = Bundle()
            var fragPerfil = Fragment()
            if(model.EmailUsuario!!.contains("alumnos")){
                bundle.putString("emailEstudiante", model.EmailUsuario!!)
                bundle.putString("emailVisitante", emailCurrentUser)
                fragPerfil = Perfil_Estudiante_Fragment()
            }else{
                bundle.putString("emailDocente", model.EmailUsuario!!)
                bundle.putString("emailVisitante", emailCurrentUser)
                fragPerfil = Perfil_Docente_Fragment()
            }
            fragPerfil.arguments = bundle
            val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
            activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfil).addToBackStack(null).commit()
        }
        holder.NombrePuntuador.setOnClickListener {
            db.collection("Usuario").document(emailCurrentUser).collection("Notificaciones").document(snapshots.getSnapshot(holder.adapterPosition).id).update("Vista", true)

            val bundle = Bundle()
            var fragPerfil = Fragment()
            if(model.EmailUsuario!!.contains("alumnos")){
                bundle.putString("emailEstudiante", model.EmailUsuario!!)
                bundle.putString("emailVisitante", emailCurrentUser)
                fragPerfil = Perfil_Estudiante_Fragment()
            }else{
                bundle.putString("emailDocente", model.EmailUsuario!!)
                bundle.putString("emailVisitante", emailCurrentUser)
                fragPerfil = Perfil_Docente_Fragment()
            }
            fragPerfil.arguments = bundle
            val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
            activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfil).addToBackStack(null).commit()
        }
    }

    class NotificacionAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView){
        val CodDocumento : Button = itemView.findViewById(R.id.idCompetencia)
        val NombrePuntuador : Button = itemView.findViewById(R.id.puntuador)
        val UrlFoto : CircleImageView = itemView.findViewById(R.id.imgUsuario)
        val textoNotif : TextView = itemView.findViewById(R.id.textoNotifi)
        val Created : TextView = itemView.findViewById(R.id.hora)
    }

}