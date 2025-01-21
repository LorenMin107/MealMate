package com.example.mealmate.models

import android.os.Parcel
import android.os.Parcelable

data class MealBoard(
    val mealName: String = "",
    val mealImage: String = "",
    val createdBy: String = "",
    val assignedTo: ArrayList<String> = ArrayList(),
    var documentId: String = "",
    val ingredients: ArrayList<Ingredient> = ArrayList(), // Updated
    val cookingTime: String = "",
    val procedure: String = "",
    val shoppingList: ArrayList<Ingredient> = ArrayList()

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createStringArrayList()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Ingredient.CREATOR)!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.createTypedArrayList(Ingredient.CREATOR)!!
    )

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(mealName)
        writeString(mealImage)
        writeString(createdBy)
        writeStringList(assignedTo)
        writeString(documentId)
        writeTypedList(ingredients)
        writeString(cookingTime)
        writeString(procedure)
        writeTypedList(shoppingList)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<MealBoard> {
        override fun createFromParcel(source: Parcel): MealBoard = MealBoard(source)
        override fun newArray(size: Int): Array<MealBoard?> = arrayOfNulls(size)
    }
}
