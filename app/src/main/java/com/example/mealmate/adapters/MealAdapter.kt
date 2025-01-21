package com.example.mealmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.models.Ingredient
import com.example.mealmate.R

class MealAdapter(
    private val meals: Map<String, ArrayList<Ingredient>>
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    inner class MealViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mealName: TextView = view.findViewById(R.id.tv_meal_name)
        val ingredientList: RecyclerView = view.findViewById(R.id.rv_ingredient_list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val mealName = meals.keys.elementAt(position)
        val ingredients = meals[mealName]

        holder.mealName.text = mealName
        holder.ingredientList.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.ingredientList.adapter = ShoppingListAdapter(ingredients!!)
    }

    override fun getItemCount(): Int = meals.size
}

