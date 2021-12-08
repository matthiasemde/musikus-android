package de.practicetime.practicetime

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

const val PROGRESS_UPDATED = 1337

class ProgressUpdateActivity  : AppCompatActivity(R.layout.activity_progress_update) {
    private lateinit var dao: PTDao

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

        openDatabase()

        val latestSessionId = intent.extras?.getInt("KEY_SESSION")

        if (latestSessionId != null) {
            parseSession(latestSessionId)
        } else {
            exitActivity()
        }

        initProgressedGoalList()

        // prepare the skip and continue button
        continueButton =  findViewById(R.id.progressUpdateLeave)
        skipButton = findViewById(R.id.progressUpdateSkipAnimation)

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
            itemAnimator = CustomAnimator(animationDuration = { if(skipAnimation) 200L else 1300L })
        }
    }

    /*************************************************************************
     * Parse latest session and extract goal progress
     *************************************************************************/

    private fun parseSession(sessionId: Int) {
        lifecycleScope.launch {
            val latestSession = dao.getSessionWithSections(sessionId)
            val goalProgress = dao.computeGoalProgressForSession(sessionId)

            // get all active goal instances at the time of the session
            dao.getGoalInstancesWithDescriptionsWithCategories(
                descriptionIds = goalProgress.keys.toList(),
                checkArchived = false,
                now = latestSession.sections.first().timestamp
            // store the progress in the database
            ).onEach { (instance, d) ->
                goalProgress[d.description.id].also { progress ->
                    if (progress != null && progress > 0) {
                        instance.progress += progress
                        dao.updateGoalInstance(instance)

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
                    findViewById<LinearLayout>(R.id.shrug).visibility = View.VISIBLE
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
        goalProgress: Map<Int, Int>
    ) {

        skipButton.setOnClickListener { button ->
            skipAnimation = true

            progressedGoals
                .slice(progressAdapterData.size until progressedGoals.size)
                .reversed()
                .onEach { goal ->
                    goal.instance.progress += goalProgress[goal.description.description.id] ?: 0
                }.also {
                    progressAdapterData.addAll(0, it)
                    progressAdapter?.notifyItemRangeInserted(
                        1,
                        it.size
                    )
                }

            button.visibility = View.GONE
            continueButton.visibility = View.VISIBLE
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

    private fun openDatabase() {
        val db = Room.databaseBuilder(
            this,
            PTDatabase::class.java, "pt-database"
        ).build()
        dao = db.ptDao
    }

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
        private val progressBar: ProgressBar,
        private val progressIndicator: TextView,
        private val from: Int,
        private val to: Int
    ) : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            var progress = (from + (to - from) * interpolatedTime).toInt()

            progressBar.progress = progress

            // correct for the fps scaling factor
            progress /= 60

            // progress Indicator Text
            val cappedProgress = minOf(progressBar.max / 60, progress)
            val progressHours = cappedProgress / 3600
            val progressMinutes = cappedProgress % 3600 / 60
            when {
                progressHours > 0 -> progressIndicator.text = String.format("%02d:%02d", progressHours, progressMinutes)
                progressMinutes > 0 -> progressIndicator.text = String.format("%d min", progressMinutes)
                else -> progressIndicator.text = "< 1 min"
            }
        }
    }

    /*************************************************************************
     * Custom animator
     *************************************************************************/

    private class CustomAnimator(
        val animationDuration: () -> Long
    ) : DefaultItemAnimator() {
        // change the duration of the fade in and move animation
        override fun getAddDuration() = 250L
        override fun getMoveDuration() = 500L

        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            preInfo: ItemHolderInfo,
            postInfo: ItemHolderInfo
        ): Boolean {

            if (preInfo is GoalItemHolderInfo) {
                val progressBar = (newHolder as GoalAdapter.ViewHolder).progressBarView
                val progressIndicator = newHolder.goalProgressIndicatorView
                // multiply by 60 to grantee at least 60 fps
                progressBar.max *= 60
                val animator = ProgressBarAnimation(
                    progressBar,
                    progressIndicator,
                    preInfo.progress * 60,
                    (progressBar.progress * 60).let { if (it < progressBar.max) it else progressBar.max}
                )
                animator.duration = animationDuration()
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


