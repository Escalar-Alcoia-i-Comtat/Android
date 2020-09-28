package com.arnyminerz.escalaralcoiaicomtat.data.train

import android.content.Context
import android.os.Parcelable
import com.google.android.material.chip.Chip

interface TrainDataHolder : Parcelable {
    fun clone(): TrainDataHolder

    fun updateChip(context: Context, chip: Chip?)
}