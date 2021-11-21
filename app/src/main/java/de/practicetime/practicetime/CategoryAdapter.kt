package de.practicetime.practicetime

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import de.practicetime.practicetime.entities.Category
import kotlinx.coroutines.launch


/**
 *  Adapter for the Category selection button grid.
 */

class CategoryAdapter(
    lifecycleScope: LifecycleCoroutineScope,
    dao: PTDao?,
    private val categories: ArrayList<Category>,
    private val callback: View.OnClickListener,
    private val context: Activity,
    private val grow: Boolean = false,
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    var addCategoryDialog: CategoryDialog? = null

    init {

        // the handler for creating new categories
        fun addCategoryHandler(newCategory: Category) {
            lifecycleScope.launch {
                dao?.insertCategory(newCategory)
                categories.add(newCategory)
                notifyItemInserted(categories.size)
            }
        }

        // create a new category dialog for adding new categories
        addCategoryDialog = CategoryDialog(context, ::addCategoryHandler)
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
                context
            )
            else -> ViewHolder.AddNewCategoryViewHolder(
                inflater.inflate(
                    R.layout.view_add_new_category,
                    viewGroup,
                    false
                ),
                grow,
                addCategoryDialog,
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
            grow: Boolean,
            val context: Activity,
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

                val categoryColors =  context.resources.getIntArray(R.array.category_colors)
                button.backgroundTintList = ColorStateList.valueOf(
                    categoryColors[category.colorIndex]
                )

                // TODO set right margin for last 3 elements programmatically
            }
        }

        class AddNewCategoryViewHolder(
            view: View,
            grow: Boolean,
            private val addCategoryDialog: CategoryDialog?,
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
                }
            }
        }
    }
}