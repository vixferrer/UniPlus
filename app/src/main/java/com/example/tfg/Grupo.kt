package com.example.tfg

data class Grupo (
    var Nombre : String ?= null,
    var UrlFoto : String ?= null,
    var Administradores : ArrayList<String> ?= null,
    var Integrantes : ArrayList<String> ?= null,
    var Competencias: ArrayList<String> ?= null
)