/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Michael Prommersberger
 * Additions and modifications, author Matthias Emde 
 */

package app.musikus.ui.intro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.musikus.R
import app.musikus.database.UUIDConverter
import app.musikus.database.daos.LibraryItem
import app.musikus.ui.activesession.ActiveSessionActivity
import app.musikus.ui.library.LibraryItemAdapter
import app.musikus.utils.TimeProvider
import com.github.appintro.BuildConfig
import com.github.appintro.SlideBackgroundColorHolder
import com.github.appintro.SlideSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.ZonedDateTime
import javax.inject.Inject


//private const val DUMMY_MAIN_CATEGORY_INDEX = 5



private val dummyLibraryItems = listOf(
    LibraryItem(
        id = UUIDConverter.deadBeef,
        createdAt = ZonedDateTime.parse("1997-05-07"),
        modifiedAt = ZonedDateTime.parse("1997-05-07"),
        name = "B-Dur",
        colorIndex = 8,
        libraryFolderId = null,
        customOrder = null
    ),
    LibraryItem(
        id = UUIDConverter.deadBeef,
        createdAt = ZonedDateTime.parse("1997-05-07"),
        modifiedAt = ZonedDateTime.parse("1997-05-07"),
        name = "Czerny Etude Nr.2",
        colorIndex = 1,
        libraryFolderId = null,
        customOrder = null
    ),
    LibraryItem(
        id = UUIDConverter.deadBeef,
        createdAt = ZonedDateTime.parse("1997-05-07"),
        modifiedAt = ZonedDateTime.parse("1997-05-07"),
        name = "Trauermarsch c-Moll",
        colorIndex = 0,
        libraryFolderId = null,
        customOrder = null
    ),
    LibraryItem(
        id = UUIDConverter.deadBeef,
        createdAt = ZonedDateTime.parse("1997-05-07"),
        modifiedAt = ZonedDateTime.parse("1997-05-07"),
        name = "Andantino",
        colorIndex = 6,
        libraryFolderId = null,
        customOrder = null
    ),
    LibraryItem(
        id = UUIDConverter.deadBeef,
        createdAt = ZonedDateTime.parse("1997-05-07"),
        modifiedAt = ZonedDateTime.parse("1997-05-07"),
        name = "Klaviersonate",
        colorIndex = 7,
        libraryFolderId = null,
        customOrder = null
    ),
    LibraryItem(
        id = UUIDConverter.deadBeef,
        createdAt = ZonedDateTime.parse("1997-05-07"),
        modifiedAt = ZonedDateTime.parse("1997-05-07"),
        name = "Mozart",
        colorIndex = 3,
        libraryFolderId = null,
        customOrder = null
    )
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
//        LibraryItemDialog(
//            context = requireActivity(),
//        ) { newLibraryItem ->
//            lifecycleScope.launch {
//                PTDatabase.getInstance(requireContext()).libraryItemDao.insert(newLibraryItem)
//                delay(200)
//                (requireActivity() as AppIntroActivity).changeSlide()
//            }
//        }.show()
    }

    private val goalsClickListener = View.OnClickListener {
//        lifecycleScope.launch {
//            GoalDialog(
//                context = requireActivity(),
//                libraryItems = Musikus.libraryItemDao.get(activeOnly = true),
//            ) { newGoalDescriptionWithLibraryItems, firstTarget ->
//                lifecycleScope.launch {
//                    Musikus.goalDescriptionDao.insertGoal(
//                        newGoalDescriptionWithLibraryItems,
//                        firstTarget
//                    )
//                    delay(200)
//                    (requireActivity() as AppIntroActivity).changeSlide()
//                }
//            }.show()
//        }
    }

    private val sessionsClickListener = View.OnClickListener {
        if (!BuildConfig.DEBUG) {
//            val prefs = requireActivity().getSharedPreferences(
//                getString(R.string.filename_shared_preferences), Context.MODE_PRIVATE)
//            prefs.edit().putBoolean(Musikus.PREFERENCES_KEY_APPINTRO_DONE, true).apply()
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

        val recView = view.findViewById<RecyclerView>(R.id.introLibraryItemList)
        val catDummyAdapter = LibraryItemAdapter(
            dummyLibraryItems,
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

//        val recView = view.findViewById<RecyclerView>(R.id.introGoalList)
//        val goalsDummyAdapter = GoalAdapter(
//            getDummyGoals(),
//            context = requireActivity(),
//        )
//
//        recView.apply {
//            layoutManager = LinearLayoutManager(context)
//            adapter = goalsDummyAdapter
//        }
    }

//    private fun getDummyGoals(): List<GoalInstanceWithDescriptionWithLibraryItems> {
//        val goal1 = GoalInstanceWithDescriptionWithLibraryItems(
//            GoalInstance(
//                goalDescriptionId = UUIDConverter.deadBeef,
//                startTimestamp = getStartOfDay(0).toEpochSecond(),
//                periodInSeconds = SECONDS_PER_DAY,
//                target = SECONDS_PER_HOUR,
//            ),
//            GoalDescriptionWithLibraryItems(
//                description = GoalDescription(
//                    type = GoalType.NON_SPECIFIC,
//                    repeat = true,
//                    periodInPeriodUnits = 1,
//                    periodUnit = GoalPeriodUnit.DAY,
//                    progressType = GoalProgressType.TIME,
//                    paused = false,
//                    archived = false,
//                    customOrder = null,
//                ),
//                listOf()
//            ),
//        )
//        val goal2 = GoalInstanceWithDescriptionWithLibraryItems(
//            GoalInstance(
//                goalDescriptionId = UUIDConverter.deadBeef,
//                startTimestamp = getStartOfWeek(0).toEpochSecond(),
//                periodInSeconds = SECONDS_PER_DAY * 7,
//                target = (SECONDS_PER_HOUR * 5.5f).roundToInt(),
//            ),
//            GoalDescriptionWithLibraryItems(
//                description = GoalDescription(
//                    type = GoalType.ITEM_SPECIFIC,
//                    repeat = true,
//                    periodInPeriodUnits = 1,
//                    periodUnit = GoalPeriodUnit.WEEK,
//                    progressType = GoalProgressType.TIME,
//                    paused = false,
//                    archived = false,
//                    customOrder = null,
//                ),
//                listOf(
//                    dummyLibraryItems[DUMMY_MAIN_CATEGORY_INDEX]
//                )
//            ),
//        )
//        return arrayListOf(goal1, goal2)
//    }
}

class IntroSessionsFragment : Fragment(R.layout.fragment_intro_sessions) {
    @Inject
    lateinit var timeProvider: TimeProvider

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

//        SessionSummaryAdapter.ViewHolder.ItemViewHolder(
//            view.findViewById(R.id.fragment_intro_sessions_dummyitem),
//            requireContext(),
//            getDummySessions(),
//            listOf(),
//            {_,_ -> },
//            {_,_ -> false },
//            isInAdapter = false
//        ).bind(0)
}

//private fun getDummySessions() =
//    listOf(
//        SessionWithSectionsWithLibraryItems(
//            session = Session(
//
//                breakDuration = 60 * 10,
//                rating = 4,
//                comment = "Great session! \uD83D\uDE80"
//            ),
//            sections = listOf(
//                SectionWithLibraryItem(
//                    Section(
//                        sessionId = UUIDConverter.deadBeef, // we don't care about id but it can't be null
//                        libraryItemId = UUIDConverter.deadBeef, // we don't care about id but it can't be null
//                        duration = 60 * 10,
//                        startTimestamp = timeProvider.now()
//                    ),
//                    dummyLibraryItems[0]
//                ),
//                SectionWithLibraryItem(
//                    Section(
//                        sessionId = UUIDConverter.deadBeef, // we don't care about id but it can't be null
//                        libraryItemId = UUIDConverter.deadBeef, // we don't care about id but it can't be null
//                        duration = 60 * 23,
//                        startTimestamp = timeProvider.now()
//                    ),
//                    dummyLibraryItems[1]
//                ),
//                SectionWithLibraryItem(
//                    Section(
//                        sessionId = UUIDConverter.deadBeef, // we don't care about id but it can't be null
//                        libraryItemId = UUIDConverter.deadBeef, // we don't care about id but it can't be null
//                        duration = 60 * 37,
//                        startTimestamp = timeProvider.now()
//                    ),
//                    dummyLibraryItems[DUMMY_MAIN_CATEGORY_INDEX]
//                ),
//            )
//        )
//    )
}