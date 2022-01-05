package de.practicetime.practicetime

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation.findNavController


class StatisticsOverviewFragment : Fragment(R.layout.fragment_statistics_overview) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<Button>(R.id.btn_open_session_history).setOnClickListener {
            var sessionStatsFrag = SessionsStatisticsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace((view.parent as ViewGroup).id, sessionStatsFrag, "statisticsSessionsFragment")
                .addToBackStack(null)
                .commit()

        }

        view.findViewById<Button>(R.id.btn_open_goals_history).setOnClickListener {
            var goalsStatsFrag = GoalsStatisticsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace((view.parent as ViewGroup).id, goalsStatsFrag, "statisticsGoalFragment")
                .addToBackStack(null)
                .commit()
        }
    }

}