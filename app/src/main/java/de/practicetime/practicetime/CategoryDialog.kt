package de.practicetime.practicetime

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import de.practicetime.practicetime.entities.Category

class CategoryDialog (
    context: Activity,
    onCreateHandler: (newCategory: Category) -> Unit,
) {

    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater;
    private val dialogView = inflater.inflate(
        R.layout.dialog_view_add_or_change_category,
        null,
    )

    // find and save all the views in the dialog view
    private val categoryDialogTitleView =
        dialogView.findViewById<TextView>(R.id.categoryDialogTitle)
    private val categoryNameView =
        dialogView.findViewById<EditText>(R.id.addCategoryDialogName)
    private val categoryColorButtonGroupRow1 =
        dialogView.findViewById<RadioGroup>(R.id.addCategoryDialogColorRow1)
    private val categoryColorButtonGroupRow2 =
        dialogView.findViewById<RadioGroup>(R.id.addCategoryDialogColorRow2)

    private val categoryColorButtons = listOf<RadioButton>(
        dialogView.findViewById(R.id.addCategoryDialogColor1),
        dialogView.findViewById(R.id.addCategoryDialogColor2),
        dialogView.findViewById(R.id.addCategoryDialogColor3),
        dialogView.findViewById(R.id.addCategoryDialogColor4),
        dialogView.findViewById(R.id.addCategoryDialogColor5),
        dialogView.findViewById(R.id.addCategoryDialogColor6),
        dialogView.findViewById(R.id.addCategoryDialogColor7),
        dialogView.findViewById(R.id.addCategoryDialogColor8),
        dialogView.findViewById(R.id.addCategoryDialogColor9),
        dialogView.findViewById(R.id.addCategoryDialogColor10),
    )

    private var selectedCategoryId = 0
    private var selectedColorIndex = 0

    private var alertDialog: AlertDialog? = null

    init {
        // Dialog Setup
        alertDialogBuilder.apply {
            // pass the dialogView to the builder
            setView(dialogView)

            // define the callback function for the positive button
            setPositiveButton(R.string.addCategoryAlertOk) { dialog, _ ->
                val categoryName = categoryNameView.text.toString().trim()

                // check if all fields are filled out
                if (isComplete()) {

                    // create the new / edited category
                    val newCategory = Category(
                        id = selectedCategoryId,
                        name = categoryName,
                        colorIndex = selectedColorIndex,
                        archived = false,
                        profile_id = 0
                    )

                    // and call the onCreate handler
                    onCreateHandler(newCategory)
                }

                // clear the dialog and dismiss it
                selectedCategoryId = 0
                selectedColorIndex = 0
                categoryNameView.text.clear()
                categoryColorButtonGroupRow1.clearCheck()
                categoryColorButtonGroupRow2.clearCheck()
                dialog.dismiss()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                categoryNameView.text.clear()
                categoryColorButtonGroupRow1.clearCheck()
                categoryColorButtonGroupRow2.clearCheck()
                dialog.cancel()
            }
        }

        // fetch the colors for the categories from the resources
        val categoryColors =  context.resources.getIntArray(R.array.category_colors)

        // and apply them to the radio buttons as well as set the event listener
        // which ensures only one color is selected at a time
        categoryColorButtons.forEachIndexed { index, button ->
            button.backgroundTintList = ColorStateList.valueOf(categoryColors[index])
            button.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    selectedColorIndex = index
                    if (index < 5) {
                        categoryColorButtonGroupRow2.clearCheck()
                    } else {
                        categoryColorButtonGroupRow1.clearCheck()
                    }
                }
            }
        }

        // finally, we use the alert dialog builder to create the alertDialog
        alertDialog = alertDialogBuilder.create()
        alertDialog?.window?.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.dialog_background))
    }

    // the dialog is complete if a name is entered and a color is selected
    private fun isComplete(): Boolean {
        return categoryNameView?.text.toString().trim().isNotEmpty() &&
                (categoryColorButtonGroupRow1?.checkedRadioButtonId != -1 ||
                        categoryColorButtonGroupRow2?.checkedRadioButtonId != -1)
    }

    // the public function to show the dialog
    // if a category is passed it will be edited
    fun show(category: Category? = null) {
        alertDialog?.show()
        alertDialog?.also { dialog ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            if(category != null) {
                categoryDialogTitleView.setText(R.string.addCategoryDialogTitleEdit)
                positiveButton.setText(R.string.addCategoryAlertOkEdit)

                selectedCategoryId = category.id
                categoryNameView.setText(category.name)
                categoryColorButtons[category.colorIndex].isChecked = true
            }

            positiveButton.isEnabled = isComplete()
            categoryNameView.addTextChangedListener   {
                positiveButton.isEnabled = isComplete()
            }
            categoryColorButtonGroupRow1.setOnCheckedChangeListener { _, _ ->
                positiveButton.isEnabled = isComplete()
            }
            categoryColorButtonGroupRow2.setOnCheckedChangeListener { _, _ ->
                positiveButton.isEnabled = isComplete()
            }
        }
    }
}