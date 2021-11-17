package de.practicetime.practicetime

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Dao
import android.view.ViewGroup.LayoutParams
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


/**
 *  Adapter for the Category selection button grid.
 */

val categoryColors = listOf(
    "#000000",
    "#0000FF",
    "#00FF00",
    "#00FFFF",
    "#FF0000",
    "#FF00FF",
    "#FFFF00",
    "#AAAAAA",
    "#AAAAAA",
    "#AAAAAA",
)

class CategoryAdapter(
    private val categories: ArrayList<Category>,
    private val callback: View.OnClickListener,
    private val dao: PTDao,
    private val context: Activity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val grow: Boolean = false,
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    // instantiate the builder for the alert dialog
    private val addCategoryDialogBuilder = AlertDialog.Builder(context)
    private val inflater = context.layoutInflater;
    private val dialogView = inflater.inflate(
        R.layout.dialog_view_add_or_change_category,
        null,
    )

    private val categoryNameView = dialogView.findViewById<EditText>(R.id.addCategoryDialogName)
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
    private var selectedColor: Int? = null

    var addCategoryDialog: AlertDialog? = null

    init {
        // Dialog Setup
        addCategoryDialogBuilder.apply {
            setView(dialogView)
            setPositiveButton(R.string.addCategoryAlertOk) { dialog, _ ->
                val name = categoryNameView.text.toString().trim()
                if(selectedColor != null && name.isNotEmpty()) {
                    val newCategory = Category(
                    0,
                        name = name,
                        color = selectedColor!!,
                        archived = false,
                        profile_id = 0
                    )


                    lifecycleScope.launch {
                        dao.insertCategory(newCategory)
                    }
                }
                categoryNameView.text.clear()
                categoryColorButtonGroupRow1.clearCheck()
                categoryColorButtonGroupRow2.clearCheck()
                dialog.dismiss()
            }
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
                categoryNameView.text.clear()
                categoryColorButtonGroupRow1.clearCheck()
                categoryColorButtonGroupRow2.clearCheck()
                dialog.cancel()
            }
        }

        categoryColorButtons.forEachIndexed { index, button ->
            button.background.setTint(Color.parseColor(categoryColors[index]))
            button.setOnCheckedChangeListener { buttonView, isChecked ->
                if(isChecked) {
                    selectedColor = Color.parseColor(categoryColors[index])
                    if (index < 5) {
                        categoryColorButtonGroupRow2.clearCheck()
                    } else {
                        categoryColorButtonGroupRow1.clearCheck()
                    }
                }
            }
        }

        addCategoryDialog = addCategoryDialogBuilder.create()
    }



    companion object {
        private const val VIEW_TYPE_CATEGORY = 1
        private const val VIEW_TYPE_ADD_NEW = 2
    }

    // returns the view type (ADD_NEW button on last position)
    override fun getItemViewType(position: Int): Int {
        return if (position < categories.size)
            VIEW_TYPE_CATEGORY
        else
            VIEW_TYPE_ADD_NEW
    }

    // return the amount of categories + 1 for the add new button
    override fun getItemCount() = categories.size + 1

    // create new views depending on view type
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> ViewHolder.CategoryViewHolder(
                inflater.inflate(
                    R.layout.view_category_item,
                    viewGroup,
                    false
                ),
                callback,
                grow,
            )
            else -> ViewHolder.AddNewCategoryViewHolder(
                inflater.inflate(
                    R.layout.view_add_new_category,
                    viewGroup,
                    false
                ),
                dao,
                addCategoryDialog,
                grow,
            )
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is ViewHolder.CategoryViewHolder -> viewHolder.bind(
                categories[position]
            )
            is ViewHolder.AddNewCategoryViewHolder -> viewHolder.bind(

            )
        }
    }

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        class CategoryViewHolder(
            view: View,
            callback: View.OnClickListener,
            private val grow: Boolean
        ) : ViewHolder(view) {
            private val button: Button = view.findViewById(R.id.categoryButton)

            init {
                if(grow) {
                    button.layoutParams.width = LayoutParams.MATCH_PARENT
                }
                button.setOnClickListener(callback)
            }

            fun bind(category: Category) {
                // store the id of the category on the button
                button.tag = category.id

                // archived categories should not be displayed
                if (category.archived) {
                    button.visibility = View.GONE
                }

                // contents of the view with that element
                button.text = category.name
                button.background.setTint(category.color)

                // TODO set right margin for last 3 elements programmatically
            }
        }

        class AddNewCategoryViewHolder(
            view: View,
            private val dao: PTDao,
            private val addCategoryDialog: AlertDialog?,
            private val grow: Boolean,
        ) : ViewHolder(view) {
            private val button: ImageButton = view.findViewById(R.id.addNewCategory)

            init {
                if(grow) {
                    button.layoutParams.width = LayoutParams.MATCH_PARENT
                }
            }


            fun bind() {

                button.setOnClickListener {
                    addCategoryDialog?.show()
                    addCategoryDialog?.also {
                        val positiveButton = it.getButton(AlertDialog.BUTTON_POSITIVE)
                        val categoryNameView =
                            it.findViewById<EditText>(R.id.addCategoryDialogName)
                        val categoryColorButtonGroupRow1 =
                            it.findViewById<RadioGroup>(R.id.addCategoryDialogColorRow1)
                        val categoryColorButtonGroupRow2 =
                            it.findViewById<RadioGroup>(R.id.addCategoryDialogColorRow2)

                        fun isComplete(): Boolean {
                            return categoryNameView?.text.toString().trim().isNotEmpty() &&
                                    (categoryColorButtonGroupRow1?.checkedRadioButtonId != -1 ||
                                            categoryColorButtonGroupRow2?.checkedRadioButtonId != -1)

                        }

                        positiveButton.isEnabled = false
                        categoryNameView?.addTextChangedListener   {
                            positiveButton.isEnabled = isComplete()
                        }
                        categoryColorButtonGroupRow1?.setOnCheckedChangeListener { _, _ ->
                            positiveButton.isEnabled = isComplete()
                        }
                        categoryColorButtonGroupRow2?.setOnCheckedChangeListener { _, _ ->
                            positiveButton.isEnabled = isComplete()
                        }
                    }
                }
            }
        }
    }
}