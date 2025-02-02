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
import com.example.mealmate.models.Ingredient
import com.example.mealmate.models.MealBoard
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

        // Set meal details
        findViewById<TextView>(R.id.tv_meal_name).text = mealBoard.mealName
        findViewById<TextView>(R.id.tv_created_by).text = getString(R.string.created_by, mealBoard.createdBy)
        findViewById<TextView>(R.id.tv_cooking_time).text = mealBoard.cookingTime
        findViewById<TextView>(R.id.tv_procedure).text = mealBoard.procedure

        // Extract selected ingredients for the shopping list
        shoppingList = mealBoard.ingredients.filter { it.isSelected } as ArrayList<Ingredient>

        // Show the shopping list button if there are selected ingredients
        btnViewShoppingList.visibility = if (shoppingList.isNotEmpty()) {
            Button.VISIBLE
        } else {
            Button.GONE
        }

        // Set up RecyclerView for ingredients
        rvMealList.layoutManager = LinearLayoutManager(this)
        rvMealList.adapter = IngredientsAdapter(mealBoard.ingredients, IngredientsAdapter.Mode.VIEW) { ingredient ->
            if (ingredient.isSelected) {
                if (!shoppingList.contains(ingredient)) {
                    shoppingList.add(ingredient)
                }
            } else {
                shoppingList.remove(ingredient)
            }

            btnViewShoppingList.visibility = if (shoppingList.isNotEmpty()) {
                Button.VISIBLE
            } else {
                Button.GONE
            }
        }

        // Handle "Add to Shopping List" button click
        btnViewShoppingList.setOnClickListener {
            saveShoppingList(mealBoard)
        }
    }

    private fun saveShoppingList(mealBoard: MealBoard) {
        if (shoppingList.isNotEmpty()) {
            // Check if the shopping list already exists in Firestore
            FireStoreClass().getShoppingLists(
                onSuccess = { shoppingLists ->
                    val existingList = shoppingLists.find {
                        it.forMeal == mealBoard.mealName
                    }

                    if (existingList != null) {
                        // If the meal already exists, check if there are missing ingredients
                        val missingIngredients = shoppingList.filter { ingredient ->
                            // Only consider ingredients that are not already in the list
                            !existingList.items.any { it.name == ingredient.name }
                        }

                        if (missingIngredients.isNotEmpty()) {
                            // If there are missing ingredients, add them to the existing list
                            val updatedShoppingList = existingList.copy(
                                items = ArrayList(existingList.items + missingIngredients),
                                timestamp = System.currentTimeMillis() // Update timestamp
                            )

                            // Update the shopping list with the new ingredients and timestamp
                            showProgressDialog(resources.getString(R.string.please_wait))
                            FireStoreClass().updateShoppingList(
                                updatedShoppingList,
                                onSuccess = {
                                    hideProgressDialog()
                                    showSuccessSnackBar("Shopping list updated successfully!")
                                    val intent = Intent(this, ShoppingListActivity::class.java)
                                    startActivity(intent)
                                },
                                onFailure = { errorMessage ->
                                    hideProgressDialog()
                                    showErrorSnackBar(errorMessage)
                                }
                            )
                        } else {
                            // If the ingredients are the same, show a message
                            showErrorSnackBar("This meal with the same ingredients already exists in your shopping list.")
                        }
                    } else {
                        // If the shopping list doesn't exist, create a new one
                        val shoppingListData = ShoppingList(
                            id = mealBoard.documentId,
                            forMeal = mealBoard.mealName,
                            items = shoppingList,
                            createdBy = mealBoard.createdBy,
                            timestamp = System.currentTimeMillis()
                        )

                        // Save the new shopping list
                        showProgressDialog(resources.getString(R.string.please_wait))
                        FireStoreClass().saveShoppingList(
                            shoppingListData,
                            onSuccess = {
                                hideProgressDialog()
                                showSuccessSnackBar("Shopping list added successfully!")
                                val intent = Intent(this, ShoppingListActivity::class.java)
                                startActivity(intent)
                            },
                            onFailure = { errorMessage ->
                                hideProgressDialog()
                                showErrorSnackBar(errorMessage)
                            }
                        )
                    }
                },
                onFailure = { errorMessage ->
                    showErrorSnackBar(errorMessage)
                }
            )
        } else {
            showErrorSnackBar("Please select at least one ingredient to add to the shopping list.")
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
