package com.example.mealmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.R
import com.example.mealmate.models.Ingredient

class ShoppingListAdapter(
    private val ingredients: ArrayList<Ingredient>
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_ingredient_name)
        val quantity: TextView = view.findViewById(R.id.tv_ingredient_quantity)
        val unit: TextView = view.findViewById(R.id.tv_ingredient_unit)
        val checkBox: CheckBox = view.findViewById(R.id.cb_done)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_shopping_list,
                parent,
                false
            ) // Ensure this matches `item_shopping_list.xml`
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredients[position]
        holder.name.text = ingredient.name
        holder.quantity.text = ingredient.quantity
        holder.unit.text = ingredient.unit

        holder.checkBox.isChecked = ingredient.isSelected
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            ingredient.isSelected = isChecked
        }
    }

    override fun getItemCount(): Int = ingredients.size
}

