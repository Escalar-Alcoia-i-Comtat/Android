package com.arnyminerz.escalaralcoiaicomtat.data.climb.path.completion.request

import com.google.firebase.auth.FirebaseUser

/**
 * Contains the data for marking a Path as project.
 * @author Arnau Mora
 * @since 20210430
 */
class MarkProjectData(
    user: FirebaseUser,
    comment: String?,
    notes: String?
) : MarkingDataInt(user, comment, notes)
