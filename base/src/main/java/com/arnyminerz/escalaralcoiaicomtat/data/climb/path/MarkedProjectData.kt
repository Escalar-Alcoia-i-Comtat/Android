package com.arnyminerz.escalaralcoiaicomtat.data.climb.path

import com.arnyminerz.escalaralcoiaicomtat.auth.VisibleUserData
import com.google.firebase.Timestamp

/**
 * Contains the data for marking a Path as project.
 * @author Arnau Mora
 * @since 20210430
 */
data class MarkedProjectData(
    val timestamp: Timestamp?,
    val user: VisibleUserData,
    val comment: String?,
    val notes: String?
) : MarkedDataInt
