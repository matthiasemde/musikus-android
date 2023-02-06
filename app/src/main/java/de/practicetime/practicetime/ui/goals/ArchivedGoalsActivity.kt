/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.ui.goals

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.PracticeTime
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.GoalInstanceWithDescriptionWithLibraryItems
import de.practicetime.practicetime.database.PTDatabase
import de.practicetime.practicetime.database.entities.GoalPeriodUnit
import de.practicetime.practicetime.database.entities.GoalType
import de.practicetime.practicetime.utils.TIME_FORMAT_HUMAN_PRETTY
import de.practicetime.practicetime.utils.getDurationString
import kotlinx.coroutines.launch
import java.util.*

class ArchivedGoalsActivity : AppCompatActivity() {

    private lateinit var archivedGoalsAdapter: ArchivedGoalsAdapter
    private val adapterData = ArrayList<GoalInstanceWithDescriptionWithLibraryItems>()

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
            PTDatabase.getInstance(applicationContext).goalDescriptionDao.getArchivedWithLibraryItems().forEach {
                adapterData.add(
                    GoalInstanceWithDescriptionWithLibraryItems(
                        instance = PTDatabase.getInstance(applicationContext).goalInstanceDao.getLatest(it.description.id),
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

    private fun unarchiveHandler(archivedGoal: GoalInstanceWithDescriptionWithLibraryItems, position: Int) {
        val libraryItem = archivedGoal.description.libraryItems.firstOrNull()
        val restoreLibraryItem = libraryItem?.archived ?: false
        AlertDialog.Builder(this).apply {
            setMessage(
                if(restoreLibraryItem) resources.getString(
                    R.string.archivedGoalsConfirmUnarchiveWithLibraryItem
                ).format(libraryItem?.name ?: "")
                else resources.getString(R.string.archivedGoalsConfirmUnarchive)
            )
            setPositiveButton(R.string.archivedGoalsUnarchive) { dialog, _ ->
                lifecycleScope.launch {
                    libraryItem?.let {
                        it.archived = false
                        PTDatabase.getInstance(applicationContext).libraryItemDao.update(libraryItem)
                    }
                    PTDatabase.getInstance(applicationContext).goalDescriptionDao.unarchive(archivedGoal)
                }
                adapterData.remove(adapterData[position])
                archivedGoalsAdapter.notifyItemRemoved(position)
                Toast.makeText(context, R.string.archivedGoalsUnarchiveGoalToast, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            setNegativeButton(R.string.dialogDismiss) { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }

    private fun deleteHandler(goalDescriptionId: UUID, position: Int) {
        AlertDialog.Builder(this).apply {
            setMessage(R.string.archivedGoalsConfirmDelete)
            setPositiveButton(R.string.archivedGoalsDelete) { dialog, _ ->
                lifecycleScope.launch {
                    PTDatabase.getInstance(applicationContext).goalDescriptionDao.getAndDelete(goalDescriptionId)
                }
                adapterData.remove(adapterData[position])
                archivedGoalsAdapter.notifyItemRemoved(position)
                Toast.makeText(context, context.resources.getQuantityText(
                    R.plurals.deleteGoalToast, 1
                ), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            setNegativeButton(R.string.dialogDismiss) { dialog, _ ->
                dialog.cancel()
            }
        }.create().show()
    }

    private class ArchivedGoalsAdapter(
        private val context: AppCompatActivity,
        private val archivedGoals: List<GoalInstanceWithDescriptionWithLibraryItems>,
        private val unarchiveHandler: (
            archivedGoal: GoalInstanceWithDescriptionWithLibraryItems,
            position: Int
        ) -> Unit,
        private val deleteHandler: (descriptionId: UUID, position: Int) -> Unit,
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
            val libraryItems = archivedGoal.description.libraryItems

            val goalColor = libraryItems.firstOrNull()?.colorIndex?.let {
                ColorStateList.valueOf(
                    context.resources.getIntArray(R.array.library_item_colors)[it]
                )
            } ?: ColorStateList.valueOf(
                PracticeTime.getThemeColor(R.attr.colorPrimary, context)
            )

            holder.apply {

                /** Section Color */
                if(description.type != GoalType.NON_SPECIFIC) {
                    sectionColorView.visibility = View.VISIBLE
                    sectionColorView.backgroundTintList = goalColor
                } else {
                    sectionColorView.visibility = View.GONE
                }

                /** Goal Title */
                if(description.type == GoalType.NON_SPECIFIC) {
                    goalNameView.text = context.getString(R.string.goal_name_non_specific)
                } else {
                    goalNameView.apply {
                        text = libraryItems.firstOrNull()?.name ?: "Delete me!"
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
                    iconTint = goalColor
                    strokeColor = goalColor
                    setTextColor(goalColor)
                    rippleColor = goalColor
                    setOnClickListener {
                        unarchiveHandler(archivedGoal, layoutPosition)
                    }
                }

                deleteButtonView.apply {
                    backgroundTintList = goalColor
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

