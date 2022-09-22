package com.example.tfg

data class Asignatura (
    var Nombre : String ?= null,
    var UrlFoto : String ?= null,
    var Administradores : ArrayList<String> ?= null,
    var Integrantes : ArrayList<String> ?= null,
    var Grupos: ArrayList<String> ?= null
)