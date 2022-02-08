package de.practicetime.practicetime.ui.goals

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithCategories
import de.practicetime.practicetime.ui.MainActivity
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY
import de.practicetime.practicetime.utils.getDurationString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val PROGRESS_UPDATED = 1337

class ProgressUpdateActivity  : AppCompatActivity(R.layout.activity_progress_update) {

    private val progressAdapterData =
        ArrayList<GoalInstanceWithDescriptionWithCategories>()

    private var progressAdapter: GoalAdapter? = null

    private var skipAnimation = false

    private lateinit var continueButton : MaterialButton
    private lateinit var skipButton : MaterialButton

    override fun onBackPressed() {
        exitActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.activity_progress_update_toolbar))
        supportActionBar.apply {
            title = getString(R.string.progressUpdateTitle)
        }

        // prepare the skip and continue button
        continueButton =  findViewById(R.id.progressUpdateLeave)
        skipButton = findViewById(R.id.progressUpdateSkipAnimation)

        val latestSessionId = intent.extras?.getLong("KEY_SESSION")

        if (latestSessionId != null) {
            parseSession(latestSessionId)
        } else {
            exitActivity()
        }

        initProgressedGoalList()

        continueButton.setOnClickListener { exitActivity() }
    }

    private fun initProgressedGoalList() {
        progressAdapter = GoalAdapter(
            progressAdapterData,
            context = this,
            showEmptyHeader = true,
        )

        findViewById<RecyclerView>(R.id.progessUpdateGoalList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = progressAdapter
            itemAnimator = CustomAnimator(
                progressDuration = { if(skipAnimation) 400L else 1300L },
                addDuration = { if(skipAnimation) 300L else 250L },
                moveDuration = { if(skipAnimation) 400L else 500L },
            )
        }
    }

    /*************************************************************************
     * Parse latest session and extract goal progress
     *************************************************************************/

    private fun parseSession(sessionId: Long) {
        lifecycleScope.launch {
            val latestSession = PracticeTime.sessionDao.getWithSectionsWithCategoriesWithGoals(sessionId)
            val goalProgress = PracticeTime.goalDescriptionDao.computeGoalProgressForSession(latestSession)

            // get all active goal instances at the time of the session
            PracticeTime.goalInstanceDao.getWithDescriptionsWithCategories(
                goalDescriptionIds = goalProgress.keys.toList(),
                checkArchived = false,
                now = latestSession.sections.first().section.timestamp
            // store the progress in the database
            ).onEach { (instance, d) ->
                goalProgress[d.description.id].also { progress ->
                    if (progress != null && progress > 0) {
                        instance.progress += progress
                        PracticeTime.goalInstanceDao.update(instance)

                        // undo the progress locally after updating database for the animation to work
                        instance.progress -= progress
                    }
                }
            // filter out all instances, where the goal is already at 100%
            }.filter {
                it.instance.progress < it.instance.target
            }.also {
                // if no element is progressed which wasn't already at 100%, show ¯\_(ツ)_/¯
                if(it.isEmpty()) {
                    findViewById<LinearLayout>(R.id.shrug_layout).visibility = View.VISIBLE
                    findViewById<RecyclerView>(R.id.progessUpdateGoalList).visibility = View.GONE
                    skipButton.visibility = View.GONE
                    continueButton.visibility = View.VISIBLE
                // else start the progress animation
                } else {
                    startProgressAnimation(it, goalProgress)
                }
            }
        }
    }


    /*************************************************************************
     * Progress animation coordination function
     *************************************************************************/

    private fun startProgressAnimation(
        progressedGoals
            : List<GoalInstanceWithDescriptionWithCategories>,
        goalProgress: Map<Long, Int>
    ) {

        skipButton.setOnClickListener { button ->
            skipAnimation = true

            button.visibility = View.GONE
            continueButton.visibility = View.VISIBLE

            lifecycleScope.launch {
                progressedGoals
                    .slice(progressAdapterData.size until progressedGoals.size)
                    .reversed()
                    .also {
                        progressAdapterData.addAll(0, it)
                        progressAdapter?.notifyItemRangeInserted(
                            1,
                            it.size
                        )
                        delay(900L)
                    }.onEachIndexed { index, goal ->
                        goal.instance.progress += goalProgress[goal.description.description.id] ?: 0
                        progressAdapter?.notifyItemChanged(index + 1, PROGRESS_UPDATED)
                    }
            }
        }

        // for the animation we want to alternatingly show a new goal and then its progress
        lifecycleScope.launch {
            var firstGoal = true

            for (progressedGoal in progressedGoals) {

                delay(if(firstGoal) 0L else 1500L)

                if (skipAnimation) break

                progressAdapterData.add(0, progressedGoal)
                progressAdapter?.notifyItemInserted(1) // position 1, because the invisible header is at position 0

                // the progress animation for the first element should
                // start earlier since there is now fade-in beforehand
                repeat(100) {
                    delay(if(firstGoal) 5L else 10L)
                    if(skipAnimation) return@repeat
                }

                progressedGoal.also { (p_i, p_d) ->
                    p_i.progress += goalProgress[p_d.description.id] ?: 0
                    progressAdapter?.notifyItemChanged(
                        progressAdapterData.indexOfFirst { it == progressedGoal } + 1,
                        PROGRESS_UPDATED
                    )
                }
                if (skipAnimation) break

                firstGoal = false
            }

            // when animation is complete change the visibility of skip and continue button
            skipButton.visibility = View.GONE
            continueButton.visibility = View.VISIBLE
        }
    }

    /*************************************************************************
     * Utility functions
     *************************************************************************/

    private fun exitActivity() {
        // go back to MainActivity, make new intent so MainActivity gets reloaded and shows new session
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    /*************************************************************************
     * Progress bar animation
     *************************************************************************/

    private class ProgressBarAnimation(
        private val viewHolder: GoalAdapter.ViewHolder,
        private val from: Int,
        private val to: Int
    ) : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            var progress = (from + (to - from) * interpolatedTime).toInt()

            viewHolder.progressBarView.progress = progress

            // correct for the fps scaling factor
            val target = viewHolder.progressBarView.max / 60
            progress /= 60

            /** progress Indicator Text */
            val progressLeft = maxOf(0, target - progress)
             if(progressLeft > 0) {

                 viewHolder.goalProgressDoneIndicatorView.text = getDurationString(
                     progress,
                     TIME_FORMAT_HUMAN_PRETTY
                 )

                 viewHolder.goalProgressLeftIndicatorView.text = getDurationString(
                     progressLeft,
                     TIME_FORMAT_HUMAN_PRETTY
                 )

                 viewHolder.goalProgressAchievedView.visibility = View.INVISIBLE
                 viewHolder.goalProgressDoneIndicatorView.visibility = View.VISIBLE
                 viewHolder.goalProgressLeftIndicatorView.visibility = View.VISIBLE
             } else {
                viewHolder.goalProgressAchievedView.visibility = View.VISIBLE
                viewHolder.goalProgressDoneIndicatorView.visibility = View.GONE
                viewHolder.goalProgressLeftIndicatorView.visibility = View.GONE
            }
        }
    }

    /*************************************************************************
     * Custom animator
     *************************************************************************/

    private class CustomAnimator(
        val progressDuration: () -> Long,
        val addDuration: () -> Long,
        val moveDuration: () -> Long,
    ) : DefaultItemAnimator() {
        // change the duration of the fade in and move animation
        override fun getAddDuration() = addDuration()
        override fun getMoveDuration() = moveDuration()

        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            preInfo: ItemHolderInfo,
            postInfo: ItemHolderInfo
        ): Boolean {

            if (preInfo is GoalItemHolderInfo) {
                val progressBar = (newHolder as GoalAdapter.ViewHolder).progressBarView
                // multiply by 60 to grantee at least 60 fps
                progressBar.max *= 60
                val animator = ProgressBarAnimation(
                    newHolder,
                    preInfo.progress * 60,
                    (progressBar.progress * 60).let { if (it < progressBar.max) it else progressBar.max}
                )
                animator.duration = progressDuration()
                progressBar.startAnimation(animator)
                return true
            }

            return super.animateChange(oldHolder, newHolder, preInfo, postInfo)
        }

        override fun canReuseUpdatedViewHolder(
            viewHolder: RecyclerView.ViewHolder, payloads: MutableList<Any>
        ) = true

        override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder) = true


        override fun recordPreLayoutInformation(
            state: RecyclerView.State,
            viewHolder: RecyclerView.ViewHolder,
            changeFlags: Int,
            payloads: MutableList<Any>
        ): ItemHolderInfo {

            if (changeFlags == FLAG_CHANGED) {
                if (payloads[0] as? Int == PROGRESS_UPDATED) {
                    //Get the info from the viewHolder and save it to GoalItemHolderInfo
                    return GoalItemHolderInfo(
                        (viewHolder as GoalAdapter.ViewHolder).progressBarView.progress,
                    )
                }
            }
            return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads)
        }

        class GoalItemHolderInfo(val progress: Int) : ItemHolderInfo()
    }
}


