/*
 * This software is licensed under the MIT license
 *
 * Copyright (c) 2022, Javier Carbone, author Matthias Emde
 */

package de.practicetime.practicetime.ui.library

import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import de.practicetime.practicetime.R
import de.practicetime.practicetime.database.entities.Category


/**
 *  Adapter for the Category selection button grid.
 */

class CategoryAdapter(
    private val categories: List<Category>,
    private val selectedCategories: List<Int> = listOf(),
    private val context: Activity,
    private val showInActiveSession: Boolean = false,
    private val shortClickHandler: (index: Int) -> Unit = {},
    private val longClickHandler: (index: Int) -> Boolean = { false },
    private val addCategoryHandler: () -> Unit = {},
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

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

    // return the amount of categories (+1 for the add new button if shown in active session )
    override fun getItemCount() = categories.size + if(showInActiveSession) 1 else 0

    // create new views depending on view type
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> ViewHolder.CategoryViewHolder(
                inflater.inflate(
                    R.layout.listitem_category,
                    viewGroup,
                    false
                ),
                showInActiveSession,
                context,
                selectedCategories,
                shortClickHandler,
                longClickHandler,
            )
            else -> ViewHolder.AddNewCategoryViewHolder(
                inflater.inflate(
                    R.layout.listitem_add_new_category,
                    viewGroup,
                    false
                ),
                addCategoryHandler,
            )
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is ViewHolder.CategoryViewHolder -> viewHolder.bind(
                categories[position]
            )
            is ViewHolder.AddNewCategoryViewHolder -> viewHolder.bind()
        }
    }

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        class CategoryViewHolder(
            view: View,
            showInActiveSession: Boolean,
            private val context: Activity,
            private val selectedCategories: List<Int>,
            private val shortClickHandler: (index: Int) -> Unit,
            private val longClickHandler: (index: Int) -> Boolean,
        ) : ViewHolder(view) {
            private val button: Button = view.findViewById(R.id.categoryButton)

            init {
                // if the category is not shown inside the active session
                // it can grow to the size of its container
                if(!showInActiveSession) {
                    button.layoutParams.width = LayoutParams.MATCH_PARENT
                }
            }

            fun bind(category: Category) {
                button.isSelected = selectedCategories.contains(layoutPosition)

                // set up short and long click handler for selecting categories
                button.setOnClickListener { shortClickHandler(layoutPosition) }
                button.setOnLongClickListener {
                    // tell the event handler we consumed the event
                    return@setOnLongClickListener longClickHandler(layoutPosition)
                }


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
            }
        }

        class AddNewCategoryViewHolder(
            view: View,
            private val addCategoryHandler: () -> Unit,
        ) : ViewHolder(view) {

            private val button: ImageButton = view.findViewById(R.id.addNewCategory)

            fun bind() {
                button.setOnClickListener { addCategoryHandler() }
            }
        }
    }
}
