/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2022 Matthias Emde
 *
 * Parts of this software are licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.ui.goals

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.material.button.MaterialButton
import de.practicetime.practicetime.R
import de.practicetime.practicetime.components.NumberInput
import de.practicetime.practicetime.database.GoalDescriptionWithLibraryItems
import de.practicetime.practicetime.database.entities.*
import de.practicetime.practicetime.shared.*
import de.practicetime.practicetime.viewmodel.GoalDialogData
import java.util.*

@SuppressLint("ViewConstructor")
class GoalDialog(
    context: Context,
    private val libraryItems: List<LibraryItem>,
    private val repeat: Boolean = false,
    submitHandler: (
        newGoalDescriptionWithLibraryItems: GoalDescriptionWithLibraryItems,
        newTarget: Int
    ) -> Unit,
    onDismissRequest: () -> Unit
): LinearLayout(context) {

    // instantiate the builder for the alert dialog
    private val dialogView: View = LayoutInflater.from(context).inflate(
        R.layout.dialog_add_or_edit_goal,
        null,
    )

    // find and save all the views in the dialog view
    private val goalDialogAllLibraryItemsButtonView = dialogView.findViewById<AppCompatButton>(R.id.goalDialogAllLibraryItems)
    private val goalDialogSingleLibraryItemButtonView = dialogView.findViewById<AppCompatButton>(R.id.goalDialogSpecificLibraryItems)
    private val goalDialogLibraryItemSelectorView = dialogView.findViewById<Spinner>(R.id.goalDialogLibraryItemSelector)
    private val goalDialogLibraryItemSelectorLayoutView = dialogView.findViewById<LinearLayout>(R.id.goalDialogLibraryItemSelectorLayout)
    private val goalDialogTargetView = dialogView.findViewById<ComposeView>(R.id.goalDialogTarget)
    private val goalDialogPeriodValueView = dialogView.findViewById<ComposeView>(R.id.goalDialogPeriodValue)
    private val goalDialogPeriodUnitView = dialogView.findViewById<Spinner>(R.id.goalDialogPeriodUnit)
    private val goalDialogCancelView = dialogView.findViewById<MaterialButton>(R.id.goalDialogCancel)
    private val goalDialogCreateView = dialogView.findViewById<MaterialButton>(R.id.goalDialogCreate)

    private var selectedTarget = 0
    private var selectedPeriodUnit = GoalPeriodUnit.DAY
    private var selectedPeriod = 1
    private var selectedGoalType = GoalType.NON_SPECIFIC
    private val selectedLibraryItems = ArrayList<LibraryItem>()

    init {
        initLibraryItemSelector()

        initTimeSelector()

        // define the callback function for the positive button
        goalDialogCreateView.setOnClickListener {
            // first create the new description
            val newGoalDescription = GoalDescription(
                type =  selectedGoalType,
                repeat = repeat,
                periodInPeriodUnits = selectedPeriod,
                periodUnit = selectedPeriodUnit,
            )

            // then create a object joining the description with any selected libraryItems
            val newGoalDescriptionWithLibraryItems = GoalDescriptionWithLibraryItems(
                description = newGoalDescription,
                libraryItems = if (selectedGoalType == GoalType.ITEM_SPECIFIC)
                    selectedLibraryItems.toList() else emptyList()
            )

            // and call the submit handler, passing the selected target duration
            submitHandler(newGoalDescriptionWithLibraryItems, selectedTarget)
        }

        // define the callback function for the negative button
        // to clear the dialog and then cancel it
        goalDialogCancelView.setOnClickListener {
            onDismissRequest()
        }

        dialogView.findViewById<MaterialButton>(R.id.goalDialogPeriodUnitTooltip)
            .setOnClickListener {
            it.performLongClick()
        }

        goalDialogTargetView.setContent {
            var minutes by remember { mutableStateOf("00") } // remember here to act like view
            var hours by remember { mutableStateOf("00") }
            Row {
                NumberInput(
                    value = hours,
                    onValueChange = {
                        hours = it
                        selectedTarget =
                            (hours.toIntOrNull() ?: 0) * 3600 +
                            (minutes.toIntOrNull() ?: 0) * 60
                        updatePositiveButtonState()
                    },
                    showLeadingZero = true,
                    textSize = 40.sp,
                    maxValue = 99,
                    imeAction = ImeAction.Next,
                    label = { modifier ->
                        Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                NumberInput(
                    value = minutes,
                    onValueChange = {
                        minutes = it
                        selectedTarget =
                            (hours.toIntOrNull() ?: 0) * 3600 +
                            (minutes.toIntOrNull() ?: 0) * 60
                        updatePositiveButtonState()
                    },
                    showLeadingZero = true,
                    textSize = 40.sp,
                    maxValue = 59,
                    imeAction = ImeAction.Done,
                    label = { modifier ->
                        Text(modifier = modifier, text = "m", style = MaterialTheme.typography.labelLarge)
                    }
                )
            }
        }

        goalDialogPeriodValueView.setContent {
            val defaultValue = 1
            var period by remember { mutableStateOf("") }
            NumberInput(
                modifier = Modifier.padding(horizontal = 16.dp),
                value = period,
                onValueChange = {
                    period = it
                    selectedPeriod = period.toIntOrNull() ?: defaultValue
                    updatePositiveButtonState()
                },
                textSize = 20.sp,
                minValue = 0,
                maxValue = 99,
                imeAction = ImeAction.Done,
                placeHolder = defaultValue.toString(),
                underlined = true,
            )
        }

        super.addView(dialogView)
    }

    fun update() {

    }

    private fun initLibraryItemSelector() {

        goalDialogAllLibraryItemsButtonView.isSelected = true

        goalDialogLibraryItemSelectorLayoutView.alpha = 0.5f
        goalDialogLibraryItemSelectorView.isEnabled = false

        goalDialogSingleLibraryItemButtonView.setOnClickListener {
            selectedGoalType = GoalType.ITEM_SPECIFIC

            goalDialogSingleLibraryItemButtonView.isSelected = true
            goalDialogAllLibraryItemsButtonView.isSelected = false

            goalDialogLibraryItemSelectorLayoutView.alpha = 1f
            goalDialogLibraryItemSelectorView.isEnabled = true
            updatePositiveButtonState()
        }

        goalDialogAllLibraryItemsButtonView.setOnClickListener {
            selectedGoalType = GoalType.NON_SPECIFIC

            goalDialogSingleLibraryItemButtonView.isSelected = false
            goalDialogAllLibraryItemsButtonView.isSelected = true

            goalDialogLibraryItemSelectorLayoutView.alpha = 0.5f
            goalDialogLibraryItemSelectorView.isEnabled = false
            updatePositiveButtonState()
        }

        goalDialogLibraryItemSelectorView.apply {
            adapter = LibraryItemDropDownAdapter(
                context,
                libraryItems,
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedLibraryItems.clear()
                    if (pos > 0)
                        selectedLibraryItems.add(libraryItems[pos-1])  // -1 because pos=0 is hint
                    updatePositiveButtonState()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedLibraryItems.clear()
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
        goalDialogCreateView.isEnabled =
            (selectedTarget > 0 && selectedPeriod > 0) &&
            (selectedLibraryItems.size > 0 || selectedGoalType == GoalType.NON_SPECIFIC)
    }

    private class LibraryItemDropDownAdapter(
        private val context: Context,
        private val libraryItems: List<LibraryItem>
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
                vh.name?.text = libraryItems[position - 1].name
                // set the color to the libraryItem color
                val libraryItemColors = context.resources.getIntArray(R.array.library_item_colors)
                vh.color?.backgroundTintList = ColorStateList.valueOf(
                    libraryItemColors[libraryItems[position - 1].colorIndex]
                )
            } else {
                vh.color?.visibility  = View.GONE
                vh.name?.text = context.resources.getString(R.string.goalDialogCatSelectHint)
            }

            return view
        }

        override fun getItem(position: Int) = libraryItems[position]

        override fun getCount() = libraryItems.size + 1

        override fun getItemId(position: Int) = position.toLong()

        private class ItemHolder(row: View?) {
            val color = row?.findViewById<ImageView>(R.id.libraryItemSpinnerColor)
            val name = row?.findViewById<TextView>(R.id.libraryItemSpinnerName)
        }
    }
}

@Composable
fun TimeInput(
    value: Int,
    onValueChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        var hours = (value / 3600).toString().padStart(2, '0')
        var minutes = ((value % 3600) / 60).toString().padStart(2, '0')

        NumberInput(
            value = hours,
            onValueChange = {
                hours = it
                onValueChanged((hours.toIntOrNull() ?: 0) * 3600 +
                        (minutes.toIntOrNull() ?: 0) * 60
                )
            },
            showLeadingZero = true,
            textSize = 40.sp,
            maxValue = 99,
            imeAction = ImeAction.Next,
            label = { modifier ->
                Text(modifier = modifier, text = "h", style = MaterialTheme.typography.labelLarge)
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            value = minutes,
            onValueChange = {
                minutes = it
                onValueChanged((hours.toIntOrNull() ?: 0) * 3600 +
                        (minutes.toIntOrNull() ?: 0) * 60
                )
            },
            showLeadingZero = true,
            textSize = 40.sp,
            maxValue = 59,
            imeAction = ImeAction.Done,
            label = { modifier ->
                Text(modifier = modifier, text = "m", style = MaterialTheme.typography.labelLarge)
            }
        )
    }
}

@Composable
fun PeriodInput(
    periodInPeriodUnits: Int,
    periodUnit: GoalPeriodUnit,
    periodUnitSelectorExpanded: Boolean,
    onPeriodChanged: (Int) -> Unit,
    onPeriodUnitChanged: (GoalPeriodUnit) -> Unit,
    onPeriodUnitSelectorExpandedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "in")
        Spacer(modifier = Modifier.width(8.dp))
        NumberInput(
            value = periodInPeriodUnits.toString(),
            onValueChange = { onPeriodChanged(it.toIntOrNull() ?: 0) },
            textSize = 20.sp,
            maxValue = 99,
            imeAction = ImeAction.Next,
        )
        Spacer(modifier = Modifier.width(8.dp))
        SelectionSpinner(
            modifier = Modifier.width(130.dp),
            expanded = periodUnitSelectorExpanded,
            options = GoalPeriodUnit.values().map { IntSelectionSpinnerOption(it.ordinal, GoalPeriodUnit.toString(it)) },
            selected = IntSelectionSpinnerOption(periodUnit.ordinal, GoalPeriodUnit.toString(periodUnit)),
            onExpandedChange = onPeriodUnitSelectorExpandedChanged,
            onSelectedChange = {selection ->
                onPeriodUnitChanged(GoalPeriodUnit.values()[(selection as IntSelectionSpinnerOption?)?.id ?: 0])
                onPeriodUnitSelectorExpandedChanged(false)
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GoalDialog(
    dialogData: GoalDialogData,
    periodUnitSelectorExpanded: Boolean,
    libraryItems: List<LibraryItem>,
    libraryItemsSelectorExpanded: Boolean,
    onTargetChanged: (Int) -> Unit,
    onPeriodChanged: (Int) -> Unit,
    onPeriodUnitChanged: (GoalPeriodUnit) -> Unit,
    onPeriodUnitSelectorExpandedChanged: (Boolean) -> Unit,
    onGoalTypeChanged: (GoalType) -> Unit,
    onLibraryItemsSelectorExpandedChanged: (Boolean) -> Unit,
    onSelectedLibraryItemsChanged: (List<LibraryItem>) -> Unit,
    onConfirmHandler: () -> Unit,
    onDismissHandler: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissHandler,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp) // TODO: figure out a better way to do this
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialogHeader(title = stringResource(id = R.string.addGoalDialogTitle))

            var confirmButtonEnabled = true
            TimeInput(dialogData.target, onTargetChanged)
            confirmButtonEnabled = confirmButtonEnabled && dialogData.target > 0

            if(dialogData.periodUnit != null && dialogData.periodInPeriodUnits != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PeriodInput(
                    periodInPeriodUnits = dialogData.periodInPeriodUnits,
                    periodUnit = dialogData.periodUnit,
                    periodUnitSelectorExpanded = periodUnitSelectorExpanded,
                    onPeriodChanged = onPeriodChanged,
                    onPeriodUnitChanged = onPeriodUnitChanged,
                    onPeriodUnitSelectorExpandedChanged = onPeriodUnitSelectorExpandedChanged
                )
                confirmButtonEnabled = confirmButtonEnabled && dialogData.periodInPeriodUnits > 0
            }

            if(dialogData.goalType != null && dialogData.selectedLibraryItems != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.padding(horizontal = 32.dp)){Divider()}

                Spacer(modifier = Modifier.height(12.dp))

                MyToggleButton(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    options = GoalType.values().map {
                        ToggleButtonOption(it.ordinal, GoalType.toString(it))
                    },
                    selected = ToggleButtonOption(
                        dialogData.goalType.ordinal,
                        GoalType.toString(dialogData.goalType)
                    ),
                    onSelectedChanged = { option ->
                        onGoalTypeChanged(GoalType.values()[option.id])
                    }
                )

                if(dialogData.goalType == GoalType.ITEM_SPECIFIC) {
                    Spacer(modifier = Modifier.height(12.dp))

                    if(libraryItems.isNotEmpty()) {
                        SelectionSpinner(
                            expanded = libraryItemsSelectorExpanded,
                            options = libraryItems.map {
                                UUIDSelectionSpinnerOption(
                                    it.id,
                                    it.name
                                )
                            },
                            selected = dialogData.selectedLibraryItems.firstOrNull()?.let {
                                UUIDSelectionSpinnerOption(it.id, it.name)
                            } ?: libraryItems.first().let {
                                UUIDSelectionSpinnerOption(it.id, it.name)
                            },
                            onExpandedChange = onLibraryItemsSelectorExpandedChanged,
                            onSelectedChange = { selection ->
                                onSelectedLibraryItemsChanged(libraryItems.filter {
                                    it.id == (selection as UUIDSelectionSpinnerOption).id
                                })
                                onLibraryItemsSelectorExpandedChanged(false)
                            }
                        )
                    } else {
                        Text(text = "No items in your library.")

                    }
                }

                confirmButtonEnabled = confirmButtonEnabled &&
                    (
                        dialogData.goalType == GoalType.NON_SPECIFIC ||
                        dialogData.selectedLibraryItems.isNotEmpty()
                    )
            }

            DialogActions(
                onConfirmHandler = onConfirmHandler,
                onDismissHandler = onDismissHandler,
                confirmButtonEnabled = confirmButtonEnabled,
                confirmButtonText = "Create"
            )
        }
    }
}

@Composable
fun EditGoalDialog(
    value: Int,
    onValueChanged: (Int) -> Unit,
    onConfirmHandler: () -> Unit,
    onDismissHandler: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissHandler
    ) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            DialogHeader(title = stringResource(id = R.string.goalDialogTitleEdit))
            TimeInput(value, onValueChanged)
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismissHandler,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Cancel")
                }
                TextButton(
                    onClick = onConfirmHandler,
//                    enabled = name.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(id = R.string.goalDialogOkEdit))
                }
            }
        }
    }
}
