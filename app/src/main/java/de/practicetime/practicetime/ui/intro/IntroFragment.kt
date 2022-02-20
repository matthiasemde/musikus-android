package de.practicetime.practicetime.ui.intro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlideSelectionListener
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
import kotlin.math.roundToInt


private const val DUMMY_MAIN_CATEGORY_INDEX = 5

private val dummyCategories = listOf(
    Category(name="B-Dur", colorIndex = 8),
    Category(name="Czerny Etude Nr.2", colorIndex = 1),
    Category(name="Trauermarsch c-Moll", colorIndex = 0),
    Category(name="Andantino", colorIndex = 6),
    Category(name="Klaviersonate", colorIndex = 7),
    Category(name="Mozart", colorIndex = 3)
)

class IntroFragment(
    @ColorRes override val defaultBackgroundColorRes: Int = R.color.md_red_300
) : Fragment(), SlideSelectionListener, SlideBackgroundColorHolder {
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
        var arrowText = ""
        var fabClickListener: View.OnClickListener? = null

        val heading = view.findViewById<TextView>(R.id.fragment_intro_title)
        val icon = view.findViewById<ImageView>(R.id.fragment_intro_iv_tab_icon)

        when(fragType) {
            IntroFragmentType.FRAGMENT_LIBRARY -> {
                heading.text = getString(R.string.navigationLibraryTitle)
                icon.setImageResource(R.drawable.ic_library)
                fragment = IntroLibraryFragment()
                description = getString(R.string.intro_text_library)
                arrowText = getString(R.string.intro_text_library_2)
                fabClickListener = libraryClickListener
            }
            IntroFragmentType.FRAGMENT_GOAL -> {
                heading.text = getString(R.string.navigationGoalsTitle)
                icon.setImageResource(R.drawable.ic_goals)
                fragment = IntroGoalsFragment()
                description = getString(R.string.intro_text_goals)
                arrowText = getString(R.string.intro_text_goals_2)
                fabClickListener = goalsClickListener
            }
            IntroFragmentType.FRAGMENT_SESSION -> {
                heading.text = getString(R.string.navigationSessionsTitle)
                icon.setImageResource(R.drawable.ic_sessions)
                fragment = IntroSessionsFragment()
                description = getString(R.string.intro_text_sessions)
                arrowText = getString(R.string.intro_text_sessions_2)
                fabClickListener = sessionsClickListener
            }
        }

        childFragmentManager.beginTransaction()
            .add(container.id, fragment)
            .commit()

        view.findViewById<TextView>(R.id.fragment_intro_text).text =
            description
        view.findViewById<TextView>(R.id.fragment_intro_arrow_text).text =
            arrowText


        val fab = view.findViewById<FloatingActionButton>(R.id.fragment_intro_fab)
        fab.setOnClickListener(fabClickListener)
        fab.visibility = View.INVISIBLE
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
                    PracticeTime.goalDescriptionDao.insertGoal(
                        newGoalDescriptionWithCategories,
                        firstTarget
                    )
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

    override fun onSlideDeselected() {}

    override fun onSlideSelected() {


        requireView().findViewById<TextView>(R.id.fragment_intro_arrow_text)
            .animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(800)
            .start()

        requireView().findViewById<ImageView>(R.id.fragment_intro_arrow)
            .animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                view?.findViewById<FloatingActionButton>(R.id.fragment_intro_fab)?.show()
            }
            .setStartDelay(1000)
            .start()
    }
}


class IntroLibraryFragment : Fragment(R.layout.fragment_intro_library) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val recView = view.findViewById<RecyclerView>(R.id.introCategoryList)
        val catDummyAdapter = CategoryAdapter(
            dummyCategories,
            context = requireActivity(),
        )

        recView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = catDummyAdapter
        }
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
                    dummyCategories[DUMMY_MAIN_CATEGORY_INDEX]
                )
            ),
        )
        return arrayListOf(goal1, goal2)
    }
}

class IntroSessionsFragment : Fragment(R.layout.fragment_intro_sessions) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        SessionSummaryAdapter.ViewHolder.ItemViewHolder(
            view.findViewById(R.id.fragment_intro_sessions_dummyitem),
            requireContext(),
            getDummySessions(),
            listOf(),
            {_,_ -> },
            {_,_ -> false },
            isInAdapter = false
        ).bind(0)

        view.findViewById<LinearLayout>(R.id.summaryDayLayout).visibility = View.GONE
}

private fun getDummySessions() =
    listOf(
        SessionWithSectionsWithCategories(
            session = Session(
                breakDuration = 60 * 10,
                rating = 4,
                comment = "Great session! \uD83D\uDE80"
            ),
            sections = listOf(
                SectionWithCategory(
                    Section(
                        sessionId = 1,
                        categoryId = 1,
                        duration = 60 * 10,
                        timestamp = getCurrTimestamp()
                    ),
                    dummyCategories[0]
                ),
                SectionWithCategory(
                    Section(
                        sessionId = 1,
                        categoryId = 1,
                        duration = 60 * 23,
                        timestamp = getCurrTimestamp()
                    ),
                    dummyCategories[1]
                ),
                SectionWithCategory(
                    Section(
                        sessionId = 1,
                        categoryId = 1,
                        duration = 60 * 37,
                        timestamp = getCurrTimestamp()
                    ),
                    dummyCategories[DUMMY_MAIN_CATEGORY_INDEX]
                ),
            )
        )
    )
}