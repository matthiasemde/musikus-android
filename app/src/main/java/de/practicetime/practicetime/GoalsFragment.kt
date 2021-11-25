package de.practicetime.practicetime

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

private var dao: PTDao? = null

const val SECONDS_PER_MONTH = 60 * 60 * 24 * 28 // don't judge me :(
const val SECONDS_PER_WEEK = 60 * 60 * 24 * 7
const val SECONDS_PER_DAY = 60 * 60 * 24
const val SECONDS_PER_HOUR = 60 * 60

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    private val activeGoalsWithCategories = ArrayList<GoalWithCategories>()

    private var goalAdapter : GoalAdapter? = null

    private var addGoalDialog: GoalDialog? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openDatabase()

        initGoalList()

        // create a new category dialog for adding new goals
        addGoalDialog = GoalDialog(
            requireActivity(),
            dao!!,
            lifecycleScope,
            ::addGoalHandler
        )

        view.findViewById<FloatingActionButton>(R.id.goalsFab).setOnClickListener {
            addGoalDialog?.show()
        }
    }

    private fun initGoalList() {
        goalAdapter = GoalAdapter(
            activeGoalsWithCategories,
            context = requireActivity(),
        )

        requireActivity().findViewById<RecyclerView>(R.id.goalList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = goalAdapter
        }

        // load all active goals from the database and notify the adapter
        lifecycleScope.launch {
            dao?.getActiveGoalsWithCategories()?.let {
                activeGoalsWithCategories.addAll(it)
            }
            goalAdapter?.notifyItemRangeInserted(0, activeGoalsWithCategories.size)
        }
    }

    // the handler for creating new goals
    private fun addGoalHandler(newGoalWithCategories: GoalWithCategories) {
        lifecycleScope.launch {
            dao?.insertGoalWithCategories(newGoalWithCategories)
            activeGoalsWithCategories.add(newGoalWithCategories)
            goalAdapter?.notifyItemInserted(activeGoalsWithCategories.size)
        }
    }

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            requireContext(),
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }
}
