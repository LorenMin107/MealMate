package com.example.mealmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.models.MealBoard
import android.content.Context
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.mealmate.R

open class MealBoardItemsAdapter(
    private val context: Context,
    private var list: ArrayList<MealBoard>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_meal_board, parent, false))
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val model = list[position]
        if (holder is MyViewHolder)
        {
            Glide
                .with(context)
                .load(model.mealImage)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(holder.itemView.findViewById(R.id.iv_create_meal_board_image))

            holder.itemView.findViewById<TextView>(R.id.tv_meal_board_name).text = model.mealName
            holder.itemView.findViewById<TextView>(R.id.tv_created_by).text = "Created By: ${model.createdBy}"

            holder.itemView.setOnClickListener{
                if (onClickListener != null)
                {
                    onClickListener!!.onClick(position, model)
                }
            }


        }
    }

    interface OnClickListener{
        fun onClick(position: Int, model: MealBoard)
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private class MyViewHolder(view: View): RecyclerView.ViewHolder(view)

}