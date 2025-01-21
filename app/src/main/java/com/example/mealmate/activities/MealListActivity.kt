package com.example.mealmate.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.adapters.IngredientsAdapter
import com.example.mealmate.firebase.FireStoreClass
import com.example.mealmate.models.MealBoard
import com.example.mealmate.models.Ingredient
import com.example.mealmate.models.ShoppingList
import com.example.mealmate.utils.Constants

class MealListActivity : BaseActivity() {

    private lateinit var btnViewShoppingList: Button
    private lateinit var rvMealList: RecyclerView
    private var shoppingList: ArrayList<Ingredient> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meal_list)

        btnViewShoppingList = findViewById(R.id.btn_view_shopping_list)
        rvMealList = findViewById(R.id.rv_ingredient_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars =
                true
        }

        var mealBoardDocumentId = ""
        if (intent.hasExtra(Constants.DOCUMENT_ID)) {
            mealBoardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        FireStoreClass().getMealBoardDetails(
            documentId = mealBoardDocumentId,
            onSuccess = { mealBoard ->
                hideProgressDialog()
                setupMealBoardDetails(mealBoard)

                btnViewShoppingList.setOnClickListener {
                    val intent = Intent(this, ShoppingListActivity::class.java)
                    intent.putParcelableArrayListExtra(Constants.SHOPPING_LISTS, mealBoard.shoppingList)
                    startActivity(intent)
                }
            },
            onFailure = { errorMessage ->
                hideProgressDialog()
                showErrorSnackBar(errorMessage)
            }
        )
    }

    private fun setupMealBoardDetails(mealBoard: MealBoard) {
        setupActionBar(mealBoard.mealName)

        // Set meal image
        val ivMealImage: ImageView = findViewById(R.id.iv_meal_image)
        if (mealBoard.mealImage.isNotEmpty()) {
            Glide.with(this)
                .load(mealBoard.mealImage)
                .centerCrop()
                .placeholder(R.drawable.ic_board_place_holder)
                .into(ivMealImage)
        }

        // Set meal name
        val tvMealName: TextView = findViewById(R.id.tv_meal_name)
        tvMealName.text = mealBoard.mealName

        // Set created by
        val tvCreatedBy: TextView = findViewById(R.id.tv_created_by)
        tvCreatedBy.text = getString(R.string.created_by, mealBoard.createdBy)

        // Set cooking time
        val tvCookingTime: TextView = findViewById(R.id.tv_cooking_time)
        tvCookingTime.text = mealBoard.cookingTime
        // Set procedure
        val tvProcedure: TextView = findViewById(R.id.tv_procedure)
        tvProcedure.text = mealBoard.procedure

        // Extract selected ingredients for the shopping list
        shoppingList = mealBoard.ingredients.filter { it.isSelected } as ArrayList<Ingredient>

        // Show the shopping list button if there are selected ingredients
        btnViewShoppingList.visibility = if (shoppingList.isNotEmpty()) {
            Button.VISIBLE
        } else {
            Button.GONE
        }

        // Set up the RecyclerView with the ingredients
        rvMealList.layoutManager = LinearLayoutManager(this)
        rvMealList.adapter = IngredientsAdapter(mealBoard.ingredients) { ingredient ->
            if (ingredient.isSelected) {
                if (!mealBoard.shoppingList.contains(ingredient)) {
                    mealBoard.shoppingList.add(ingredient)
                }
            } else {
                mealBoard.shoppingList.remove(ingredient)
            }

            btnViewShoppingList.visibility = if (mealBoard.shoppingList.isNotEmpty()) {
                Button.VISIBLE
            } else {
                Button.GONE
            }

            // Save the shopping list to Firestore
            val shoppingList = ShoppingList(
                id = mealBoard.documentId,
                forMeal = mealBoard.mealName,
                items = mealBoard.shoppingList,
                createdBy = mealBoard.createdBy
            )
            FireStoreClass().saveShoppingList(shoppingList, {}, {})
        }
    }

    private fun setupActionBar(title: String) {
        val toolbar: Toolbar = findViewById(R.id.toolbar_meal_list_activity)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = title
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}