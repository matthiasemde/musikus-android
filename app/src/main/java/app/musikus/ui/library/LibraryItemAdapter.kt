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

package app.musikus.ui.library

import android.app.Activity
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.musikus.R
import app.musikus.database.daos.LibraryItem


/**
 *  Adapter for the LibraryItem selection button grid.
 */

class LibraryItemAdapter(
    private val libraryItems: List<LibraryItem>,
    private val selectedLibraryItems: List<Int> = listOf(),
    private val context: Activity,
    private val showInActiveSession: Boolean = false,
    private val shortClickHandler: (index: Int) -> Unit = {},
    private val longClickHandler: (index: Int) -> Boolean = { false },
    private val addLibraryItemHandler: () -> Unit = {},
    ) : RecyclerView.Adapter<LibraryItemAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 1
        private const val VIEW_TYPE_ADD_NEW = 2
    }

    // returns the view type (ADD_NEW button on last position)
    override fun getItemViewType(position: Int): Int {
        return if (position < libraryItems.size)
            VIEW_TYPE_CATEGORY
        else
            VIEW_TYPE_ADD_NEW
    }

    // return the amount of library items (+1 for the add new button if shown in active session )
    override fun getItemCount() = libraryItems.size + if(showInActiveSession) 1 else 0

    // create new views depending on view type
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> ViewHolder.ListItemViewHolder(
                inflater.inflate(
                    if (showInActiveSession) R.layout.listitem_library_item_old
                    else R.layout.listitem_library_item,
                    viewGroup,
                    false
                ),
                showInActiveSession,
                context,
                selectedLibraryItems,
                shortClickHandler,
                longClickHandler,
            )
            else -> ViewHolder.AddNewLibraryItemViewHolder(
                inflater.inflate(
                    R.layout.listitem_add_new_library_item,
                    viewGroup,
                    false
                ),
                addLibraryItemHandler,
            )
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is ViewHolder.ListItemViewHolder -> viewHolder.bind(
                libraryItems[position]
            )
            is ViewHolder.AddNewLibraryItemViewHolder -> viewHolder.bind()
        }
    }

    sealed class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        class ListItemViewHolder(
            private val view: View,
            private val showInActiveSession: Boolean,
            private val context: Activity,
            private val selectedLibraryItems: List<Int>,
            private val shortClickHandler: (index: Int) -> Unit,
            private val longClickHandler: (index: Int) -> Boolean,
        ) : ViewHolder(view) {

            fun bind(libraryItem: LibraryItem) {
                val libraryItemColors = context.resources.getIntArray(R.array.library_item_colors)

                // set up short and long click handler for selecting library items
                itemView.setOnClickListener { shortClickHandler(layoutPosition) }
                itemView.setOnLongClickListener {
                    // tell the event handler we consumed the event
                    return@setOnLongClickListener longClickHandler(layoutPosition)
                }

                if(showInActiveSession) {
                    val button = view.findViewById<Button>(R.id.libraryItemButton)
                    button.isSelected = selectedLibraryItems.contains(layoutPosition)


                    // store the id of the library item on the button
                    button.tag = libraryItem.id

                    // contents of the view with that element
                    button.text = libraryItem.name

                    button.backgroundTintList = ColorStateList.valueOf(
                        libraryItemColors[libraryItem.colorIndex ?: 69]
                    )
                } else {
//                    val cardView = view.findViewById<CardView>(R.id.library_item_card)
                    val colorIndicatorView = view.findViewById<ImageView>(R.id.library_item_color_indicator)
                    val nameView = view.findViewById<TextView>(R.id.library_item_name)

//                    cardView.isSelected = selectedLibraryItems.contains(layoutPosition)
                    colorIndicatorView.backgroundTintList = ColorStateList.valueOf(
                        libraryItemColors[libraryItem.colorIndex ?: 69]
                    )

                    nameView.text = libraryItem.name
                }
            }
        }

        class AddNewLibraryItemViewHolder(
            view: View,
            private val addLibraryItemHandler: () -> Unit,
        ) : ViewHolder(view) {

            private val button: ImageButton = view.findViewById(R.id.addNewLibraryItem)

            fun bind() {
                button.setOnClickListener { addLibraryItemHandler() }
            }
        }
    }
}
