package com.arnyminerz.escalaralcoiaicomtat.list.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import timber.log.Timber

class MainPagerAdapter(
    fa: FragmentActivity,
    val items: HashMap<Int, Fragment>
) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        Timber.v("Creating fragment $position")
        return items[position]!!
    }
}
