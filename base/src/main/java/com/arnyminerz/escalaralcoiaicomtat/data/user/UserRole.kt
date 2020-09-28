package com.arnyminerz.escalaralcoiaicomtat.data.user

import androidx.annotation.ColorRes
import com.arnyminerz.escalaralcoiaicomtat.R

const val PERMISSION_MUTE = 1
const val PERMISSION_MODIFY_GRADE = 2
const val PERMISSION_MODIFY_BLOCK = 3
const val PERMISSION_MODIFY_ATTRIBUTES = 4
const val PERMISSION_DISABLE_ACCOUNTS = 5
const val PERMISSION_VIEW_APP = 6

enum class UserRole(
    val key: Int,
    val displayName: String? = null,
    @ColorRes val color: Int? = null,
    val parent: UserRole? = null,
    vararg permissions: Int
) {
    NORMAL(0),
    MODERATOR(1, "Mod", R.color.role_moderator, null, PERMISSION_MUTE),
    EXPERT(
        2,
        "Exp",
        R.color.role_expert,
        MODERATOR,
        PERMISSION_MODIFY_GRADE,
        PERMISSION_MODIFY_BLOCK
    ),
    MAINTAINER(3, "Mnt", R.color.role_maintainer, EXPERT, PERMISSION_MODIFY_ATTRIBUTES),
    ADMIN(4, "Adm", R.color.role_admin, MAINTAINER, PERMISSION_DISABLE_ACCOUNTS),
    DEVELOPER(5, "Dev", R.color.role_developer, ADMIN, PERMISSION_VIEW_APP);

    private val permissions: ArrayList<Int> = arrayListOf()

    companion object {
        fun find(key: Int): UserRole? {
            for (role in values())
                if (role.key == key)
                    return role
            return null
        }
    }

    init {
        this.permissions.addAll(permissions.toList())
        if (parent != null && parent.permissions.isNotEmpty())
            this.permissions.addAll(parent.permissions)
    }

    fun showChip(): Boolean = displayName != null
}