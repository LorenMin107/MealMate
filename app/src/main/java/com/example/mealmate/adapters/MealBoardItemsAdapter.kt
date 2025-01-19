package com.example.mealmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.models.MealBoard
import android.content.Context
import android.content.Intent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.activities.CreateMealBoardActivity
import com.example.mealmate.activities.MainActivity
import com.example.mealmate.firebase.FireStoreClass
import com.example.mealmate.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
        if (holder is MyViewHolder) {
            Glide
                .with(context)
                .load(model.mealImage)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(holder.itemView.findViewById(R.id.iv_create_meal_board_image))

            holder.itemView.findViewById<TextView>(R.id.tv_meal_board_name).text = model.mealName
            holder.itemView.findViewById<TextView>(R.id.tv_created_by).text = "Created By: ${model.createdBy}"

            holder.itemView.setOnClickListener {
                onClickListener?.onClick(position, model)
            }
        }

        holder.itemView.findViewById<ImageButton>(R.id.ib_edit_meal_board).setOnClickListener {
            if (context is MainActivity) {
                context.startActivity(
                    Intent(context, CreateMealBoardActivity::class.java)
                        .putExtra("mealBoardDetails", model)  // Pass the MealBoard object
                )
            }
        }

        holder.itemView.findViewById<ImageButton>(R.id.ib_delete_meal_board).setOnClickListener {
            alertDialogForDeleteMealBoard(context, model.documentId, model.mealImage)
        }
    }

    private fun alertDialogForDeleteMealBoard(context: Context, mealBoardId: String, image: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Meal Board")
        builder.setMessage("Are you sure you want to delete this meal board?")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") { dialogInterface, _ ->
            dialogInterface.dismiss()
            if (context is MainActivity) {
                context.showProgressDialog("Please wait")
            }
            deleteMealBoard(context, mealBoardId, image)
        }
        builder.setNegativeButton("No") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun deleteMealBoard(context: Context, mealBoardId: String, imagePath: String) {
        val fireStore = FirebaseFirestore.getInstance()
        fireStore.collection(Constants.MEAL_BOARD).document(mealBoardId)
            .delete()
            .addOnSuccessListener {
                if (context is MainActivity) {
                    context.hideProgressDialog()
                }

                // Extract the relative path from the image URL
                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imagePath)
                storageReference.delete()
                    .addOnSuccessListener {
                        // Image deleted successfully
                        Toast.makeText(context, "Meal board deleted successfully.", Toast.LENGTH_SHORT).show()
                        FireStoreClass().getMealBoardsList(context as MainActivity)
                    }
                    .addOnFailureListener { e ->
                        // Handle failure to delete the image
                        Toast.makeText(context, "Failed to delete image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                if (context is MainActivity) {
                    context.hideProgressDialog()
                }
                Toast.makeText(context, "Failed to delete meal board: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    interface OnClickListener {
        fun onClick(position: Int, model: MealBoard)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
