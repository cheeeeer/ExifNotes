package com.tommihirvonen.exifnotes.datastructures

import kotlinx.parcelize.Parcelize

@Parcelize
data class Camera(
        override var id: Long = 0,
        override var make: String? = null,
        override var model: String? = null,
        var serialNumber: String? = null,
        var minShutter: String? = null,
        var maxShutter: String? = null,
        var shutterIncrements: Increment = Increment.THIRD,
        var exposureCompIncrements: PartialIncrement = PartialIncrement.THIRD)
    : Gear(id, make, model), Comparable<Gear>