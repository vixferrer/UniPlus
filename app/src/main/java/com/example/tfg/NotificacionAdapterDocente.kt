package com.example.tfg

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.fragment_perfil__docente_.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Text

class NotificacionAdapterDocente(options: FirestoreRecyclerOptions<NotificacionDocente>) :
        FirestoreRecyclerAdapter<NotificacionDocente, NotificacionAdapterDocente.NotificacionAdapterVH>(options) {

    private val db = FirebaseFirestore.getInstance()
    private var emailCurrentUser: String = ""
    private val TAG = "NotificacionAdapter"
    private var frag = Fragment()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionAdapterVH {
        return NotificacionAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.notificacion_solicitud_item, parent, false))
    }

    fun setEmail(email : String) {
        emailCurrentUser = email
    }

    fun setFragment(fragm : Fragment) {
        frag = fragm
    }

    override fun onBindViewHolder(holder: NotificacionAdapterVH, position: Int, model: NotificacionDocente) {
        var coleccion = ""
        coleccion = if (model.Tipo == "A") { "Asignatura" } else { "Grupo" }

        val ref = db.collection(coleccion).document(model.CodDocumento!!)
        ref.get().addOnCompleteListener {
            if(it.result!!.exists()){
                holder.CodDocumento.text = it.result!!.get("Nombre") as String?
                db.collection("Usuario").document(model.EmailUsuario!!).get().addOnSuccessListener {it2 ->
                    holder.NombreUsuario.text = it2.get("NombreCompleto") as String?
                    Picasso.with(holder.UrlFoto.context).load(it2.get("UrlFotoPerfil") as String?).into(holder.UrlFoto)
                }
            }else{
                db.collection("Usuario").document(emailCurrentUser).collection("Notificaciones").document(snapshots.getSnapshot(holder.adapterPosition).id).delete()
            }
        }

        holder.UrlFoto.setOnClickListener {
                val bundle = Bundle()
                var fragPerfil = Fragment()
                if (model.EmailUsuario!!.contains("alumnos")) {
                    bundle.putString("emailEstudiante", model.EmailUsuario!!)
                    bundle.putString("emailVisitante", emailCurrentUser)
                    fragPerfil = Perfil_Estudiante_Fragment()
                } else {
                    bundle.putString("emailDocente", model.EmailUsuario!!)
                    bundle.putString("emailVisitante", emailCurrentUser)
                    fragPerfil = Perfil_Docente_Fragment()
                }
                fragPerfil.arguments = bundle
                val activity: AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfil).addToBackStack(null).commit()
            }

        holder.NombreUsuario.setOnClickListener {
                val bundle = Bundle()
                var fragPerfil = Fragment()
                if (model.EmailUsuario!!.contains("alumnos")) {
                    bundle.putString("emailEstudiante", model.EmailUsuario!!)
                    bundle.putString("emailVisitante", emailCurrentUser)
                    fragPerfil = Perfil_Estudiante_Fragment()
                } else {
                    bundle.putString("emailDocente", model.EmailUsuario!!)
                    bundle.putString("emailVisitante", emailCurrentUser)
                    fragPerfil = Perfil_Docente_Fragment()
                }
                fragPerfil.arguments = bundle
                val activity: AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfil).addToBackStack(null).commit()
        }

        holder.CodDocumento.setOnClickListener {
            val bundle = Bundle()
            var fragColeccion = Fragment()
            bundle.putString("email", emailCurrentUser)

            if(model.Tipo == "A"){
                bundle.putString("idAsignatura", model.CodDocumento!!)
                fragColeccion = Perfil_Asignatura_Fragment()
                fragColeccion.arguments = bundle
                val activity: AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragColeccion).addToBackStack(null).commit()
            }else{
                db.collection("Grupo").document(model.CodDocumento!!).get().addOnSuccessListener {
                    val idAsignatura = it.get("Asignatura") as String?
                    if(!idAsignatura.isNullOrEmpty())
                        bundle.putString("idAsignatura", idAsignatura)
                    bundle.putString("idGrupo", model.CodDocumento!!)
                    fragColeccion = Perfil_Grupo_Fragment()
                    fragColeccion.arguments = bundle
                    val activity: AppCompatActivity = holder.itemView.context as AppCompatActivity
                    activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                    activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragColeccion).addToBackStack(null).commit()
                }
            }
        }

        holder.itemView.setOnClickListener {
            val bundle = Bundle()
            var fragColeccion = Fragment()
            bundle.putString("email", emailCurrentUser)

            if(model.Tipo == "A"){
                bundle.putString("idAsignatura", model.CodDocumento!!)
                fragColeccion.arguments = bundle
                val activity: AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragColeccion).addToBackStack(null).commit()
            }else{
                db.collection("Grupo").document(model.CodDocumento!!).get().addOnSuccessListener {
                    val idAsignatura = it.get("Asignatura") as String?
                    if(!idAsignatura.isNullOrEmpty())
                        bundle.putString("idAsignatura", idAsignatura)
                    bundle.putString("idGrupo", model.CodDocumento!!)
                    fragColeccion.arguments = bundle
                    val activity: AppCompatActivity = holder.itemView.context as AppCompatActivity
                    activity.findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                    activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragColeccion).addToBackStack(null).commit()
                }
            }
        }

        holder.Aceptar.setOnClickListener {
            val builder = AlertDialog.Builder(holder.itemView.context)
            if (model.Tipo == "A") builder.setMessage("¿Desea aceptar la solicitud de unión a esta asignatura?")
            else { builder.setMessage("¿Desea aceptar la solicitud de unión a este grupo?") }
            builder.setCancelable(false).setPositiveButton("Sí") { dialog, id ->
                if (model.Tipo == "A") {
                    db.collection("Usuario").document(emailCurrentUser).get().addOnSuccessListener {
                        db.collection("Asignatura").document(model.CodDocumento!!).get().addOnSuccessListener { it2: DocumentSnapshot ->
                            val nombreDocente = it.get("NombreCompleto") as String?
                            val nombreAsignatura = it2.get("Nombre") as String?
                                if (!nombreDocente.isNullOrEmpty() && !nombreAsignatura.isNullOrEmpty()) {
                                    val title = "¡Un docente ha aceptado su solicitud de unión!"
                                    val message = nombreDocente + " lo ha añadido a " + nombreAsignatura
                                    val integrantes = it2.get("Integrantes") as ArrayList<String>?
                                    if(!integrantes.isNullOrEmpty()){
                                        integrantes.add(model.EmailUsuario!!)
                                        db.collection("Asignatura").document(model.CodDocumento!!).update("Integrantes", integrantes)
                                        for(integrante in integrantes){
                                            if(!integrante.contains("alumnos")){
                                                val document = db.collection("Usuario").document(integrante).collection("Notificaciones")
                                                        .whereEqualTo("CodDocumento", model.CodDocumento).whereEqualTo("EmailUsuario", model.EmailUsuario)
                                                document.get().addOnSuccessListener { itd ->
                                                    for(doc in itd.documents){
                                                        db.collection("Usuario").document(integrante)
                                                                .collection("Notificaciones").document(doc.id).delete()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    db.collection("Usuario").document(model.EmailUsuario!!).get().addOnSuccessListener { it4: DocumentSnapshot ->
                                            val asignaturas = it4.get("Asignaturas") as ArrayList<String>?
                                            if(!asignaturas.isNullOrEmpty()){
                                                asignaturas.add(model.CodDocumento!!)
                                                db.collection("Usuario").document(model.EmailUsuario!!).update("Asignaturas", asignaturas)
                                            }
                                    }
                                    val topic = "/topics/" + model.EmailUsuario!!.substringBefore('@')
                                    PushNotification(NotificationData(title, message), topic).also { itPN -> sendNotification(itPN) }
                                    val documento = db.collection("Usuario").document(model.EmailUsuario!!).collection("Notificaciones").document()
                                    documento.set(hashMapOf("CodDocumento" to model.CodDocumento,
                                                                        "Tipo" to "UnionA",
                                                                        "EmailUsuario" to emailCurrentUser,
                                                                        "Created" to FieldValue.serverTimestamp(),
                                                                        "Vista" to false)
                                    )
                                }
                            }
                    }
                } else {
                    db.collection("Usuario").document(emailCurrentUser).get().addOnSuccessListener {
                        db.collection("Grupo").document(model.CodDocumento!!).get().addOnSuccessListener { it2: DocumentSnapshot ->
                            val nombreDocente = it.get("NombreCompleto") as String?
                            val nombreGrupo = it2.get("Nombre") as String?
                            val codAsignatura = it2.get("Asignatura") as String?
                            val integrantes = it2.get("Integrantes") as ArrayList<String>?

                            if (!nombreDocente.isNullOrEmpty() && !nombreGrupo.isNullOrEmpty() && !codAsignatura.isNullOrEmpty()) {
                                val title = "¡Un docente ha aceptado su solicitud de unión!"
                                val message = nombreDocente + " lo ha añadido a " + nombreGrupo
                                if(!integrantes.isNullOrEmpty()){
                                    integrantes.add(model.EmailUsuario!!)
                                    db.collection("Grupo").document(model.CodDocumento!!).update("Integrantes", integrantes)
                                    for(integrante in integrantes){
                                        if(!integrante.contains("alumnos")){
                                            val document = db.collection("Usuario").document(integrante).collection("Notificaciones")
                                                    .whereEqualTo("CodDocumento", model.CodDocumento).whereEqualTo("EmailUsuario", model.EmailUsuario)
                                            document.get().addOnSuccessListener { itd ->
                                                for(doc in itd.documents){
                                                    db.collection("Usuario").document(integrante)
                                                            .collection("Notificaciones").document(doc.id).delete()
                                                }
                                            }
                                        }
                                    }
                                }
                                db.collection("Asignatura").document(codAsignatura).get().addOnSuccessListener { it3 ->
                                    val integrantesAsig = it3.get("Integrantes") as ArrayList<String>?
                                    if(!integrantesAsig.isNullOrEmpty()){
                                        if(!integrantesAsig.contains(model.EmailUsuario!!)){
                                            integrantesAsig.add(model.EmailUsuario!!)
                                            db.collection("Asignatura").document(codAsignatura).update("Integrantes", integrantesAsig)
                                        }
                                    }
                                }
                                db.collection("Usuario").document(model.EmailUsuario!!).get().addOnSuccessListener { it4: DocumentSnapshot ->
                                    val grupos = it4.get("Grupos") as ArrayList<String>?
                                    val asignaturasE = it4.get("Asignaturas") as ArrayList<String>?
                                    if(!grupos.isNullOrEmpty() && !asignaturasE.isNullOrEmpty()){
                                        grupos.add(model.CodDocumento!!)
                                        if(!asignaturasE.contains(codAsignatura)){
                                            asignaturasE.add(codAsignatura)
                                            db.collection("Usuario").document(model.EmailUsuario!!).update("Asignaturas", asignaturasE)
                                        }
                                        db.collection("Usuario").document(model.EmailUsuario!!).update("Grupos", grupos)
                                    }
                                }
                                val topic = "/topics/" + model.EmailUsuario!!.substringBefore('@')
                                PushNotification(NotificationData(title, message), topic).also { itPN -> sendNotification(itPN) }
                                val documento = db.collection("Usuario").document(model.EmailUsuario!!).collection("Notificaciones").document()
                                documento.set(hashMapOf("CodDocumento" to model.CodDocumento,
                                        "Tipo" to "UnionG",
                                        "EmailUsuario" to emailCurrentUser,
                                        "Created" to FieldValue.serverTimestamp(),
                                        "Vista" to false)
                                )
                            }
                        }
                    }
                }

                val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, Home_Docente_Fragment()).addToBackStack(null).commit()

            }.setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }

        holder.Cancelar.setOnClickListener {
                val builder = AlertDialog.Builder(holder.itemView.context)
                if (model.Tipo == "A")
                    builder.setMessage("¿Desea borrar la solicitud de unión a esta asignatura?")
                else { builder.setMessage("¿Desea borrar la solicitud de unión a este grupo?") }
                builder.setCancelable(false).setPositiveButton("Sí") { dialog, id ->
                    val document = db.collection("Usuario").document(emailCurrentUser).collection("Notificaciones")
                            .whereEqualTo("CodDocumento", model.CodDocumento).whereEqualTo("EmailUsuario", model.EmailUsuario)
                    document.get().addOnSuccessListener { itd ->
                        for(doc in itd.documents){
                            db.collection("Usuario").document(emailCurrentUser)
                                    .collection("Notificaciones").document(doc.id).delete()
                        }
                    }
            }.setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
            val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
            activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, Home_Docente_Fragment()).addToBackStack(null).commit()
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

    class NotificacionAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView){
            val CodDocumento : Button = itemView.findViewById(R.id.idDocumento)
            val NombreUsuario : Button = itemView.findViewById(R.id.puntuador)
            val UrlFoto : CircleImageView = itemView.findViewById(R.id.imgUsuario)
            val Aceptar : Button = itemView.findViewById(R.id.bUnirUsuario)
            val Cancelar : Button = itemView.findViewById(R.id.bOmitirUsuario)
    }

}