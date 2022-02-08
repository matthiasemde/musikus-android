package de.practicetime.practicetime.ui.goals

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.R
import de.practicetime.practicetime.components.NumberInput
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.utils.secondsDurationToHoursMinSec
import java.util.*
import kotlin.collections.ArrayList

class GoalDialog(
    private val context: Activity,
    private val categories: List<Category>,
    submitHandler: (
        newGoalDescriptionWithCategories: GoalDescriptionWithCategories,
        newTarget: Int
    ) -> Unit,
) {

    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater
    private val dialogView = inflater.inflate(
        R.layout.dialog_add_or_edit_goal,
        null,
    )

    // find and save all the views in the dialog view
    private val goalDialogAllCategoriesButtonView = dialogView.findViewById<AppCompatButton>(R.id.goalDialogAllCategories)
    private val goalDialogSingleCategoryButtonView = dialogView.findViewById<AppCompatButton>(R.id.goalDialogSpecificCategories)
    private val goalDialogCategorySelectorView = dialogView.findViewById<Spinner>(R.id.goalDialogCategorySelector)
    private val goalDialogCategorySelectorLayoutView = dialogView.findViewById<LinearLayout>(R.id.goalDialogCategorySelectorLayout)
    private val goalDialogOneTimeGoalView = dialogView.findViewById<CheckBox>(R.id.goalDialogOneTimeGoal)
    private val goalDialogTargetHoursView = dialogView.findViewById<NumberInput>(R.id.goalDialogHours)
    private val goalDialogTargetMinutesView = dialogView.findViewById<NumberInput>(R.id.goalDialogMinutes)
    private val goalDialogPeriodValueView = dialogView.findViewById<NumberInput>(R.id.goalDialogPeriodValue)
    private val goalDialogPeriodUnitView = dialogView.findViewById<Spinner>(R.id.goalDialogPeriodUnit)

    private var selectedTarget = 0
    private var selectedPeriodUnit = GoalPeriodUnit.DAY
    private var selectedPeriod = 1
    private var selectedGoalType = GoalType.NON_SPECIFIC
    private val selectedCategories = ArrayList<Category>()

    private var alertDialog: AlertDialog

    init {
        initCategorySelector()

        initTimeSelector()

        // dialog Setup
        alertDialogBuilder.apply {
            // pass the dialogView to the builder
            setView(dialogView)

            // define the callback function for the positive button
            setPositiveButton(R.string.addCategoryAlertOk) { dialog, _ ->
                // first create the new description
                val newGoalDescription = GoalDescription(
                    type =  selectedGoalType,
                    repeat = goalDialogOneTimeGoalView.isChecked,
                    periodInPeriodUnits = selectedPeriod,
                    periodUnit = selectedPeriodUnit,
                )

                // then create a object joining the description with any selected categories
                val newGoalDescriptionWithCategories = GoalDescriptionWithCategories(
                    description = newGoalDescription,
                    categories = if (selectedGoalType == GoalType.CATEGORY_SPECIFIC)
                        selectedCategories.toList() else emptyList()
                )

                // and call the submit handler, passing the selected target duration
                submitHandler(newGoalDescriptionWithCategories, selectedTarget)

                dialog.dismiss()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                dialog.cancel()
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.goalDialogOneTimeGoalTooltip)
            .setOnClickListener {
            it.performLongClick()
        }
        dialogView.findViewById<MaterialButton>(R.id.goalDialogPeriodUnitTooltip)
            .setOnClickListener {
            it.performLongClick()
        }

        // finally, we use the alert dialog builder to create the alertDialog
        alertDialog = alertDialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(context, R.drawable.dialog_background)
        )
    }

    private fun resetDialog() {
        // clear the dialog and dismiss it
        goalDialogTargetHoursView.setText("0")
        goalDialogTargetMinutesView.setText("0")
        goalDialogPeriodValueView.setText("1")

        goalDialogPeriodUnitView.setSelection(0)

        goalDialogAllCategoriesButtonView.performClick()

        goalDialogCategorySelectorView.setSelection(0)

        goalDialogOneTimeGoalView.isChecked = true

        updatePositiveButtonState()
    }

    private fun initCategorySelector() {

        goalDialogAllCategoriesButtonView.isSelected = true

        goalDialogCategorySelectorLayoutView.alpha = 0.5f
        goalDialogCategorySelectorView.isEnabled = false

        goalDialogSingleCategoryButtonView.setOnClickListener {
            selectedGoalType = GoalType.CATEGORY_SPECIFIC

            goalDialogSingleCategoryButtonView.isSelected = true
            goalDialogAllCategoriesButtonView.isSelected = false

            goalDialogCategorySelectorLayoutView.alpha = 1f
            goalDialogCategorySelectorView.isEnabled = true
            updatePositiveButtonState()
        }

        goalDialogAllCategoriesButtonView.setOnClickListener {
            selectedGoalType = GoalType.NON_SPECIFIC

            goalDialogSingleCategoryButtonView.isSelected = false
            goalDialogAllCategoriesButtonView.isSelected = true

            goalDialogCategorySelectorLayoutView.alpha = 0.5f
            goalDialogCategorySelectorView.isEnabled = false
            updatePositiveButtonState()
        }

        goalDialogCategorySelectorView.apply {
            adapter = CategoryDropDownAdapter(
                context,
                categories,
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedCategories.clear()
                    if (pos > 0)
                        selectedCategories.add(categories[pos-1])  // -1 because pos=0 is hint
                    updatePositiveButtonState()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCategories.clear()
                }
            }
        }
    }

    private fun initTimeSelector() {
        goalDialogPeriodUnitView.apply {
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                GoalPeriodUnit.values().map { unit ->
                    when(unit) {
                        GoalPeriodUnit.DAY -> "days"
                        GoalPeriodUnit.WEEK -> "weeks"
                        GoalPeriodUnit.MONTH -> "months"
                    }
                }
            ).let {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                it
            }
            onItemSelectedListener  = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedPeriodUnit = GoalPeriodUnit.values()[pos]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    // sets the enabled state of the add button depending on valid and complete inputs
    private fun updatePositiveButtonState() {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled =
            (selectedTarget > 0 && selectedPeriod > 0) &&
            (selectedCategories.size > 0 || selectedGoalType == GoalType.NON_SPECIFIC)
    }

    // the public function to show the dialog
    fun show() {
        resetDialog()

        alertDialog.show()
        alertDialog.also { _ ->
            goalDialogTargetHoursView.addTextChangedListener {
                selectedTarget = (goalDialogTargetHoursView.value() ?: 0) * 3600 +
                (goalDialogTargetMinutesView.value() ?: 0) * 60
                updatePositiveButtonState()
            }
            goalDialogTargetMinutesView.addTextChangedListener {
                selectedTarget = (goalDialogTargetHoursView.value() ?: 0) * 3600 +
                (goalDialogTargetMinutesView.value() ?: 0) * 60
                updatePositiveButtonState()
            }
            goalDialogPeriodValueView.addTextChangedListener {
                selectedPeriod = goalDialogPeriodValueView.value() ?: 0
                updatePositiveButtonState()
            }
        }
    }

    private class CategoryDropDownAdapter(
        private val context: Context,
        private val categories: List<Category>
    ) : BaseAdapter() {

        private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val vh: ItemHolder
            if (convertView == null) {
                view = inflater.inflate(R.layout.listitem_goal_dropdown, parent, false)
                vh = ItemHolder(view)
                view?.tag = vh
            } else {
                view = convertView
                vh = view.tag as ItemHolder
            }
            if (position != 0) {
                vh.color?.visibility  = View.VISIBLE
                vh.name?.text = categories[position - 1].name
                // set the color to the category color
                val categoryColors = context.resources.getIntArray(R.array.category_colors)
                vh.color?.backgroundTintList = ColorStateList.valueOf(
                    categoryColors[categories[position - 1].colorIndex]
                )
            } else {
                vh.color?.visibility  = View.GONE
                vh.name?.text = context.resources.getString(R.string.goalDialogCatSelectHint)
            }

            return view
        }

        override fun getItem(position: Int) = categories[position]

        override fun getCount() = categories.size + 1

        override fun getItemId(position: Int) = position.toLong()

        private class ItemHolder(row: View?) {
            val color = row?.findViewById<ImageView>(R.id.categorySpinnerColor)
            val name = row?.findViewById<TextView>(R.id.categorySpinnerName)
        }
    }
}
