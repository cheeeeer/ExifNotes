/*
 * Exif Notes
 * Copyright (C) 2022  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.adapters

import android.content.Context
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.databinding.ItemGearBinding
import com.tommihirvonen.exifnotes.datastructures.*

class FilmStockAdapter(private val context: Context)
    : RecyclerView.Adapter<FilmStockAdapter.ViewHolder>() {

    var filmStocks: List<FilmStock> = emptyList()

    companion object {
        // Since we have to manually add the menu items to the context menus to pass the item position
        // to the implementing class, menu item ids are declared here instead of a menu resource file.
        // No menu resource file can be referenced here, so we use public constants instead.
        const val MENU_ITEM_EDIT = 1
        const val MENU_ITEM_DELETE = 2
    }

    init {
        // Used to make the RecyclerView perform better and to make our custom animations
        // work more reliably. Now we can use notifyDataSetChanged(), which works well
        // with possible custom animations.
        setHasStableIds(true)
    }

    /**
     * Package-private ViewHolder class which can be recycled
     * for better performance and memory management.
     * All common view elements for all items are initialized here.
     */
    inner class ViewHolder(val binding: ItemGearBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Instead of short click perform long click to activate the OnCreateContextMenuListener.
            binding.itemGearLayout.setOnClickListener { obj: View -> obj.performLongClick() }
            binding.itemGearLayout.setOnCreateContextMenuListener { contextMenu: ContextMenu, _: View?, _: ContextMenu.ContextMenuInfo? ->
                val filmStock = filmStocks[bindingAdapterPosition]
                val name = filmStock.name
                contextMenu.setHeaderTitle(name)

                // Use the order parameter (3rd parameter) of the ContextMenu.add() method
                // to pass the position of the list item which was clicked.
                // This can be used in the implementing class to retrieve the items position.
                contextMenu.add(0, MENU_ITEM_EDIT,
                    bindingAdapterPosition, R.string.Edit)
                contextMenu.add(0, MENU_ITEM_DELETE,
                    bindingAdapterPosition, R.string.Delete)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemGearBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filmStock = filmStocks[position]
        holder.binding.tvGearName.text = filmStock.name
        val stringBuilder = StringBuilder()
        stringBuilder.append("ISO:").append("\t\t\t\t\t\t\t").append(filmStock.iso).append("\n")
            .append("Type:").append("\t\t\t\t\t\t").append(filmStock.getTypeName(context)).append("\n")
            .append("Process:").append("\t\t\t").append(filmStock.getProcessName(context))
        holder.binding.tvMountables.text = stringBuilder.toString()
    }

    override fun getItemCount(): Int = filmStocks.size

    override fun getItemId(position: Int): Long = filmStocks[position].id

}