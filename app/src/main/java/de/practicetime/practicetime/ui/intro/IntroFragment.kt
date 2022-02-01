package de.practicetime.practicetime.ui.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.ui.goals.GoalAdapter
import de.practicetime.practicetime.ui.library.CategoryAdapter
import de.practicetime.practicetime.utils.SECONDS_PER_DAY
import de.practicetime.practicetime.utils.SECONDS_PER_HOUR
import de.practicetime.practicetime.utils.getStartOfDay
import de.practicetime.practicetime.utils.getStartOfWeek
import kotlin.math.roundToInt


class IntroFragment() : Fragment() {

    private lateinit var fragType: IntroFragmentType

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {

        fragType = arguments?.get(FRAGMENT_TYPE_KEY) as IntroFragmentType
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val container = view.findViewById<FragmentContainerView>(R.id.fragment_intro_frag_container_view)
        var fragment: Fragment? = null
        var description = ""

        when(fragType) {
            IntroFragmentType.FRAGMENT_LIBRARY -> {
                fragment = IntroLibraryFragment()
                description = getString(R.string.intro_text_library)
            }
            IntroFragmentType.FRAGMENT_GOAL -> {
                fragment = IntroGoalsFragment()
                description = getString(R.string.intro_text_goals)
            }
        }

        childFragmentManager.beginTransaction()
            .add(container.id, fragment)
            .commit()

        view.findViewById<TextView>(R.id.fragment_intro_text).text =
            description
    }


}


class IntroGoalsFragment : Fragment(R.layout.fragment_intro_goals) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val recView = view.findViewById<RecyclerView>(R.id.introGoalList)
        val goalsDummyAdapter = GoalAdapter(
            getDummyGoals(),
            context = requireActivity(),
        )

        recView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = goalsDummyAdapter
        }
    }

    private fun getDummyGoals(): List<GoalInstanceWithDescriptionWithCategories> {
        val goal1 = GoalInstanceWithDescriptionWithCategories(
            GoalInstance(
                goalDescriptionId = 1,
                startTimestamp = getStartOfDay(0).toEpochSecond(),
                periodInSeconds = SECONDS_PER_DAY,
                target = SECONDS_PER_HOUR,
                progress = (SECONDS_PER_HOUR * 0.7f).roundToInt()
            ),
            GoalDescriptionWithCategories(
                description = GoalDescription(
                    type = GoalType.NON_SPECIFIC,
                    oneTime = false,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.DAY,
                ),
                listOf()
            ),
        )
        val goal2 = GoalInstanceWithDescriptionWithCategories(
            GoalInstance(
                goalDescriptionId = 1,
                startTimestamp = getStartOfWeek(0).toEpochSecond(),
                periodInSeconds = SECONDS_PER_DAY * 7,
                target = (SECONDS_PER_HOUR * 5.5f).roundToInt(),
                progress = (SECONDS_PER_HOUR * 2f).roundToInt()
            ),
            GoalDescriptionWithCategories(
                description = GoalDescription(
                    type = GoalType.CATEGORY_SPECIFIC,
                    oneTime = false,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.WEEK,
                ),
                listOf(
                    Category(
                        id = 1,
                        name = getString(R.string.dummy_category_name),
                        colorIndex = 4
                    )
                )
            ),
        )

        return arrayListOf(goal1, goal2)
    }
}

class IntroLibraryFragment : Fragment(R.layout.fragment_intro_library) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    val recView = view.findViewById<RecyclerView>(R.id.introCategoryList)
    val catDummyAdapter = CategoryAdapter(
        getDummyCategories(),
        context = requireActivity(),
    )

    recView.apply {
        layoutManager = GridLayoutManager(context, 2)
        adapter = catDummyAdapter
    }
}

private fun getDummyCategories() =
    listOf(
        Category(name="Die Schöpfung", colorIndex=0),
        Category(name="Beethoven Septett", colorIndex=1),
        Category(name="Trauermarsch c-Moll", colorIndex=3),
        Category(name="Andantino", colorIndex=6),
        Category(name="Klaviersonate", colorIndex=7),
        Category(name=getString(R.string.dummy_category_name), colorIndex = 4)
    )

}