/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package app.musikus.ui.library

import android.app.Activity
import android.content.res.ColorStateList
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import app.musikus.Musikus
import app.musikus.R
import app.musikus.database.entities.LibraryItem

class LibraryItemDialog (
    context: Activity,
    submitHandler: (libraryItem: LibraryItem) -> Unit,
) {

    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater
    private val dialogView = inflater.inflate(
        R.layout.dialog_add_or_change_library_item,
        null,
    )

    // find and save all the views in the dialog view
    private val libraryItemDialogTitleView =
        dialogView.findViewById<TextView>(R.id.libraryItemDialogTitle)
    private val libraryItemNameView =
        dialogView.findViewById<EditText>(R.id.addLibraryItemDialogName)
    private val libraryItemColorButtonGroupRow1 =
        dialogView.findViewById<RadioGroup>(R.id.addLibraryItemDialogColorRow1)
    private val libraryItemColorButtonGroupRow2 =
        dialogView.findViewById<RadioGroup>(R.id.addLibraryItemDialogColorRow2)

    private val libraryItemColorButtons = listOf<RadioButton>(
        dialogView.findViewById(R.id.addLibraryItemDialogColor1),
        dialogView.findViewById(R.id.addLibraryItemDialogColor2),
        dialogView.findViewById(R.id.addLibraryItemDialogColor3),
        dialogView.findViewById(R.id.addLibraryItemDialogColor4),
        dialogView.findViewById(R.id.addLibraryItemDialogColor5),
        dialogView.findViewById(R.id.addLibraryItemDialogColor6),
        dialogView.findViewById(R.id.addLibraryItemDialogColor7),
        dialogView.findViewById(R.id.addLibraryItemDialogColor8),
        dialogView.findViewById(R.id.addLibraryItemDialogColor9),
        dialogView.findViewById(R.id.addLibraryItemDialogColor10),
    )

    private var libraryItem: LibraryItem? = null

    private var selectedName = ""
    private var selectedColorIndex = -1

    private var alertDialog: AlertDialog

    init {
        // Dialog Setup
        alertDialogBuilder.apply {
            // pass the dialogView to the builder
            setView(dialogView)

            // define the callback function for the positive button
            setPositiveButton(R.string.addLibraryItemAlertOk) { dialog, _ ->

                // check if all fields are filled out
                if (isComplete()) {
                    // create the edited / new libraryItem
                    (libraryItem?.apply {
                        name = selectedName
                        colorIndex = selectedColorIndex
                    } ?: LibraryItem(
                        name = selectedName,
                        colorIndex = selectedColorIndex,
                    // and call the submit handler
                    )).let {
                        submitHandler(it)
                    }
                }

                // clear the dialog and dismiss it
                resetDialog()
                dialog.dismiss()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.addLibraryItemAlertCancel) { dialog, _ ->
                resetDialog()
                dialog.cancel()
            }
        }

        // fetch the colors for the libraryItems from the resources
        val libraryItemColors =  context.resources.getIntArray(R.array.library_item_colors)

        // and apply them to the radio buttons as well as set the event listener
        // which ensures only one color is selected at a time
        libraryItemColorButtons.forEachIndexed { index, button ->
            button.backgroundTintList = ColorStateList.valueOf(libraryItemColors[index])
            button.buttonTintList = ColorStateList.valueOf(Musikus.getThemeColor(R.attr.colorOnPrimary, context))
            button.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked) {
                    selectedColorIndex = index
                    if (index < 5) {
                        libraryItemColorButtonGroupRow2.clearCheck()
                    } else {
                        libraryItemColorButtonGroupRow1.clearCheck()
                    }
                }
            }
        }

        // finally, we use the alert dialog builder to create the alertDialog
        alertDialog = alertDialogBuilder.create()
        alertDialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(context,
            R.drawable.dialog_background))
    }

    private fun resetDialog() {
        libraryItemNameView.text.clear()
        libraryItemColorButtonGroupRow1.clearCheck()
        libraryItemColorButtonGroupRow2.clearCheck()
        libraryItem = null
        selectedName = ""
        selectedColorIndex = -1
    }

    // the dialog is complete if a name is entered and a color is selected
    private fun isComplete(): Boolean {
        return selectedName.isNotEmpty() && selectedColorIndex != -1
    }

    // the public function to show the dialog
    // if a libraryItem is passed it will be edited
    fun show(editLibraryItem: LibraryItem? = null) {

        alertDialog.show()
        alertDialog.also { dialog ->
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            // if a libraryItem is passed, apply its properties to the dialog
            editLibraryItem?.let {
                libraryItemDialogTitleView.setText(R.string.addLibraryItemDialogTitleEdit)
                positiveButton.setText(R.string.addLibraryItemAlertOkEdit)
                libraryItem = it
                libraryItemNameView.setText(it.name)
                selectedName = it.name
                libraryItemColorButtons[it.colorIndex].isChecked = true
            }

            positiveButton.isEnabled = isComplete()
            libraryItemNameView.addTextChangedListener {
                selectedName = libraryItemNameView.text.toString().trim()
                positiveButton.isEnabled = isComplete()
            }
            libraryItemColorButtonGroupRow1.setOnCheckedChangeListener { _, _ ->
                positiveButton.isEnabled = isComplete()
            }
            libraryItemColorButtonGroupRow2.setOnCheckedChangeListener { _, _ ->
                positiveButton.isEnabled = isComplete()
            }
        }
    }
}
