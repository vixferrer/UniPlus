package com.example.tfg

data class User(var NombreCompleto : String ?= null,
                var Rol: String?=null,
                var UrlFotoPerfil : String ?= null,
                var Asignaturas : ArrayList<String> ?= null,
                var Grupos: ArrayList<String> ?= null)

