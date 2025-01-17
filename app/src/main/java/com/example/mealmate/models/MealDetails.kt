package com.example.mealmate.models

import android.os.Parcel
import android.os.Parcelable

data class MealDetails(
    var title: String = "",
    var cookingTime: String = "",
    var description: String = "",
    var ingredients: String = "",
    val mealImage: String = "",
    val createdBy: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(title)
        writeString(description)
        writeString(ingredients)
        writeString(createdBy)
    }

    override fun describeContents()= 0

    companion object CREATOR : Parcelable.Creator<MealDetails> {
        override fun createFromParcel(source: Parcel): MealDetails = MealDetails(source)
        override fun newArray(size: Int): Array<MealDetails?> = arrayOfNulls(size)
    }
}