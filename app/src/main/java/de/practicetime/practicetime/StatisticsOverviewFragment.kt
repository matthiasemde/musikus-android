package de.practicetime.practicetime

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation.findNavController


class StatisticsOverviewFragment : Fragment(R.layout.fragment_statistics_overview) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<Button>(R.id.btn_open_session_history).setOnClickListener {
            val i = Intent(requireContext(), SessionStatsActivity::class.java)
            requireActivity().startActivity(i)
        }

        view.findViewById<Button>(R.id.btn_open_goals_history).setOnClickListener {
            val i = Intent(requireContext(), GoalStatsActivity::class.java)
            requireActivity().startActivity(i)
        }
    }

}