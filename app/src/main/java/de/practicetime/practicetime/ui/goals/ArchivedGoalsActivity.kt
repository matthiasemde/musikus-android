package de.practicetime.practicetime.ui.goals

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.GoalInstanceWithDescriptionWithCategories
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY
import de.practicetime.practicetime.utils.getDurationString
import kotlinx.coroutines.launch

class ArchivedGoalsActivity : AppCompatActivity() {

    private lateinit var archivedGoalsAdapter: ArchivedGoalsAdapter
    private val adapterData = ArrayList<GoalInstanceWithDescriptionWithCategories>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_archived_goals)

        setSupportActionBar(findViewById(R.id.activity_archived_goals_toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.archivedGoalsTitle)
        }

        archivedGoalsAdapter = ArchivedGoalsAdapter(
            this,
            adapterData,
            ::unarchiveHandler,
            ::deleteHandler
        )

        lifecycleScope.launch {
            PracticeTime.goalDescriptionDao.getArchivedWithCategories().forEach {
                adapterData.add(
                    GoalInstanceWithDescriptionWithCategories(
                        instance = PracticeTime.goalInstanceDao.getLatest(it.description.id),
                        description = it
                    )
                )
            }
            archivedGoalsAdapter.notifyItemRangeInserted(0, adapterData.size)
        }

        findViewById<RecyclerView>(R.id.activity_archived_goals_list).apply {
            layoutManager = LinearLayoutManager(this@ArchivedGoalsActivity)
            adapter = archivedGoalsAdapter
        }

    }

    private fun unarchiveHandler(descriptionId: Long, target: Int) {

    }

    private fun deleteHandler(goalDescriptionId: Long, position: Int) {
        lifecycleScope.launch {
            PracticeTime.goalDescriptionDao.deleteGoal(goalDescriptionId)
        }
        adapterData.remove(adapterData[position])
        archivedGoalsAdapter.notifyItemRemoved(position)
    }

    private class ArchivedGoalsAdapter(
        private val context: AppCompatActivity,
        private val archivedGoals: List<GoalInstanceWithDescriptionWithCategories>,
        private val unarchiveHandler: (descriptionId: Long, target: Int) -> Unit,
        private val deleteHandler: (descriptionId: Long, position: Int) -> Unit,
    ) : RecyclerView.Adapter<ArchivedGoalsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.listitem_archived_goal, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val archivedGoal = archivedGoals[position]

            val description = archivedGoal.description.description
            val categories = archivedGoal.description.categories

            val categoryColor = ColorStateList.valueOf(
                context.resources.getIntArray(R.array.category_colors)[
                        categories.firstOrNull()?.colorIndex ?: 0
                ]
            )

            holder.apply {

                /** Section Color */
                if(archivedGoal.description.description.type != GoalType.NON_SPECIFIC) {
                    sectionColorView.visibility = View.VISIBLE
                    sectionColorView.backgroundTintList = categoryColor
                } else {
                    sectionColorView.visibility = View.GONE
                }

                /** Goal Title */
                if(description.type == GoalType.NON_SPECIFIC) {
                    goalNameView.text = context.getString(R.string.goal_name_non_specific)
                } else {
                    goalNameView.apply {
                        text = categories.firstOrNull()?.name ?: "Delete me!"
                    }
                }

                /** Goal Description */
                val count = description.periodInPeriodUnits
                val periodFormatted =
                    when (description.periodUnit) {
                        GoalPeriodUnit.DAY -> context.resources.getQuantityString(R.plurals.time_period_day, count, count)
                        GoalPeriodUnit.WEEK -> context.resources.getQuantityString(R.plurals.time_period_week, count, count)
                        GoalPeriodUnit.MONTH -> context.resources.getQuantityString(R.plurals.time_period_month, count, count)
                    }

                goalDescriptionView.text = TextUtils.concat(
                    getDurationString(archivedGoal.instance.target, TIME_FORMAT_HUMAN_PRETTY),
                    " ",
                    periodFormatted
                )

                unarchiveButtonView.apply {
                    backgroundTintList = categoryColor
                    setOnClickListener {
                        unarchiveHandler(description.id, archivedGoal.instance.target)
                    }
                }

                deleteButtonView.apply {
                    backgroundTintList = categoryColor
                    setOnClickListener {
                        deleteHandler(description.id, layoutPosition)
                    }
                }
            }
        }

        override fun getItemCount() = archivedGoals.size

        private class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sectionColorView: ImageView = view.findViewById(R.id.sectionColor)
            val goalNameView: TextView = view.findViewById(R.id.goalName)
            val goalDescriptionView: TextView = view.findViewById(R.id.goalDescription)

            val unarchiveButtonView: MaterialButton = view.findViewById(R.id.listitem_archived_goal_btn_reactivate)
            val deleteButtonView: MaterialButton = view.findViewById(R.id.listitem_archived_goal_btn_delete)
        }

    }
}

