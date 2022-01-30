package de.practicetime.practicetime.shared

import android.app.Activity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import de.practicetime.practicetime.R
import de.practicetime.practicetime.components.NumberInput
import de.practicetime.practicetime.utils.secondsDurationToHoursMinSec

class EditTimeDialog (
    context: Activity,
    title: String,
    onEditHandler: (newTime: Int) -> Unit,
) {
    // instantiate the builder for the alert dialog
    private val alertDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater
    private val dialogView = inflater.inflate(
        R.layout.dialog_edit_time,
        null,
    )

    private var dialog: AlertDialog

    // find and save all the views in the dialog view
    private val editHoursView = dialogView.findViewById<NumberInput>(R.id.time_dialog_hours)
    private val editMinutesView = dialogView.findViewById<NumberInput>(R.id.time_dialog_minutes)

    init {
        // Dialog Setup
        alertDialogBuilder.apply {
            setView(dialogView)

            dialogView.findViewById<TextView>(R.id.time_dialog_title).text = title

            setPositiveButton(R.string.editDialogOk) { dialog, _ ->
                if (isComplete()) {
                    onEditHandler(
                        editHoursView.text.toString().toInt() * 3600 +
                                editMinutesView.text.toString().toInt() * 60
                    )
                }
                editHoursView.clearFocus()
                editMinutesView.clearFocus()
            }

            // define the callback function for the negative button
            // to clear the dialog and then cancel it
            setNegativeButton(R.string.editDialogCancel) { dialog, _ ->
                editHoursView.clearFocus()
                editMinutesView.clearFocus()
                dialog.cancel()
            }
        }

        dialog = alertDialogBuilder.create()
        dialog.window?.setBackgroundDrawable(context.getDrawable(R.drawable.dialog_background))
    }

    fun show(initTime: Int) {
        val (initHours, initMinutes, _) = secondsDurationToHoursMinSec(initTime)

        dialog.show()
        dialog.also {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            editHoursView.setText(initHours.toString())
            editMinutesView.setText(initMinutes.toString())

            positiveButton.isEnabled = isComplete()

            editHoursView.addTextChangedListener {
                positiveButton.isEnabled = isComplete()
            }
            editMinutesView.addTextChangedListener {
                positiveButton.isEnabled = isComplete()
            }
        }
    }

    private fun isComplete(): Boolean {
        val minutes = editMinutesView.text.toString().trim().let {
            if (it.isNotEmpty()) it.toInt() else 0
        }
        val hours = editHoursView.text.toString().trim().let {
            if (it.isNotEmpty()) it.toInt() else 0
        }

        return (minutes + hours > 0)
    }
}
