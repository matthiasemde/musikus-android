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
import de.practicetime.practicetime.entities.Category




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
    private val categoryColorRadioGroup = dialogView.findViewById<RadioGroup>(R.id.addCategoryDialogColor)

    private val categoryColorRadioGroupButtons = listOf<ImageButton>(
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

    init {
        // Dialog Setup
        addCategoryDialogBuilder.apply {
            setView(dialogView)
            setPositiveButton(R.string.addCategoryAlertOk) { _, _ ->
            }
            setNegativeButton(R.string.addCategoryAlertCancel) { dialog, _ ->
            }
        }
        categoryColorRadioGroupButtons.forEachIndexed { index, button ->
            button.isClickable = true;
            button.setBackgroundResource(R.drawable.background_color_picker_btn_unselected)
//            button.background.setTint(Color.parseColor(categoryColors[index]))
            button.setOnClickListener {
//                button.startAnimation(AnimationUtils.loadAnimation(getBaseContext(), android.R.anim.fade_in))
                if (it.isSelected) {
                    it.isSelected = false
                    button.setBackgroundResource(R.drawable.background_color_picker_btn_unselected)
//                    button.background.setTint(Color.parseColor(categoryColors[index]))
                } else {
                    it.isSelected = true
                    button.setBackgroundResource(R.drawable.background_color_picker_btn_selected)
//                    button.background.setTint(Color.parseColor(categoryColors[index]))
                }
            }
        }
    }
    val addCategoryDialog: AlertDialog = addCategoryDialogBuilder.create()


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
            private val addCategoryDialog: AlertDialog,
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
                    addCategoryDialog.show()
                }
            }
        }
    }
}