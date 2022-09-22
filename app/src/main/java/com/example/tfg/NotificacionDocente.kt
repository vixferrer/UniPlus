package com.example.tfg

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue

data class NotificacionDocente (
    var CodDocumento : String ?= null,
    var Tipo: String?= null,
    var EmailUsuario : String ?= null,
    var Created : Timestamp ?= null
)