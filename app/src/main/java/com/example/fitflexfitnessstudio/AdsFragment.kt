package com.example.fitflexfitnessstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AdsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout: TabLayout = view.findViewById(R.id.tabLayout)
        val viewPager: ViewPager2 = view.findViewById(R.id.viewPager)

        // Set up ViewPager2 with Adapter
        val adapter = PromoPageAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
        // Connect TabLayout and ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Promos"
                1 -> "Membership"
                2 -> "Classes"
                else -> "Tab $position"
            }
        }.attach()
    }
}
