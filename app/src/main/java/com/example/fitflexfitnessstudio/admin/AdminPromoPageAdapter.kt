package com.example.fitflexfitnessstudio.admin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitflexfitnessstudio.admin.AdminPromoFragment
import com.example.fitflexfitnessstudio.admin.AdminClassFragment
import com.example.fitflexfitnessstudio.admin.AdminMembershippriceFragment

class AdminPromoPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3  // Three tabs for admin

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AdminPromoFragment()
            1 -> AdminMembershippriceFragment()
            2 -> AdminClassFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}