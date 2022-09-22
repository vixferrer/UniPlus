package com.example.tfg

import android.graphics.Color
import android.graphics.Color.GRAY
import android.graphics.Color.RED
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.AccessController.getContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList

private const val TOPIC = "/topics/myTopic"

class CompetenciaAdapter(options: FirestoreRecyclerOptions<Competencia>) :
        FirestoreRecyclerAdapter<Competencia, CompetenciaAdapter.CompetenciaAdapterVH>(options) {

    private var emailPerfil: String = ""
    private var emailVisitante: String = ""
    private var idGrupo: String = ""
    private lateinit var fragm: Fragment
    private var nombreFragment: String = ""
    private var guardadas : ArrayList<String> = ArrayList<String>()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "CompetenciaAdapter"

    fun setEmail(email : String) {
        emailPerfil = email
    }

    fun setEmailVisitante(email : String) {
        emailVisitante = email
    }

    fun setidGrupo(id: String) {
        idGrupo = id
    }

    fun setFragment(frag : Fragment){
        fragm = frag
    }

    fun setSeleccionadas(competencias: ArrayList<String>){
        guardadas = ArrayList<String>()
        for(item in competencias)
            guardadas.add(item)
        notifyDataSetChanged()
    }

    fun setFragmentLlamador(nomFragment : String){
        nombreFragment = nomFragment
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompetenciaAdapterVH {
        return CompetenciaAdapterVH(LayoutInflater.from(parent.context).inflate(R.layout.competencia_item, parent, false))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CompetenciaAdapterVH, position: Int, model: Competencia) {
        if(nombreFragment == "Competencias-Docente")
            holder.Puntuacion.visibility = GONE
        //Si la competencia actual está en la lista de competencias de esta asignatura la mostramos
        if(guardadas.contains(snapshots.getSnapshot(position).id)){
            holder.itemView.visibility = VISIBLE
            holder.Codigo.text = model.Codigo
            if(model.Titulo!!.isNotEmpty())
                holder.Titulo.text = model.Titulo
            else{holder.Descripcion.text = model.Descripcion}

            //Para la puntuación:
            val idCompetenciaPuntuada : String = snapshots.getSnapshot(position).id
            var yaEstaPuntuada : Boolean = false

            if(nombreFragment == "Perfil-Estudiante"){
                val documento = db.collection("Usuario").document(emailPerfil).collection("Puntuaciones").document(idCompetenciaPuntuada)
                documento.get().addOnSuccessListener { doc->
                    if(doc.exists()) {
                        val puntos = doc.get("Puntos") as Long?
                        yaEstaPuntuada = true
                        if(puntos!=null)
                            holder.Puntuacion.text = puntos.toString()
                    }
                }

                holder.Puntuacion.isActivated = emailVisitante != emailPerfil

                if(!yaEstaPuntuada){
                    if(holder.Puntuacion.isActivated){
                        holder.Puntuacion.text = "+"
                        holder.Puntuacion.visibility = VISIBLE
                    }
                    else{
                        holder.Puntuacion.text = "0"
                    }
                }

                if(holder.Puntuacion.isActivated){
                    holder.Puntuacion.setOnClickListener {itVista ->
                        val builder = AlertDialog.Builder(holder.itemView.context)
                        builder.setMessage("¿Desea puntuarle la competencia "+ holder.Codigo.text +"?")
                        builder.setCancelable(false)
                                .setPositiveButton("Sí"){ dialog, id ->
                                    val title = "¡Una competencia ha sido puntuada!"
                                    db.collection("Usuario").document(emailVisitante).get().addOnSuccessListener {
                                        val nombre = it.get("NombreCompleto") as String?
                                        if(nombre!=null){
                                            db.collection("Competencia").document(idCompetenciaPuntuada).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                                val codigo = it2.get("Codigo") as String?
                                                val message = nombre + " le ha puntuado la competencia " + codigo
                                                if(title!=null && message != null){
                                                    val topic = "/topics/"+emailPerfil.substringBefore('@')
                                                    PushNotification(NotificationData(title, message), topic).also { itPN-> sendNotification(itPN) }
                                                    val idAutomatico : String = db.collection("Usuario").document(emailPerfil).collection("Notificaciones").document().id
                                                    val documento = db.collection("Usuario").document(emailPerfil).collection("Notificaciones").document()
                                                    val currentDateTime = LocalDateTime.now()
                                                    documento.set(
                                                            hashMapOf("CodDocumento" to idCompetenciaPuntuada,
                                                                    "Tipo" to "Puntuacion",
                                                                    "EmailUsuario" to emailVisitante,
                                                                    "Created" to  FieldValue.serverTimestamp(),
                                                                    "Vista" to false)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (yaEstaPuntuada) {
                                        documento.update("Puntos", FieldValue.increment(1))
                                        documento.get().addOnSuccessListener {
                                            val puntuacionUsuario = it.get(emailVisitante.replace('.', ',')) as Long?
                                            val historialEst = it.get("HistorialEstudiantes") as ArrayList<String>?
                                            val historialDoc = it.get("HistorialDocentes") as ArrayList<String>?
                                            if (puntuacionUsuario != null)
                                                documento.update(emailVisitante.replace('.', ','), FieldValue.increment(1))
                                            else { documento.update(emailVisitante.replace('.', ','), 1) }
                                            if(emailVisitante.contains("alumnos")){
                                                if(historialEst!=null){
                                                    if(!historialEst.contains(emailVisitante)){
                                                        historialEst.add(emailVisitante)
                                                        documento.update("HistorialEstudiantes", historialEst)
                                                    }
                                                }else{
                                                    val lista = ArrayList<String>()
                                                    lista.add(emailVisitante)
                                                    documento.update("HistorialEstudiantes", lista)
                                                }
                                            }else{
                                                if(historialDoc!=null){
                                                    if(!historialDoc.contains(emailVisitante)){
                                                        historialDoc.add(emailVisitante)
                                                        documento.update("HistorialDocentes", historialDoc)
                                                    }
                                                }else{
                                                    val lista = ArrayList<String>()
                                                    lista.add(emailVisitante)
                                                    documento.update("HistorialDocentes", lista)
                                                }
                                            }

                                        }
                                    } else {
                                        documento.set(hashMapOf("Puntos" to 1, emailVisitante.replace('.', ',') to 1))
                                        if(emailVisitante.contains("alumnos")){
                                            val historialEst = ArrayList<String>()
                                            historialEst.add(emailVisitante)
                                            documento.update("HistorialEstudiantes", historialEst)
                                        }else{
                                            val historialDoc = ArrayList<String>()
                                            historialDoc.add(emailVisitante)
                                            documento.update("HistorialDocentes", historialDoc)
                                        }
                                        yaEstaPuntuada = true
                                    }

                                    documento.get().addOnSuccessListener { doc ->
                                        val puntos = doc.get("Puntos") as Long?
                                        holder.Puntuacion.text = puntos.toString()
                                    }
                                }.setNegativeButton("No") { dialog, id ->
                                    dialog.dismiss()
                                }
                        val alert = builder.create()
                        alert.show()
                    }
                }else{
                    holder.Puntuacion.backgroundTintList = holder.Puntuacion.context.getColorStateList(R.color.grey)
                }
            }
            else if(nombreFragment == "Perfil-Grupo"){
                val documento = db.collection("Grupo").document(idGrupo).collection("Puntuaciones").document(idCompetenciaPuntuada)
                documento.get().addOnSuccessListener { doc->
                    if(doc.exists()) {
                        val puntos = doc.get("Puntos") as Long?
                        yaEstaPuntuada = true
                        if(puntos!=null)
                            holder.Puntuacion.text = puntos.toString()
                    }
                }

                holder.Puntuacion.isActivated = !emailVisitante.contains("alumnos")

                if(!yaEstaPuntuada){
                    if(holder.Puntuacion.isActivated){
                        holder.Puntuacion.text = "+"
                        holder.Puntuacion.visibility = VISIBLE
                    }else{
                        holder.Puntuacion.text = "0"
                    }
                }

                if(holder.Puntuacion.isActivated) {
                    holder.Puntuacion.setOnClickListener {
                        val builder = AlertDialog.Builder(holder.itemView.context)
                        builder.setMessage("¿Desea puntuarle la competencia "+holder.Codigo.text+ " a este grupo?")
                                .setCancelable(false)
                                .setPositiveButton("Sí"){ dialog, id ->
                                    val title = "¡Una competencia ha sido puntuada!"
                                    db.collection("Usuario").document(emailVisitante).get().addOnSuccessListener {
                                        val nombre = it.get("NombreCompleto") as String?
                                        if(nombre!=null){
                                            db.collection("Competencia").document(idCompetenciaPuntuada).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                                val codigo = it2.get("Codigo") as String?
                                                val message = nombre + " os ha puntuado la competencia " + codigo
                                                if(title!=null && message != null){
                                                    db.collection("Grupo").document(idGrupo).get().addOnSuccessListener { it3: DocumentSnapshot ->
                                                        val integrantesGrupo = it3.get("Integrantes") as java.util.ArrayList<String>?
                                                        for (integranteG in integrantesGrupo!!) {
                                                            if (integranteG.contains("alumnos")) {
                                                                val topic = "/topics/"+integranteG.substringBefore('@')
                                                                PushNotification(NotificationData(title, message), topic).also { itPN-> sendNotification(itPN) }
                                                                val documento = db.collection("Usuario").document(integranteG).collection("Notificaciones").document()
                                                                val currentDateTime = LocalDateTime.now()
                                                                documento.set(
                                                                        hashMapOf("CodDocumento" to idCompetenciaPuntuada,
                                                                                "Tipo" to "Puntuacion",
                                                                                "EmailUsuario" to emailVisitante,
                                                                                "Created" to  FieldValue.serverTimestamp(),
                                                                                "Vista" to false)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (yaEstaPuntuada) {
                                        documento.update("Puntos", FieldValue.increment(1))
                                        //tambien actualizamos la competencia puntuada a cada integrante de este grupo:
                                        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                            val integrantesGrupo = it2.get("Integrantes") as java.util.ArrayList<String>?
                                            for(integranteG in integrantesGrupo!!) {
                                                if (integranteG.contains("alumnos")){
                                                    val documentoCom = db.collection("Usuario").document(integranteG).collection("Puntuaciones").document(idCompetenciaPuntuada)
                                                    documentoCom.update("Puntos", FieldValue.increment(1))
                                                    documentoCom.get().addOnSuccessListener {
                                                        val puntuacionUsuario = it.get(emailVisitante.replace('.', ',')) as Long?
                                                        val historialDoc = it.get("HistorialDocentes") as ArrayList<String>?
                                                        if (puntuacionUsuario != null)
                                                            documentoCom.update(emailVisitante.replace('.', ','), FieldValue.increment(1))
                                                        else { documentoCom.update(emailVisitante.replace('.', ','), 1) }
                                                        if(historialDoc!=null){
                                                                if(!historialDoc.contains(emailVisitante)){
                                                                    historialDoc.add(emailVisitante)
                                                                    documentoCom.update("HistorialDocentes", historialDoc)
                                                                }
                                                        }else{
                                                                val lista = ArrayList<String>()
                                                                lista.add(emailVisitante)
                                                                documentoCom.update("HistorialDocentes", lista)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else {
                                        documento.set(hashMapOf("Puntos" to 1))
                                        db.collection("Grupo").document(idGrupo).get().addOnSuccessListener { it2: DocumentSnapshot ->
                                            val integrantesGrupo = it2.get("Integrantes") as java.util.ArrayList<String>?
                                            for(integranteG in integrantesGrupo!!) {
                                                if (integranteG.contains("alumnos")){
                                                    val documento = db.collection("Usuario").document(integranteG).collection("Puntuaciones").document(idCompetenciaPuntuada)
                                                    documento.get().addOnSuccessListener { doc->
                                                        if(doc.exists()) {
                                                            val puntosCompetencia = it2.get("Puntos") as Int?
                                                            if(puntosCompetencia!=null)
                                                                documento.update("Puntos", FieldValue.increment(1))
                                                            val puntuacionUsuario = doc.get(emailVisitante.replace('.', ',')) as Long?
                                                            val historialDoc = doc.get("HistorialDocentes") as ArrayList<String>?
                                                            if (puntuacionUsuario != null)
                                                                documento.update(emailVisitante.replace('.', ','), FieldValue.increment(1))
                                                            else { documento.update(emailVisitante.replace('.', ','), 1) }
                                                            if(historialDoc!=null){
                                                                if(!historialDoc.contains(emailVisitante)){
                                                                    historialDoc.add(emailVisitante)
                                                                    documento.update("HistorialDocentes", historialDoc)
                                                                }
                                                            }else{
                                                                val lista = ArrayList<String>()
                                                                lista.add(emailVisitante)
                                                                documento.update("HistorialDocentes", lista)
                                                            }
                                                        }
                                                        else{
                                                            val lista = ArrayList<String>()
                                                            lista.add(emailVisitante)
                                                            documento.set(hashMapOf("Puntos" to 1, emailVisitante.replace('.', ',') to 1, "HistorialDocentes" to lista))
                                                        }
                                                    }
                                                }
                                                    db.collection("Usuario").document(integranteG).collection("Puntuaciones").document(idCompetenciaPuntuada).set(hashMapOf("Puntos" to 1, emailVisitante.replace('.', ',') to 1))
                                            }
                                        }
                                        yaEstaPuntuada = true
                                    }
                                    documento.get().addOnSuccessListener { doc->
                                        val puntos = doc.get("Puntos") as Long?
                                        holder.Puntuacion.text = puntos.toString()
                                    }
                                }.setNegativeButton("No") { dialog, id ->
                                    dialog.dismiss()
                                }
                        val alert = builder.create()
                        alert.show()
                    }
                }else{
                    holder.Puntuacion.backgroundTintList = holder.Puntuacion.context.getColorStateList(R.color.grey)
                }

            }

        }else{
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }

        holder.itemView.setOnClickListener{
            val bundle = Bundle()
            val fragPerfilCompetencia = Perfil_Competencia_Fragment()
            val activity : AppCompatActivity = holder.itemView.context as AppCompatActivity
            val transaction = activity.supportFragmentManager.beginTransaction()
            bundle.putString("email", emailVisitante)
            bundle.putString("idCompetencia", snapshots.getSnapshot(position).id)
            fragPerfilCompetencia.arguments = bundle
            if(nombreFragment == "Perfil-Grupo"){
                activity.supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilCompetencia).addToBackStack(null).commit()
            }
            else{
                transaction.add((fragm.requireView().parent as ViewGroup).id, fragPerfilCompetencia)
                transaction.hide(fragm)
                transaction.addToBackStack(fragm::class.java.simpleName)
                transaction.commit()
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

    class CompetenciaAdapterVH(itemView : View) : RecyclerView.ViewHolder(itemView){
        val Codigo : TextView = itemView.findViewById(R.id.codigo)
        val Titulo : TextView = itemView.findViewById(R.id.descripcion)
        val Descripcion : TextView = itemView.findViewById(R.id.descripcion)
        val Puntuacion : Button = itemView.findViewById(R.id.bPuntuar)
    }
}