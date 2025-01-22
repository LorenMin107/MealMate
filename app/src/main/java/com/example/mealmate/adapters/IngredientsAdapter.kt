package com.example.mealmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.models.Ingredient
import com.example.mealmate.R

class IngredientsAdapter(
    private val ingredientList: ArrayList<Ingredient>,
    private val mode: Mode,
    private val onAction: (Ingredient) -> Unit
) : RecyclerView.Adapter<IngredientsAdapter.ViewHolder>() {

    enum class Mode {
        EDIT, VIEW
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_ingredient_name)
        val quantity: TextView = view.findViewById(R.id.tv_ingredient_quantity)
        val unit: TextView = view.findViewById(R.id.tv_ingredient_unit)
        val removeButton: Button? = view.findViewById(R.id.btn_remove_ingredient)
        val selectCheckBox: CheckBox? = view.findViewById(R.id.cb_select_ingredient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredientList[position]

        holder.name.text = ingredient.name
        holder.quantity.text = ingredient.quantity
        holder.unit.text = ingredient.unit

        when (mode) {
            Mode.EDIT -> {
                // Show "Remove" button and hide checkbox
                holder.removeButton?.visibility = View.VISIBLE
                holder.selectCheckBox?.visibility = View.GONE

                holder.removeButton?.setOnClickListener {
                    onAction(ingredient) // Notify activity to remove the ingredient
                }
            }

            Mode.VIEW -> {
                // Show checkbox and hide "Remove" button
                holder.removeButton?.visibility = View.GONE
                holder.selectCheckBox?.visibility = View.VISIBLE

                holder.selectCheckBox?.isChecked = ingredient.isSelected
                holder.selectCheckBox?.setOnCheckedChangeListener { _, isChecked ->
                    ingredient.isSelected = isChecked
                    onAction(ingredient) // Notify activity about the selection change
                }
            }
        }
    }

    override fun getItemCount(): Int = ingredientList.size
}
