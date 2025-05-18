package com.example.fitflexfitnessstudio.admin

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitflexfitnessstudio.admin.promo.AdminPromoSlide1Fragment
import com.example.fitflexfitnessstudio.admin.promo.AdminPromoSlide2Fragment
import com.example.fitflexfitnessstudio.admin.promo.AdminPromoSlide3Fragment

class AdminPromoPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AdminPromoSlide1Fragment()
            1 -> AdminPromoSlide2Fragment()
            2 -> AdminPromoSlide3Fragment()
            else -> AdminPromoSlide1Fragment()
        }
    }
}