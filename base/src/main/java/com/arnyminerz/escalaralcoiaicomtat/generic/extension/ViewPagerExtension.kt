package com.arnyminerz.escalaralcoiaicomtat.generic.extension

import androidx.core.view.get
import com.google.android.material.bottomnavigation.BottomNavigationView

fun BottomNavigationView.select(position: Int) {
    selectedItemId = menu[position].itemId
}