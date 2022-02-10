package de.practicetime.practicetime.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.appintro.SlideBackgroundColorHolder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.practicetime.practicetime.BuildConfig
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.ui.activesession.ActiveSessionActivity
import de.practicetime.practicetime.ui.goals.GoalAdapter
import de.practicetime.practicetime.ui.goals.GoalDialog
import de.practicetime.practicetime.ui.library.CategoryAdapter
import de.practicetime.practicetime.ui.library.CategoryDialog
import de.practicetime.practicetime.ui.sessionlist.SessionSummaryAdapter
import de.practicetime.practicetime.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt


class IntroFragment(
    @ColorRes override val defaultBackgroundColorRes: Int = R.color.md_red_300
) : Fragment(), SlideBackgroundColorHolder {
    override val defaultBackgroundColor = 0

    private lateinit var fragType: IntroFragmentType

    override fun setBackgroundColor(backgroundColor: Int) {
        requireView().setBackgroundColor(backgroundColor)
    }

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
        var fabClickListener: View.OnClickListener? = null

        when(fragType) {
            IntroFragmentType.FRAGMENT_LIBRARY -> {
                fragment = IntroLibraryFragment()
                description = getString(R.string.intro_text_library)
                fabClickListener = libraryClickListener
            }
            IntroFragmentType.FRAGMENT_GOAL -> {
                fragment = IntroGoalsFragment()
                description = getString(R.string.intro_text_goals)
                fabClickListener = goalsClickListener
            }
            IntroFragmentType.FRAGMENT_SESSION -> {
                fragment = IntroSessionsFragment()
                description = getString(R.string.intro_text_sessions)
                fabClickListener = sessionsClickListener
            }
        }

        childFragmentManager.beginTransaction()
            .add(container.id, fragment)
            .commit()

        view.findViewById<TextView>(R.id.fragment_intro_text).text =
            description

        view.findViewById<FloatingActionButton>(R.id.fragment_intro_fab).setOnClickListener(
            fabClickListener
        )
    }

    private val libraryClickListener = View.OnClickListener {
        CategoryDialog(
            context = requireActivity(),
        ) { newCategory ->
            lifecycleScope.launch {
                PracticeTime.categoryDao.insert(newCategory)
                delay(200)
                (requireActivity() as AppIntroActivity).changeSlide()
            }
        }.show()
    }

    private val goalsClickListener = View.OnClickListener {
        lifecycleScope.launch {
            GoalDialog(
                context = requireActivity(),
                categories = PracticeTime.categoryDao.get(activeOnly = true),
            ) { newGoalDescriptionWithCategories, firstTarget ->
                lifecycleScope.launch {
                    val newGoalDescriptionId = PracticeTime.goalDescriptionDao
                        .insertGoalDescriptionWithCategories(
                        newGoalDescriptionWithCategories
                    )
                    PracticeTime.goalDescriptionDao.getWithCategories(
                        newGoalDescriptionId
                    )?.description?.createInstance(Calendar.getInstance(), firstTarget)?.let {
                        PracticeTime.goalInstanceDao.insert(it)
                    }
                    delay(200)
                    (requireActivity() as AppIntroActivity).changeSlide()
                }
            }.show()
        }
    }

    private val sessionsClickListener = View.OnClickListener {
        if (!BuildConfig.DEBUG) {
            val prefs = requireActivity().getSharedPreferences(
                getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PracticeTime.PREFERENCES_KEY_APPINTRO_DONE, true).apply()
        }
        val i = Intent(requireActivity(), ActiveSessionActivity::class.java)
        requireActivity().startActivity(i)
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
            Category(name="B-Dur", colorIndex=0),
            Category(name="Czerny Etude Nr.2", colorIndex=1),
            Category(name="Trauermarsch c-Moll", colorIndex=3),
            Category(name="Andantino", colorIndex=6),
            Category(name="Klaviersonate", colorIndex=7),
            Category(name=getString(R.string.dummy_category_name), colorIndex = 4)
        )

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
                    repeat = true,
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
                    repeat = true,
                    periodInPeriodUnits = 1,
                    periodUnit = GoalPeriodUnit.WEEK,
                ),
                listOf(
                    Category(
                        name = getString(R.string.dummy_category_name),
                        colorIndex = 4
                    )
                )
            ),
        )
        return arrayListOf(goal1, goal2)
    }
}

class IntroSessionsFragment : Fragment(R.layout.fragment_intro_sessions) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    val recView = view.findViewById<RecyclerView>(R.id.introSessionList)
    val sessionDummyAdapter = SessionSummaryAdapter(
        context = requireActivity(),
        isExpanded = true,
        getDummySessions(),
    )

    recView.apply {
        layoutManager = LinearLayoutManager(context)
        adapter = sessionDummyAdapter
    }
}

private fun getDummySessions() =
    listOf(
        SessionWithSectionsWithCategories(
            session = Session(
                breakDuration = 60 * 10,
                rating = 4,
                comment = "Great session!"
            ),
            sections = listOf(
                SectionWithCategory(
                    Section(
                        sessionId = 1,
                        categoryId = 1,
                        duration = 60 * 10,
                        timestamp = getCurrTimestamp()
                    ),
                    Category(name="B-Dur", colorIndex=0),
                ),
                SectionWithCategory(
                    Section(
                        sessionId = 1,
                        categoryId = 1,
                        duration = 60 * 23,
                        timestamp = getCurrTimestamp()
                    ),
                    Category(name="Czerny Etude Nr.2", colorIndex=1),
                ),
                SectionWithCategory(
                    Section(
                        sessionId = 1,
                        categoryId = 1,
                        duration = 60 * 37,
                        timestamp = getCurrTimestamp()
                    ),
                    Category(name=getString(R.string.dummy_category_name), colorIndex = 4)
                ),
            )
        )
    )
}