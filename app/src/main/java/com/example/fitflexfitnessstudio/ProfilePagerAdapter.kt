package com.example.fitflexfitnessstudio

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitflexfitnessstudio.profilefrag.FirstFragment
import com.example.fitflexfitnessstudio.profilefrag.SecondFragment

class ProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2 // Number of tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FirstFragment()
            1 -> SecondFragment()
            else -> FirstFragment()
        }
    }
}
