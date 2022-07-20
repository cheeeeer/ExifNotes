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

package com.tommihirvonen.exifnotes.datastructures

import android.content.Context
import android.os.Parcelable
import com.tommihirvonen.exifnotes.R
import kotlinx.parcelize.Parcelize

import java.util.ArrayList

@Parcelize
data class Frame(
        var id: Long = 0,
        var roll: Roll,
        var count: Int = 0,
        var date: DateTime? = null,
        var shutter: String? = null,
        var aperture: String? = null,
        var note: String? = null,
        var location: Location? = null,
        var formattedAddress: String? = null,
        var focalLength: Int = 0,
        var exposureComp: String? = null,
        var noOfExposures: Int = 1,
        var flashUsed: Boolean = false,
        var flashPower: String? = null, // not used
        var flashComp: String? = null, // not used
        var meteringMode: Int = 0, // not used
        var pictureFilename: String? = null,
        private var lightSource_: Int = 0,
        var lens: Lens? = null,
        var filters: MutableList<Filter> = ArrayList()
) : Parcelable {

    constructor(roll_: Roll) : this(roll = roll_)

    init {
        if (lightSource_ !in 0..7) lightSource_ = 0
    }

    var lightSource: Int
        get() = if (lightSource_ in 0..7) lightSource_ else 0
        set(value) { if (value in 0..7) lightSource_ = value }

    companion object {
        /**
         * Sorts a list of Frame objects based on sortMode in place.
         * This method is called when the user has selected a sorting criteria.
         *
         * @param context reference to the parent activity
         * @param sortMode enum type referencing the frame sort mode
         * @param list reference to the frame list that is to be sorted
         */
        fun sortFrameList(context: Context, sortMode: FrameSortMode, list: MutableList<Frame>) {
            when (sortMode) {
                FrameSortMode.FRAME_COUNT -> list.sortWith(compareBy { it.count })
                FrameSortMode.DATE -> list.sortWith(compareBy { it.date })
                FrameSortMode.LENS -> list.sortWith(compareBy { it.lens?.name })
                FrameSortMode.F_STOP -> {
                    val allApertureValues = context.resources.getStringArray(R.array.AllApertureValues)
                    list.sortWith(compareBy { allApertureValues.indexOf(it.aperture) })
                }
                FrameSortMode.SHUTTER_SPEED -> {
                    val allShutterValues = context.resources.getStringArray(R.array.AllShutterValues)
                    list.sortWith(compareBy { allShutterValues.indexOf(it.shutter) })
                }
            }
        }
    }

}