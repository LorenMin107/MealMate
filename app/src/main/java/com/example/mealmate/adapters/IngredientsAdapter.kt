package com.example.mealmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.models.Ingredient
import com.example.mealmate.R

class IngredientsAdapter(
    private val ingredientList: ArrayList<Ingredient>,
    private val onIngredientClick: (Ingredient) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tv_ingredient_name)
        val quantity = view.findViewById<TextView>(R.id.tv_ingredient_quantity)
        val unit = view.findViewById<TextView>(R.id.tv_ingredient_unit)
        val selectButton = view.findViewById<CheckBox>(R.id.cb_select_ingredient)

        fun bind(ingredient: Ingredient) {
            name.text = ingredient.name
            quantity.text = ingredient.quantity
            unit.text = ingredient.unit
            selectButton.isChecked = ingredient.isSelected

            selectButton.setOnCheckedChangeListener { _, isChecked ->
                ingredient.isSelected = isChecked
                onIngredientClick(ingredient)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ingredientList[position])
    }

    override fun getItemCount(): Int = ingredientList.size

}