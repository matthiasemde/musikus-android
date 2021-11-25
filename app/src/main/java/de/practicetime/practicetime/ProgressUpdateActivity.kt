package de.practicetime.practicetime

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import de.practicetime.practicetime.entities.GoalWithCategories
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.recyclerview.widget.DefaultItemAnimator
import de.practicetime.practicetime.entities.GoalProgressType

const val PROGRESS_UPDATED = 1337

class ProgressUpdateActivity  : AppCompatActivity(R.layout.activity_progress_update) {
    private var dao: PTDao? = null

    private val progressedGoalsWithCategoriesShown = ArrayList<GoalWithCategories>()
    private val progressedGoalsWithCategories = ArrayList<GoalWithCategories>()

    private var progressAdapter : ProgressAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        openDatabase()

        initProgressedGoalList()

        Handler(Looper.getMainLooper()).postDelayed({parseLatestSession()}, 200)

        findViewById<Button>(R.id.progressUpdateLeave).setOnClickListener{ exitActivity() }
    }

    private fun initProgressedGoalList() {
        progressAdapter = ProgressAdapter(
            progressedGoalsWithCategoriesShown,
            context = this,
        )

        findViewById<RecyclerView>(R.id.progessUpdateGoalList).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = progressAdapter
            itemAnimator = CustomAnimator()
        }
    }

    private fun parseLatestSession() {
        lifecycleScope.launch {

            val latestSession = dao?.getLatestSessionWithSectionsWithCategoriesWithGoals()

            // goalProgress maps the goal id to its progress
            val goalProgress = mutableMapOf<Int, Int>()
            latestSession?.sections?.forEach { (section, categoryWithGoals) ->
                val (_, goals) = categoryWithGoals
                goals.forEach { goal ->
                    when (goal.progressType) {
                        GoalProgressType.TIME -> {
                            goalProgress[goal.id] =
                                goalProgress[goal.id] ?: 0 + (section.duration ?: 0)
                        }
                        GoalProgressType.SESSION_COUNT -> {
                            goalProgress[goal.id] = 1
                        }
                    }
                }
            }

            dao?.getSelectedActiveGoalsWithCategories(goalProgress.keys.toList())?.let {
                progressedGoalsWithCategories.addAll(it)
            }

            if(progressedGoalsWithCategories.size > 0) {
                progressedGoalsWithCategories.forEach { (goal, _) ->
                    goalProgress[goal.id].also { progress ->
                        if (progress != null && progress > 0) {
                            goal.progress += progress
                            dao?.updateGoal(goal)

                            // undo the progress locally after updating database for the animation to work
                            goal.progress -= progress
                        }
                    }
                }
                startProgressAnimation(goalProgress)
            }
        }
    }

    private fun startProgressAnimation(goalProgress: Map<Int, Int>) {
        // load all the progressed goals after a short delay to then show them one by one

        val handler = Handler(Looper.getMainLooper())

        // for the animation we want to alternatingly show a new goal and then its progress
        fun showGoal(goalIndex: Int) {
            fun showProgress(progressIndex: Int) {
                progressedGoalsWithCategoriesShown[0].goal.apply {
                    progress += goalProgress[this.id] ?: 0
                }
                progressAdapter?.notifyItemChanged(0, PROGRESS_UPDATED)

                // only continue showing goals, as long as there are more
                if(progressIndex + 1 < progressedGoalsWithCategories.size) {
                    handler.postDelayed({showGoal(progressIndex + 1)}, 1500)
                }
            }
            progressedGoalsWithCategoriesShown.add(0, progressedGoalsWithCategories[goalIndex])
            progressAdapter?.notifyItemInserted(0)

            // the progress animation for the first element should
            // start earlier since there is now fade-in beforehand
            handler.postDelayed({showProgress(goalIndex)}, if(goalIndex == 0) 500 else 1000)
        }

        // start the progress animation in reverse order with the last goal
        handler.post {showGoal(0)}
    }

    private class ProgressAdapter(
        private val progressedGoalsWithCategories: ArrayList<GoalWithCategories>,
        private val context: Activity,
    ) : RecyclerView.Adapter<ProgressAdapter.ViewHolder>() {

        override fun getItemCount() = progressedGoalsWithCategories.size

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.view_goal_item, viewGroup, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val (goal, categories) = progressedGoalsWithCategories[position]

            viewHolder.progressBarView
            val now = Date().time / 1000L

            viewHolder.progressBarView.max = goal.target
            viewHolder.progressBarView.progress = goal.progress

            val categoryColors =  context.resources.getIntArray(R.array.category_colors)
            viewHolder.progressBarView.progressTintList = ColorStateList.valueOf(
                categoryColors[categories.first().colorIndex]
            )

            viewHolder.progressPercentView.text = "${minOf(goal.progress * 100 / goal.target, 100)}%"

            val targetFormatted = formatTime(goal.target.toLong())
            val periodFormatted = formatTime(goal.period.toLong())

            viewHolder.goalNameView.text = "Practice ${categories.first().name} for $targetFormatted in $periodFormatted"

            val remainingTimeFormatted = formatTime(
                (goal.startTimestamp + goal.period) - now
            )
            viewHolder.remainingTimeView.text = "Time left:\n$remainingTimeFormatted"
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val progressBarView: ProgressBar = view.findViewById(R.id.goalProgressBar)
            val progressPercentView: TextView = view.findViewById(R.id.goalProgressPercent)
            val goalNameView: TextView = view.findViewById(R.id.goalName)
            val remainingTimeView: TextView = view.findViewById(R.id.goalRemainingTime)
        }
    }

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
        val pBundle = Bundle()
        pBundle.putInt("KEY_NEW_SESSION", 1)
        intent.putExtras(pBundle)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private class CustomAnimator() : DefaultItemAnimator(){
        // change the duration of the fade in and move animation
        override fun getAddDuration() = 250L
        override fun getMoveDuration() = 500L

        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            preInfo: ItemHolderInfo,
            postInfo: ItemHolderInfo
        ): Boolean {

            if(preInfo is GoalItemHolderInfo) {
                val progressBar = (newHolder as ProgressAdapter.ViewHolder).progressBarView
                val percentView = newHolder.progressPercentView
                progressBar.max *= 60  // multiply by 60 to grantee at least 60 fps
                val animator = ProgressBarAnimation(
                    progressBar,
                    percentView,
                    preInfo.progress * 60,
                    progressBar.progress * 60
                )
                animator.duration = 1300
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
                if (payloads[0] as? Int == PROGRESS_UPDATED){
                    //Get the info from the viewHolder and save it to GoalItemHolderInfo
                    return GoalItemHolderInfo(
                        (viewHolder as ProgressAdapter.ViewHolder).progressBarView.progress,
                    )
                }
            }
            return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads)
        }

        class GoalItemHolderInfo(val progress: Int) : ItemHolderInfo()
    }

    private class ProgressBarAnimation(
        private val progressBar: ProgressBar,
        private val percentView: TextView,
        private val from: Int,
        private val to: Int
    ) : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            super.applyTransformation(interpolatedTime, t)
            val value = from + (to - from) * interpolatedTime
            progressBar.progress = value.toInt()
            val progressInPercent = minOf((value * 100 / progressBar.max).toInt(), 100)
            percentView.text = "$progressInPercent%"
        }
    }
}