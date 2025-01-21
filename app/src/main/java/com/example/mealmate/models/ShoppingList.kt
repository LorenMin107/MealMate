package com.example.mealmate.models

import android.os.Parcelable
import android.os.Parcel

class ShoppingList(
    val id: String = "",
    val forMeal : String = "",
    val items: ArrayList<Ingredient> = ArrayList(),
    val createdBy: String = "",
    val timestamp: Long = System.currentTimeMillis()
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Ingredient.CREATOR)!!,
        parcel.readString()!!,
        parcel.readLong()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(id)
        writeString(forMeal)
        writeTypedList(items)
        writeString(createdBy)
        writeLong(timestamp)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<ShoppingList> {
        override fun createFromParcel(source: Parcel): ShoppingList = ShoppingList(source)
        override fun newArray(size: Int): Array<ShoppingList?> = arrayOfNulls(size)
    }


}