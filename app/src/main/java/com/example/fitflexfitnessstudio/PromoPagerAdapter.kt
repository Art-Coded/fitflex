package com.example.fitflexfitnessstudio

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitflexfitnessstudio.adsfragment.AllPromosFragment
import com.example.fitflexfitnessstudio.adsfragment.RecommendedPromosFragment
import com.example.fitflexfitnessstudio.adsfragment.TopRatedPromosFragment

class PromoPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) { // Use Fragment instead of Context

    override fun getItemCount(): Int = 3  // Three tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PromoFragment1()
            1 -> PromoFragment2()
            2 -> PromoFragment3()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
