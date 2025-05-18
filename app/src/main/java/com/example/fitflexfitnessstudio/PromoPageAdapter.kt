package com.example.fitflexfitnessstudio

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitflexfitnessstudio.adsfragment.AllPromosFragment
import com.example.fitflexfitnessstudio.adsfragment.RecommendedPromosFragment
import com.example.fitflexfitnessstudio.adsfragment.TopRatedPromosFragment

class PromoPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3  // Three tabs

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllPromosFragment()
            1 -> TopRatedPromosFragment()
            2 -> RecommendedPromosFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}
