package de.practicetime.practicetime

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.text.InputFilter
import android.text.Spanned
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.practicetime.practicetime.entities.*
import kotlinx.coroutines.launch
import java.util.*

class GoalDialog(
    context: Activity,
    dao: PTDao,
    lifecycleScope: LifecycleCoroutineScope,
    onCreateHandler: (newGoal: GoalDescriptionWithCategories, firstTarget: Int) -> Unit,
) {

    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = MaterialAlertDialogBuilder(context)
    private val inflater = context.layoutInflater;
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
    private val goalDialogTargetHoursView = dialogView.findViewById<EditText>(R.id.goalDialogHours)
    private val goalDialogTargetMinutesView = dialogView.findViewById<EditText>(R.id.goalDialogMinutes)
    private val goalDialogPeriodValueView = dialogView.findViewById<EditText>(R.id.goalDialogPeriodValue)
    private val goalDialogPeriodUnitView = dialogView.findViewById<Spinner>(R.id.goalDialogPeriodUnit)

    private var trackAllCategories = true

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
                    // first create the new description
                    val newGoalDescription = GoalDescription(
                        type =  if (trackAllCategories) GoalType.NON_SPECIFIC
                                    else GoalType.CATEGORY_SPECIFIC,
                        oneTime = !(goalDialogOneTimeGoalView.isChecked),
                        periodInPeriodUnits = goalDialogPeriodValueView.text.toString().toInt(),
                        periodUnit = GoalPeriodUnit.values()[goalDialogPeriodUnitView.selectedItemPosition],
                    )

                    // then create a object joining the description with any selected categories
                    val newGoalDescriptionWithCategories = GoalDescriptionWithCategories(
                        description = newGoalDescription,
                        categories = if (!trackAllCategories)
                            selectedCategories else emptyList()
                    )

                    val targetDuration = goalDialogTargetHoursView.text.toString().trim().let{
                        if(it.isNotEmpty()) it.toInt() * 3600 else 0
                    } + goalDialogTargetMinutesView.text.toString().trim().let {
                        if(it.isNotEmpty()) it.toInt() * 60 else 0
                    }

                    // and call the onCreate handler, passing the selected target duration
                    onCreateHandler(newGoalDescriptionWithCategories, targetDuration)
                }
                dialog.dismiss()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                trackAllCategories = false
                dialog.cancel()
            }
        }

        // finally, we use the alert dialog builder to create the alertDialog
        alertDialog = alertDialogBuilder.create()
        alertDialog?.window?.setBackgroundDrawable(
            ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.dialog_background,
                context.theme
            )
        )
    }

    private fun showKeyboard(context: Activity, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    private fun hideKeyboard(context: Activity) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(context.window.decorView.rootView.windowToken, 0)
    }


    private fun initCategorySelector(
        context: Activity,
        dao: PTDao,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
        val colorPrimary = ColorStateList.valueOf(typedValue.data)

        context.theme.resolveAttribute(R.attr.colorOnPrimary, typedValue, true)
        val colorOnPrimary = ColorStateList.valueOf(typedValue.data)

        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
        val colorOnSurfaceWithAlpha = ColorStateList.valueOf(typedValue.data).withAlpha(30)

        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
        val colorOnSurface = ColorStateList.valueOf(typedValue.data)

        goalDialogSingleCategoryButtonView.apply {
            backgroundTintList = colorOnSurfaceWithAlpha
            setTextColor(colorOnSurface)
        }

        goalDialogCategorySelectorLayoutView.alpha = 0.5f
        goalDialogCategorySelectorView.isEnabled = false

        goalDialogSingleCategoryButtonView.setOnClickListener {
            trackAllCategories = false
            goalDialogAllCategoriesButtonView.apply {
                backgroundTintList = colorOnSurfaceWithAlpha
                setTextColor(colorOnSurface)
            }
            goalDialogSingleCategoryButtonView.apply {
                backgroundTintList = colorPrimary
                setTextColor(colorOnPrimary)
            }
            goalDialogCategorySelectorLayoutView.alpha = 1f
            goalDialogCategorySelectorView.isEnabled = true
            updatePositiveButtonState()
        }

        goalDialogAllCategoriesButtonView.setOnClickListener {
            trackAllCategories = true
            goalDialogAllCategoriesButtonView.apply {
                backgroundTintList = colorPrimary
                setTextColor(colorOnPrimary)
            }
            goalDialogSingleCategoryButtonView.apply {
                backgroundTintList = colorOnSurfaceWithAlpha
                setTextColor(colorOnSurface)
            }
            goalDialogCategorySelectorLayoutView.alpha = 0.5f
            goalDialogCategorySelectorView.isEnabled = false
            updatePositiveButtonState()
        }

        lifecycleScope.launch {
            dao.getActiveCategories().let { categories ->
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
        }
    }

    private fun initTimeSelector(
        context: Activity
    ) {
        goalDialogTargetMinutesView.filters = arrayOf(InputFilterMax(59))

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

    // sets the enabled state of the add button dependig on valid and complete inputs
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

        if(selectedCategories.size == 0 && !trackAllCategories)
            return false

        return (targetMinutes + targetHours > 0 && periodValue > 0)
    }

    // the public function to show the dialog
    // if a goal is passed it will be edited
    fun show(goalDescription: GoalDescription? = null) {
        alertDialog?.show()

        alertDialog?.also { dialog ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            if(goalDescription != null) {
                goalDialogTitleView.setText(R.string.goalDialogTitleEdit)
                positiveButton.setText(R.string.goalDialogOkEdit)

                goalDialogAllCategoriesButtonView.visibility = View.GONE
                goalDialogSingleCategoryButtonView.visibility = View.GONE
                goalDialogPeriodValueView.visibility = View.GONE
                goalDialogPeriodUnitView.visibility = View.GONE
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

    private class CategoryDropDownAdapter(
        private val context: Context,
        private val categories: List<Category>
    ) : BaseAdapter() {

        private val inflater: LayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
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

        override fun getItem(position: Int): Any? {
            return categories[position];
        }

        override fun getCount(): Int {
            return categories.size;
        }

        override fun getItemId(position: Int): Long {
            return position.toLong();
        }

        private class ItemHolder(row: View?) {
            val color = row?.findViewById<ImageView>(R.id.categorySpinnerColor)
            val name = row?.findViewById<TextView>(R.id.categorySpinnerName)
        }
    }
}
