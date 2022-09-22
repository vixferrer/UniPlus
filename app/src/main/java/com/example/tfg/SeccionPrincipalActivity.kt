package com.example.tfg

import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.seccion_principal.*

class SeccionPrincipalActivity: AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener  {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.seccion_principal)

        /*-------------------Menú inferior-----------------------------*/
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val navController = findNavController(R.id.fragment)
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.home_Docente_Fragment, R.id.busqueda_Docente_Fragment, R.id.perfil_Docente_Fragment))
        bottomNavigationView.setupWithNavController(navController)

        /*-------------------Tool Bar y Menú lateral-----------------------------*/
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        toolbar.title = "Inicio"

        lateralNavigationView.bringToFront()
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        lateralNavigationView.setNavigationItemSelectedListener(this)

        /*-------------------Setup-----------------------------*/
        val bundle = intent.extras
        val email = FirebaseAuth.getInstance().currentUser.email
        val nombreTopic = email.substringBefore('@')
        FirebaseMessaging.getInstance().subscribeToTopic(nombreTopic) //este alumno se va a suscribir a su tema, cuyo nombre coincide con su email (hasta el @)
        lateralNavigationView.menu.clear()
        if(email.contains("alumnos")){
            lateralNavigationView.inflateMenu(R.menu.menu_lateral_estudiante)
        }else{
            lateralNavigationView.inflateMenu(R.menu.menu_lateral)
        }

        setup(email ?: "")

        /*-------------------Menú inferior-----------------------------*/
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            bottomNavigationView.menu.setGroupCheckable(0, true, true)
            lateralNavigationView.menu.setGroupCheckable(0, false, true)
            findViewById<FrameLayout>(R.id.fragment).removeAllViews()
            when (item.itemId) {
                R.id.home_Docente_Fragment -> {
                    val fragHome = Home_Docente_Fragment()
                    val bundle2 = Bundle()
                    bundle2.putString("email", FirebaseAuth.getInstance().currentUser.email)
                    fragHome.arguments = bundle2
                    supportFragmentManager.beginTransaction().replace(R.id.fragment, fragHome).addToBackStack("Home").commit()
                }
                R.id.busqueda_Docente_Fragment -> {
                    val bundle2 = Bundle()
                    bundle2.putString("email", FirebaseAuth.getInstance().currentUser.email)
                    val fragBusqueda = Busqueda_Docente_Fragment()
                    fragBusqueda.arguments = bundle2
                    supportFragmentManager.beginTransaction().replace(R.id.fragment, fragBusqueda).addToBackStack("Busqueda").commit()
                }
                R.id.perfil_Docente_Fragment -> {
                    val bundle2 = Bundle()
                    val fragPerfilDocente = Perfil_Docente_Fragment()
                    val fragPerfilEstudiante = Perfil_Estudiante_Fragment()
                    if(email!!.contains("alumnos")){
                        bundle2.putString("emailVisitante", FirebaseAuth.getInstance().currentUser.email)
                        bundle2.putString("emailEstudiante", FirebaseAuth.getInstance().currentUser.email)
                        fragPerfilEstudiante.arguments = bundle2
                        supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilEstudiante).addToBackStack("Perfil").commit()
                    }else{
                        bundle2.putString("emailDocente", FirebaseAuth.getInstance().currentUser.email)
                        bundle2.putString("emailVisitante", FirebaseAuth.getInstance().currentUser.email)
                        fragPerfilDocente.arguments = bundle2
                        supportFragmentManager.beginTransaction().replace(R.id.fragment, fragPerfilDocente).addToBackStack("Perfil").commit()
                    }
                }
            }
            true
        }

        /*-------------------Guardado de datos de usuario autenticado----------*/
        val prefs: SharedPreferences.Editor = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email).apply()
    }

    private fun setup(email: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Usuario").document(email).get().addOnSuccessListener {
            NombreCompleto_Menulateral.text = it.get("NombreCompleto") as String?
            Rol_Menulateral.text = it.get("Rol") as String?
            Picasso.with(this).load(it.get("UrlFotoPerfil") as String?).into(profile_image)
        }

        bCerrarSesion.setOnClickListener {
            val prefs: SharedPreferences.Editor = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            if(email!!.contains("alumnos"))
                FirebaseMessaging.getInstance().unsubscribeFromTopic(email.substringBefore('@'))

            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, InicioSesionActivity::class.java))
        }
    }

    //Para el menú lateral:
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val i = Intent()
        val fl : FrameLayout = findViewById<FrameLayout>(R.id.fragment)
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val btmNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val bundle2 = Bundle()

        bundle2.putString("email", email)

        lateralNavigationView.menu.setGroupCheckable(0, true, true)
        btmNav.menu.setGroupCheckable(0, false, true)

        when (item.itemId) {
            R.id.nav_asignaturas -> {
                val fragAsignaturas = AsignaturasDocente()
                fragAsignaturas.arguments = bundle2
                fl.removeAllViews()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, fragAsignaturas).addToBackStack(null).commit()
            }
            R.id.nav_competencias -> {
                val fragCompetencias = competencias_Docente_Fragment()
                fragCompetencias.arguments = bundle2
                fl.removeAllViews()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, fragCompetencias).addToBackStack(null).commit()
            }
            R.id.nav_ajustes_Docente -> {
                val fragAjustesDocente = Ajustes_Docente_Fragment()
                fragAjustesDocente.arguments = bundle2
                fl.removeAllViews()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, fragAjustesDocente).addToBackStack(null).commit()
            }
            R.id.nav_ajustes_estudiante ->{
                val fragAjustesEstudiante = Ajustes_Estudiante_Fragment()
                fragAjustesEstudiante.arguments = bundle2
                fl.removeAllViews()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, fragAjustesEstudiante).addToBackStack(null).commit()
            }
            R.id.nav_grupos ->{
                val fragGruposEstudiante = GruposEstudianteFragment()
                fragGruposEstudiante.arguments = bundle2
                fl.removeAllViews()
                supportFragmentManager.beginTransaction().replace(R.id.fragment, fragGruposEstudiante).addToBackStack(null).commit()
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val count = supportFragmentManager.backStackEntryCount
            var currentFragmentTag = String()
            val btmNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
            val homeItem : MenuItem =  btmNav.menu.getItem(0)
            val busqItem : MenuItem =  btmNav.menu.getItem(1)
            val perfilItem : MenuItem =  btmNav.menu.getItem(2)

            if (count>0 && supportFragmentManager.getBackStackEntryAt(count-1).name.equals("NUEVO")){
                findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                val fm: FragmentManager = supportFragmentManager
                for (i in 0 until fm.backStackEntryCount-2) {
                    fm.popBackStack()
                }
            }else if (count>0 && supportFragmentManager.getBackStackEntryAt(count-1).name.equals("ELIMINACION")){
                findViewById<FrameLayout>(R.id.fragment).removeAllViews()
                val fm: FragmentManager = supportFragmentManager
                for (i in 0 until fm.backStackEntryCount-2) {
                    fm.popBackStack()
                }
            }

            if(count==2 && supportFragmentManager.getBackStackEntryAt(count-1).name != null)
                currentFragmentTag = supportFragmentManager.getBackStackEntryAt(count-1).name!!

            if(currentFragmentTag == "Home_Docente" || count==0)
                finishAffinity()
            else if(count==1 && currentFragmentTag != "Home_Docente"){
                supportFragmentManager.beginTransaction().replace(R.id.fragment, Home_Docente_Fragment()).addToBackStack("Home_Docente").commit()
                btmNav.selectedItemId = homeItem.itemId
                supportFragmentManager.popBackStack(null,0)
            }
            else
                supportFragmentManager.popBackStack(null,0)
        }
    }

   override fun onRestart() {
        super.onRestart()
        //When BACK BUTTON is pressed, the activity on the stack is restarted
        //Do what you want on the refresh procedure here
    }
}
