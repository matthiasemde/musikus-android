package de.practicetime.practicetime

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import de.practicetime.practicetime.components.NumberInput
import de.practicetime.practicetime.entities.*
import java.util.*

class GoalDialog(
    private val context: Activity,
    private val categories: List<Category>,
    onCreateHandler: (newGoal: GoalDescriptionWithCategories, firstTarget: Int) -> Unit,
) {

    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater
    private val dialogView = inflater.inflate(
        R.layout.dialog_view_add_or_edit_goal,
        null,
    )

    // find and save all the views in the dialog view
    private val goalDialogTitleView = dialogView.findViewById<TextView>(R.id.categoryDialogTitle)
    private val goalDialogAllCategoriesButtonView = dialogView.findViewById<AppCompatButton>(R.id.goalDialogAllCategories)
    private val goalDialogSingleCategoryButtonView = dialogView.findViewById<AppCompatButton>(R.id.goalDialogSpecificCategories)
    private val goalDialogCategorySelectorView = dialogView.findViewById<Spinner>(R.id.goalDialogCategorySelector)
    private val goalDialogCategorySelectorLayoutView = dialogView.findViewById<LinearLayout>(R.id.goalDialogCategorySelectorLayout)
    private val goalDialogOneTimeGoalView = dialogView.findViewById<CheckBox>(R.id.goalDialogOneTimeGoal)
    private val goalDialogTargetHoursView = dialogView.findViewById<NumberInput>(R.id.goalDialogHours)
    private val goalDialogTargetMinutesView = dialogView.findViewById<NumberInput>(R.id.goalDialogMinutes)
    private val goalDialogPeriodValueView = dialogView.findViewById<EditText>(R.id.goalDialogPeriodValue)
    private val goalDialogPeriodUnitView = dialogView.findViewById<Spinner>(R.id.goalDialogPeriodUnit)

    private var trackAllCategories = true

    private val selectedCategories = ArrayList<Category>()
    private var selectedGoalDescriptionId = 0

    private var alertDialog: AlertDialog? = null

    init {
        initCategorySelector()

        initTimeSelector()

        // dialog Setup
        alertDialogBuilder.apply {
            // pass the dialogView to the builder
            setView(dialogView)

            // define the callback function for the positive button
            setPositiveButton(R.string.addCategoryAlertOk) { dialog, _ ->
                if(isComplete()) {
                    // first create the new description
                    val newGoalDescription = GoalDescription(
                        id = selectedGoalDescriptionId,
                        type =  if (trackAllCategories) GoalType.NON_SPECIFIC
                                    else GoalType.CATEGORY_SPECIFIC,
                        oneTime = !(goalDialogOneTimeGoalView.isChecked),
                        periodInPeriodUnits = goalDialogPeriodValueView.text.toString().toInt(),
                        periodUnit = GoalPeriodUnit.values()[goalDialogPeriodUnitView.selectedItemPosition],
                    )

                    Log.d("CREATE", "track: $trackAllCategories\nselectedCategories:$selectedCategories")
                    // then create a object joining the description with any selected categories
                    val newGoalDescriptionWithCategories = GoalDescriptionWithCategories(
                        description = newGoalDescription,
                        categories = if (!trackAllCategories)
                            selectedCategories.toList() else emptyList()
                    )

                    Log.d("CREATE", "$newGoalDescriptionWithCategories")

                    val targetDuration = goalDialogTargetHoursView.text.toString().trim().let{
                        if(it.isNotEmpty()) it.toInt() * 3600 else 0
                    } + goalDialogTargetMinutesView.text.toString().trim().let {
                        if(it.isNotEmpty()) it.toInt() * 60 else 0
                    }

                    // and call the onCreate handler, passing the selected target duration
                    onCreateHandler(newGoalDescriptionWithCategories, targetDuration)
                }

                // clear the dialog and dismiss it
                selectedGoalDescriptionId = 0

                goalDialogAllCategoriesButtonView.performClick()

                selectedCategories.clear()
                goalDialogCategorySelectorView.setSelection(0)

                goalDialogOneTimeGoalView.isChecked = true

                goalDialogTargetHoursView.setText("0")
                goalDialogTargetMinutesView.setText("0")
                goalDialogPeriodValueView.setText("1")

                goalDialogPeriodUnitView.setSelection(0)
                dialog.dismiss()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                dialog.cancel()
            }
        }

        // finally, we use the alert dialog builder to create the alertDialog
        alertDialog = alertDialogBuilder.create()
        alertDialog?.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(context, R.drawable.dialog_background)
        )
    }

    private fun initCategorySelector() {

        goalDialogAllCategoriesButtonView.isSelected = true

        goalDialogCategorySelectorLayoutView.alpha = 0.5f
        goalDialogCategorySelectorView.isEnabled = false

        goalDialogSingleCategoryButtonView.setOnClickListener {
            trackAllCategories = false

            goalDialogSingleCategoryButtonView.isSelected = true
            goalDialogAllCategoriesButtonView.isSelected = false

            goalDialogCategorySelectorLayoutView.alpha = 1f
            goalDialogCategorySelectorView.isEnabled = true
            updatePositiveButtonState()
        }

        goalDialogAllCategoriesButtonView.setOnClickListener {
            trackAllCategories = true

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
            goalDialogCategorySelectorView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCategories.clear()
                    if (position > 0)
                        selectedCategories.add(categories[position-1])  // -1 because pos=0 is hint
                    updatePositiveButtonState()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCategories.clear()
                }
            }
        }
    }

    private fun initTimeSelector() {
        ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
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

    // sets the enabled state of the add button depending on valid and complete inputs
    private fun updatePositiveButtonState() {
        alertDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = isComplete()
    }

    // check if all fields in the dialog are filled out
    private fun isComplete(): Boolean {
        val targetMinutes = goalDialogTargetMinutesView.text.toString().trim().let {
            if (it.isNotEmpty()) it.toInt() else 0
        }
        val targetHours = goalDialogTargetHoursView.text.toString().trim().let {
            if (it.isNotEmpty()) it.toInt() else 0
        }
        val periodValue = goalDialogPeriodValueView.text.toString().trim().let {
            if (it.isNotEmpty()) it.toInt() else 0
        }

        return (targetMinutes + targetHours > 0 && periodValue > 0) &&
            (selectedCategories.size > 0 || trackAllCategories)
    }

    // the public function to show the dialog
    // if a goal is passed it will be edited
    fun show(goalInstanceWithDescriptionWithCategories: GoalInstanceWithDescriptionWithCategories? = null) {
        alertDialog?.show()

        alertDialog?.also { dialog ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            if(goalInstanceWithDescriptionWithCategories != null) {
                selectedGoalDescriptionId =
                    goalInstanceWithDescriptionWithCategories.description.description.id
                goalDialogTitleView.setText(R.string.goalDialogTitleEdit)
                positiveButton.setText(R.string.goalDialogOkEdit)

                val hours = goalInstanceWithDescriptionWithCategories.instance.target / 3600
                val minutes = goalInstanceWithDescriptionWithCategories.instance.target % 3600 / 60

                goalDialogTargetHoursView.setText(hours.toString())
                goalDialogTargetMinutesView.setText(minutes.toString())

                // we need this, to make the positive button clickable - value is ignored
                trackAllCategories = true

                dialogView.findViewById<View>(R.id.goalDialogNotTarget).visibility = View.GONE
            } else {
                goalDialogTargetHoursView.setText("0")
                goalDialogTargetMinutesView.setText("0")
            }

            updatePositiveButtonState()
            goalDialogTargetHoursView.addTextChangedListener {
                updatePositiveButtonState()
            }
            goalDialogTargetMinutesView.addTextChangedListener {
                updatePositiveButtonState()
            }
            goalDialogPeriodValueView.addTextChangedListener {
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
                view = inflater.inflate(R.layout.view_goal_spinner_item, parent, false)
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
