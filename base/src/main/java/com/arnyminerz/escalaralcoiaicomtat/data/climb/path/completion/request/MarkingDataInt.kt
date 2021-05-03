package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.request

import com.google.firebase.auth.FirebaseUser

open class MarkingDataInt(
    val user: FirebaseUser,
    val comment: String?,
    val notes: String?
)
