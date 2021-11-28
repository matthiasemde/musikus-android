package de.practicetime.practicetime

import android.app.Activity
import android.graphics.Typeface
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import java.util.*


class GoalDialog(
    context: Activity,
    dao: PTDao,
    lifecycleScope: LifecycleCoroutineScope,
    onCreateHandler: (newGoal: GoalWithCategories) -> Unit,
) {

    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater;
    private val dialogView = inflater.inflate(
        R.layout.dialog_view_add_or_edit_goal,
        null,
    )

    // find and save all the views in the dialog view
    private val goalDialogTitleView = dialogView.findViewById<TextView>(R.id.categoryDialogTitle)
    private val goalDialogAllCategoriesView = dialogView.findViewById<TextView>(R.id.goalDialogAllCategories)
    private val goalDialogSpecificCategoriesView = dialogView.findViewById<TextView>(R.id.goalDialogSpecificCategories)
    private val goalDialogTypeSwitchView = dialogView.findViewById<SwitchCompat>(R.id.goalDialogTypeSwitch)
    private val goalDialogCategorySelectorView = dialogView.findViewById<Spinner>(R.id.goalDialogCategorySelector)
    private val goalDialogTargetHoursView = dialogView.findViewById<EditText>(R.id.goalDialogHours)
    private val goalDialogTargetMinutesView = dialogView.findViewById<EditText>(R.id.goalDialogMinutes)
    private val goalDialogPeriodValueView = dialogView.findViewById<EditText>(R.id.goalDialogPeriodValue)
    private val goalDialogPeriodUnitView = dialogView.findViewById<Spinner>(R.id.goalDialogPeriodUnit)

    private val selectedCategories = ArrayList<Category>()

    private var alertDialog: AlertDialog? = null

    init {

        initCategorySelector(context, dao, lifecycleScope)

        initTimeSelector(context)

        // dialog Setup
        alertDialogBuilder.apply {
            // pass the dialogView to the builder
            setView(dialogView)

            // define the callback function for the positive button
            setPositiveButton(R.string.addCategoryAlertOk) { dialog, _ ->
                if(isComplete()) {
                    lifecycleScope.launch {
                        val newGoalGroupId = (dao.getMaxGoalGroupId()?: 0L).toInt() + 1
                        val newGoal = computeNewGoal(
                            groupId = newGoalGroupId,
                            type = if (goalDialogTypeSwitchView.isChecked)
                                GoalType.SPECIFIC_CATEGORIES else
                                    GoalType.TOTAL_TIME,
                            timeFrame = Calendar.getInstance(),
                            periodInPeriodUnits = goalDialogPeriodValueView.text.toString().toInt(),
                            periodUnit = GoalPeriodUnit.values()[goalDialogPeriodUnitView.selectedItemPosition],
                            target = goalDialogTargetHoursView.text.toString().toInt() * 3600 +
                                    goalDialogTargetMinutesView.text.toString().toInt() * 60
                        )
                        val newGoalWithCategories = GoalWithCategories(
                            goal = newGoal,
                            categories = if (goalDialogTypeSwitchView.isChecked)
                                selectedCategories else emptyList()
                        )

                        // and call the onCreate handler
                        onCreateHandler(newGoalWithCategories)
                    }
                }
                dialog.dismiss()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                goalDialogTypeSwitchView.isChecked = false
                dialog.cancel()
            }
        }

        // finally, we use the alert dialog builder to create the alertDialog
        alertDialog = alertDialogBuilder.create()
    }

    private fun initCategorySelector(
        context: Activity,
        dao: PTDao,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        goalDialogTypeSwitchView.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                goalDialogCategorySelectorView.isEnabled = true
                goalDialogAllCategoriesView.typeface = Typeface.DEFAULT
                goalDialogSpecificCategoriesView.typeface = Typeface.DEFAULT_BOLD
            } else {
                goalDialogCategorySelectorView.isEnabled = false
                goalDialogAllCategoriesView.typeface = Typeface.DEFAULT_BOLD
                goalDialogSpecificCategoriesView.typeface = Typeface.DEFAULT
            }
        }

        lifecycleScope.launch {
            dao.getActiveCategories().let { categories ->
                ArrayAdapter(
                    context,
                    R.layout.view_goal_spinner_item,
                    categories.map { it.name }
                ).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    goalDialogCategorySelectorView.adapter = it
                }
                goalDialogCategorySelectorView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedCategories.clear()
                        selectedCategories.add(categories[position])
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedCategories.clear()
                    }
                }
            }
        }
    }

    private fun initTimeSelector(
        context: Activity
    ) {
        goalDialogTargetMinutesView.filters = arrayOf(InputFilterMax(59))

        ArrayAdapter(
            context,
            R.layout.view_goal_spinner_item,
            GoalPeriodUnit.values().map { unit ->
                when(unit) {
                    GoalPeriodUnit.DAY -> "days"
                    GoalPeriodUnit.WEEK -> "weeks"
                    GoalPeriodUnit.MONTH -> "months"
                }
            },
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            goalDialogPeriodUnitView.adapter = adapter
        }
    }


    // check if all fields in the dialog are filled out
    private fun isComplete(): Boolean {
        return (goalDialogTargetHoursView.text.toString().isNotEmpty() ||
                goalDialogTargetMinutesView.text.toString().isNotEmpty()) &&
            goalDialogPeriodValueView.text.toString().isNotEmpty()
    }

    // the public function to show the dialog
    // if a goal is passed it will be edited
    fun show(goal: Goal? = null) {
        alertDialog?.show()

        alertDialog?.also { dialog ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            if(goal != null) {
                goalDialogTitleView.setText(R.string.goalDialogTitleEdit)
                positiveButton.setText(R.string.goalDialogOkEdit)

                goalDialogTypeSwitchView.visibility = View.GONE
                goalDialogSpecificCategoriesView.visibility = View.GONE
                goalDialogPeriodValueView.visibility = View.GONE
                goalDialogPeriodUnitView.visibility = View.GONE
            }

            positiveButton.isEnabled = isComplete()
            goalDialogTargetHoursView.addTextChangedListener {
                positiveButton.isEnabled = isComplete()
            }
            goalDialogTargetMinutesView.addTextChangedListener {
                positiveButton.isEnabled = isComplete()
            }
            goalDialogPeriodValueView.addTextChangedListener {
                positiveButton.isEnabled = isComplete()
            }
        }
    }

    private class InputFilterMax(
        private var max: Int
    ) : InputFilter {
        init {
            assert(max >= 1) {
                Log.e("Assertion failed", "Maximum has to be larger than 0")
            }
        }

        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dstart: Int,
            dend: Int
        ): CharSequence {
            try {
                val input = (dest.slice(0 until dstart).toString()
                    + source.toString()
                    + dest.slice(dend until dest.length).toString()
                )
                if (input.toInt() in 0..max) return source
            } catch (nfe: NumberFormatException) { }
            return ""
        }
    }
}
