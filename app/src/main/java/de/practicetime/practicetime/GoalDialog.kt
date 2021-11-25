package de.practicetime.practicetime

import android.app.Activity
import android.graphics.Typeface
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import android.text.Spanned

import android.text.InputFilter
import androidx.core.widget.addTextChangedListener
import de.practicetime.practicetime.entities.*
import java.lang.NumberFormatException


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
//                if(isComplete()) {
//                    val newGoalWithCategories = GoalWithCategories(
//                        goal = Goal(
//                            type = GoalType.SPECIFIC_CATEGORIES
//                        ),
//                        categories = listOf()
//                    )
//
//                    // and call the onCreate handler
//                    onCreateHandler(newGoalWithCategories)
//                }
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
        // fetch the colors for the categories from the resources
        val categoryColors =  context.resources.getIntArray(R.array.category_colors)

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
            }
        }
    }

    private fun initTimeSelector(
        context: Activity
    ) {
        goalDialogTargetMinutesView.filters = arrayOf(InputFilterMax(60))

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
        return goalDialogTargetHoursView.text.toString().isNotEmpty() &&
            goalDialogTargetMinutesView.text.toString().isNotEmpty() &&
            goalDialogPeriodValueView.text.toString().isNotEmpty()
    }

    // the public function to show the dialog
    // if a goal is passed it will be edited
    fun show(goal: Goal? = null) {
        alertDialog?.show()

        alertDialog?.also { dialog ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = isComplete()
            goalDialogTargetHoursView.addTextChangedListener {
                positiveButton?.isEnabled = isComplete()
            }
            goalDialogTargetMinutesView.addTextChangedListener {
                positiveButton?.isEnabled = isComplete()
            }
            goalDialogPeriodValueView.addTextChangedListener {
                positiveButton?.isEnabled = isComplete()
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
                var input = (dest.slice(0 until dstart).toString()
                    + source.toString()
                    + dest.slice(dend until dest.length).toString()
                )
                if (input.toInt() in 1..max) return source
            } catch (nfe: NumberFormatException) { }
            return ""
        }
    }
}
