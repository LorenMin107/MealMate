package com.example.mealmate.models

import android.os.Parcel
import android.os.Parcelable

data class Ingredient(
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    @Transient
    var isSelected: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeString(quantity)
        writeString(unit)
        writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Ingredient> {
        override fun createFromParcel(source: Parcel): Ingredient = Ingredient(source)
        override fun newArray(size: Int): Array<Ingredient?> = arrayOfNulls(size)
    }
}