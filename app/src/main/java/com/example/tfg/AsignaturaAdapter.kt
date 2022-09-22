package com.example.tfg

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import java.time.LocalDateTime

class AsignaturaAdapter(options: FirestoreRecyclerOptions<Asignatura>) :
        FirestoreRecyclerAdapter<Asignatura, AsignaturaAdapter.AsignaturaAdapterVH>(options) {

    private var emailCurrentUser: String = ""
    private var fragmentLlamador: String = ""
    private var itemCount = Int
    private var esBusqueda = false
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "AsignaturaAdapter"
    private var asignaturasGuardadas = ArrayList<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AsignaturaAdapterVH {
        return when (fragmentLlamador) {
            "Perfil" -> AsignaturaAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.asignatura_peque_item, parent, false))
            "Perfil-Competencia"-> AsignaturaAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.asignatura_peque_item, parent, false))
            else -> {
                AsignaturaAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.asignatura_item, parent, false))
            }
        }
    }

    fun setEmail(email : String) {
        emailCurrentUser = email
    }

    fun setAsignaturasGuardadas(grupos : ArrayList<String>){
        for(item in grupos)
            asignaturasGuardadas.add(item)
        notifyDataSetChanged()
    }

    fun setFragmentLlamador(frag: String) {
        fragmentLlamador = frag
    }

    fun setEsBusqueda() {
        esBusqueda = true
    }

    override fun onBindViewHolder(holder: AsignaturaAdapterVH, position: Int, model: Asignatura) {
        if(!asignaturasGuardadas.contains(snapshots.getSnapshot(holder.adapterPosition).id))
            hideAsignatura(holder.itemView)
        else{
            holder.itemView.visibility = View.VISIBLE
            holder.Nombre.text = model.Nombre
            Picasso.with(holder.UrlFoto.context).load(model.UrlFoto).into(holder.UrlFoto)
        }
        holder.itemView.setOnClickListener{
            db.collection("Usuario").document(emailCurrentUser).get().addOnSuccessListener {
                    val asignaturas = it.get("Asignaturas") as ArrayList<String>?
                    val nombre = it.get("NombreCompleto") as String?
                    if(asignaturas.isNullOrEmpty() || !asignaturas.contains(snapshots.getSnapshot(holder.adapterPosition).id)){
                        val builder = AlertDialog.Builder(holder.itemView.context)
                        builder.setMessage("¿Desea solicitar unirse a esta asignatura?")
                        builder.setCancelable(false)
                                .setPositiveButton("Sí"){ dialog, id ->
                                    val title = "¡Alguien ha solicitado unirse a su asignatura!"
                                    val message = nombre + " quiere unirse a " + model.Nombre
                                    if(title!=null && message != null){
                                        db.collection("Asignatura").document(snapshots.getSnapshot(holder.adapterPosition).id).get().addOnSuccessListener {it2: DocumentSnapshot ->
                                            val integrantes = it2.get("Integrantes") as ArrayList<String>?
                                            if(!integrantes.isNullOrEmpty()){
                                                for(integrante in integrantes){
                                                    if(!integrante.contains("alumnos")){
                                                        val topic = "/topics/"+integrante.substringBefore('@')
                                                        PushNotification(NotificationData(title, message), topic).also { itPN-> sendNotification(itPN) }
                                                        val documento = db.collection("Usuario").document(integrante).collection("Notificaciones").document()
                                                        documento.set(
                                                                hashMapOf("CodDocumento" to snapshots.getSnapshot(holder.adapterPosition).id,
                                                                        "Tipo" to "A",
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
                        val bundle = Bundle()
                        bundle.putString("email", emailCurrentUser)
                        bundle.putString("idAsignatura", snapshots.getSnapshot(holder.adapterPosition).id)
                        val fragPerfilAsignatura = Perfil_Asignatura_Fragment()
                        fragPerfilAsignatura.arguments = bundle

                        val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                        activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilAsignatura).addToBackStack(null).commit()
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

    private fun hideAsignatura(itemView: View){
        itemView.visibility = View.GONE
        itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
    }

    class AsignaturaAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView){
        val Nombre : TextView = itemView.findViewById(R.id.nombreAsignatura)
        val UrlFoto : CircleImageView = itemView.findViewById(R.id.imgPerfilAsignatura)
    }
}