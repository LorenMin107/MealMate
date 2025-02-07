package com.example.mealmate.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.activities.CreateMealBoardActivity
import com.example.mealmate.activities.MainActivity
import com.example.mealmate.firebase.FireStoreClass
import com.example.mealmate.models.MealBoard

class MealBoardItemsAdapter(
    private val context: Context,
    private var list: MutableList<MealBoard>,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<MealBoardItemsAdapter.MyViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_meal_board, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = list[position]
        Glide.with(context)
            .load(model.mealImage)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(holder.itemView.findViewById(R.id.iv_create_meal_board_image))

        holder.itemView.findViewById<TextView>(R.id.tv_meal_board_name).text = model.mealName
        holder.itemView.findViewById<TextView>(R.id.tv_created_by).text =
            "Created By: ${model.createdBy}"

        holder.itemView.setOnClickListener {
            onClickListener?.onClick(position, model)
        }

        holder.itemView.findViewById<ImageButton>(R.id.ib_edit_meal_board).setOnClickListener {
            editMealBoard(model)
        }

        holder.itemView.findViewById<ImageButton>(R.id.ib_delete_meal_board).setOnClickListener {
            alertDialogForDeleteMealBoard(model.documentId, model.mealImage) {
                notifyItemChanged(holder.adapterPosition)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<MealBoard>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun alertDialogForDeleteMealBoard(
        mealBoardId: String,
        image: String,
        onCancel: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Meal Board")
            .setMessage("Are you sure you want to delete this meal board?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Yes") { dialogInterface, _ ->
                dialogInterface.dismiss()
                if (context is MainActivity) context.showProgressDialog("Please wait")

                FireStoreClass().deleteMealBoard(
                    mealBoardId = mealBoardId,
                    onSuccess = {
                        if (context is MainActivity) context.hideProgressDialog()
                        list.removeAll { it.documentId == mealBoardId }
                        notifyDataSetChanged()
                        Toast.makeText(
                            context,
                            "Meal board deleted successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { errorMessage ->
                        if (context is MainActivity) context.hideProgressDialog()
                        Toast.makeText(
                            context,
                            "Failed to delete: $errorMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                        onCancel()
                    }
                )
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
                onCancel()
            }
        builder.create().show()
    }

    private fun editMealBoard(mealBoard: MealBoard) {
        if (context is MainActivity) {
            val intent = Intent(context, CreateMealBoardActivity::class.java)
            intent.putExtra("mealBoardDetails", mealBoard)
            context.startActivity(intent)
        }
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnClickListener {
        fun onClick(position: Int, model: MealBoard)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    // Swipe Handler
    init {
        val swipeHandler = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && position < list.size) {
                    val mealBoard = list[position]
                    if (direction == ItemTouchHelper.LEFT) {
                        alertDialogForDeleteMealBoard(mealBoard.documentId, mealBoard.mealImage) {
                            notifyItemChanged(position)
                        }
                    } else if (direction == ItemTouchHelper.RIGHT) {
                        editMealBoard(mealBoard)
                        notifyItemChanged(position)
                    }
                } else {
                    notifyItemChanged(position)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                val textPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 40f
                    isAntiAlias = true
                }

                if (dX > 0) {
                    // Swipe right (Edit)
                    paint.color = ContextCompat.getColor(context, R.color.colorPrimary)
                    c.drawRect(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.left + dX,
                        itemView.bottom.toFloat(),
                        paint
                    )
                    c.drawText(
                        "Edit",
                        itemView.left + 50f,
                        itemView.top + itemView.height / 2f + 20f,
                        textPaint
                    )
                } else {
                    // Swipe left (Delete)
                    paint.color = ContextCompat.getColor(context, R.color.colorDelete)
                    c.drawRect(
                        itemView.right + dX,
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                    c.drawText(
                        "Delete",
                        itemView.right - 200f,
                        itemView.top + itemView.height / 2f + 20f,
                        textPaint
                    )
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}
