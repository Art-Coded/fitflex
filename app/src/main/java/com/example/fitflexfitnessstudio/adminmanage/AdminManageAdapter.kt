package com.example.fitflexfitnessstudio.adminmanage

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitflexfitnessstudio.admin.AdminPromoFragment
import com.example.fitflexfitnessstudio.admin.AdminClassFragment
import com.example.fitflexfitnessstudio.admin.AdminMembershippriceFragment

class AdminManageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3  // Three tabs for admin

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MembersFragment()
            1 -> AttendanceFragment()
            2 -> ManageNotifFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}