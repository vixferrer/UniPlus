package com.example.tfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GrupoAdapter(options: FirestoreRecyclerOptions<Grupo>) :
        FirestoreRecyclerAdapter<Grupo, GrupoAdapter.GrupoAdapterVH>(options) {

    private var emailCurrentUser: String = ""
    private var idAsignatura: String = ""
    private var gruposGuardados = ArrayList<String>()
    private val TAG = "GrupoAdapter"
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupoAdapterVH {
        return GrupoAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.grupo_item, parent, false))
    }

    fun setEmail(email : String) {
        emailCurrentUser = email
    }

    fun setGruposGuardados(grupos : ArrayList<String>){
        for(item in grupos)
            gruposGuardados.add(item)
        notifyDataSetChanged()
    }

    fun setidAsignatura(id : String){
        idAsignatura = id
    }

    override fun onBindViewHolder(holder: GrupoAdapterVH, position: Int, model: Grupo) {
        if(gruposGuardados.contains(snapshots.getSnapshot(holder.adapterPosition).id)){
            holder.itemView.visibility = View.VISIBLE
            holder.Nombre.text = model.Nombre
            Picasso.with(holder.UrlFoto.context).load(model.UrlFoto).into(holder.UrlFoto)
        }
        else{
            Log.i("GrupoAdapter", "Se ha ocultado un grupo ${model.Nombre}")
            hideGrupo(holder.itemView)
        }
        holder.itemView.setOnClickListener{
            db.collection("Usuario").document(emailCurrentUser).get().addOnSuccessListener {
                val grupos = it.get("Grupos") as ArrayList<String>?
                val nombre = it.get("NombreCompleto") as String?
                if(grupos.isNullOrEmpty() || !grupos.contains(snapshots.getSnapshot(holder.adapterPosition).id)){
                    val builder = AlertDialog.Builder(holder.itemView.context)
                    builder.setMessage("¿Desea solicitar unirse a este grupo?")
                    builder.setCancelable(false)
                            .setPositiveButton("Sí"){ dialog, id ->
                                val title = "¡Alguien ha solicitado unirse a su grupo!"
                                val message = nombre + " quiere unirse a " + model.Nombre
                                if(title!=null && message != null){
                                    db.collection("Grupo").document(snapshots.getSnapshot(holder.adapterPosition).id).get().addOnSuccessListener {it2: DocumentSnapshot ->
                                        val integrantes = it2.get("Integrantes") as ArrayList<String>?
                                        if(!integrantes.isNullOrEmpty()){
                                            for(integrante in integrantes){
                                                if(!integrante.contains("alumnos")){
                                                    val topic = "/topics/"+integrante.substringBefore('@')
                                                    PushNotification(NotificationData(title, message), topic).also { itPN-> sendNotification(itPN) }
                                                    val documento = db.collection("Usuario").document(integrante).collection("Notificaciones").document()
                                                    documento.set(
                                                            hashMapOf("CodDocumento" to snapshots.getSnapshot(holder.adapterPosition).id,
                                                                    "Tipo" to "G",
                                                                    "EmailUsuario" to emailCurrentUser,
                                                                    "Created" to  FieldValue.serverTimestamp()
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }.setNegativeButton("No") { dialog, id ->
                                dialog.dismiss()
                            }
                    val alert = builder.create()
                    alert.show()
                }else{
                    db.collection("Grupo").document(snapshots.getSnapshot(holder.adapterPosition).id).get().addOnSuccessListener {it2 ->
                        val bundle = Bundle()
                        bundle.putString("email", emailCurrentUser)
                        bundle.putString("idGrupo", snapshots.getSnapshot(holder.adapterPosition).id)
                        val asignatura = it2.get("Asignatura") as String?
                        bundle.putString("idAsignatura", asignatura!!)
                        val fragPerfilGrupo = Perfil_Grupo_Fragment()
                        fragPerfilGrupo.arguments = bundle

                        val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilGrupo).addToBackStack(null).commit()
                    }
                }
            }

        }
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d(TAG, "Response: ${Gson().toJson(response)}")
            } else {
                Log.e(TAG, response.errorBody().toString())
            }
        } catch(e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun hideGrupo(itemView: View){
        itemView.visibility = View.GONE
        itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
    }

    class GrupoAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView){
        val Nombre : TextView = itemView.findViewById(R.id.nombreGrupo)
        val UrlFoto : CircleImageView = itemView.findViewById(R.id.imgPerfilGrupo)
    }
}