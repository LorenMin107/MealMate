package com.example.mealmate.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mealmate.R
import com.example.mealmate.adapters.MealAdapter
import com.example.mealmate.firebase.FireStoreClass
import com.example.mealmate.models.Ingredient
import com.example.mealmate.models.ShoppingList

class ShoppingListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shopping_list)

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

        setupActionBar()

        FireStoreClass().getShoppingLists(
            onSuccess = { shoppingLists ->
                // Reset isSelected to false for all ingredients
                shoppingLists.forEach { shoppingList ->
                    shoppingList.items.forEach { it.isSelected = false }
                }

                val groupedItems = groupIngredientsByMeal(shoppingLists)
                val rvGroupedList = findViewById<RecyclerView>(R.id.rv_grouped_list)
                rvGroupedList.layoutManager = LinearLayoutManager(this)
                rvGroupedList.adapter = MealAdapter(groupedItems)

                // Clear Button Logic
                val btnClearShoppingList = findViewById<Button>(R.id.btn_clear_shopping_list)
                btnClearShoppingList.setOnClickListener {
                    for ((meal, ingredients) in groupedItems) {
                        ingredients.removeAll { it.isSelected }
                    }

                    // Notify the adapter about the data changes
                    rvGroupedList.adapter?.notifyDataSetChanged()

                    // Update or delete shopping lists in FireStore
                    groupedItems.forEach { (mealName, updatedIngredients) ->
                        val shoppingListToUpdate = shoppingLists.find { it.forMeal == mealName }
                        if (shoppingListToUpdate != null) {
                            if (updatedIngredients.isEmpty()) {
                                // Delete the shopping list if no ingredients are left
                                FireStoreClass().deleteShoppingList(
                                    shoppingListToUpdate.id,
                                    onSuccess = { showSuccessSnackBar("$mealName shopping list removed.") },
                                    onFailure = { errorMessage -> showErrorSnackBar(errorMessage) }
                                )
                            } else {
                                // Update the shopping list with remaining ingredients
                                shoppingListToUpdate.items.clear()
                                shoppingListToUpdate.items.addAll(updatedIngredients)
                                FireStoreClass().updateShoppingList(
                                    shoppingListToUpdate,
                                    onSuccess = { showSuccessSnackBar("$mealName updated.") },
                                    onFailure = { errorMessage -> showErrorSnackBar(errorMessage) }
                                )
                            }
                        }
                    }
                }

                // Button to send shopping list via SMS
                val btnSendSMS = findViewById<Button>(R.id.btn_send_sms)
                btnSendSMS.setOnClickListener {
                    sendShoppingListViaSMS(groupedItems)
                }

            },
            onFailure = { errorMessage ->
                showErrorSnackBar(errorMessage)
            }
        )
    }

    private fun sendShoppingListViaSMS(groupedItems: Map<String, ArrayList<Ingredient>>) {
        // Format the shopping list into a string
        val shoppingListString = StringBuilder()
        for ((mealName, ingredients) in groupedItems) {
            shoppingListString.append("For $mealName\n")
            shoppingListString.append("Ingredients:\n")
            for (ingredient in ingredients) {
                shoppingListString.append("- ${ingredient.name}: ${ingredient.quantity} ${ingredient.unit}\n")
            }
            shoppingListString.append("\n")
        }

        // Create an intent to send an SMS
        val smsIntent = Intent(Intent.ACTION_SENDTO)
        smsIntent.data = Uri.parse("smsto:") // This ensures only SMS apps are shown
        smsIntent.putExtra("sms_body", shoppingListString.toString()) // Attach the shopping list body

        try {
            startActivity(smsIntent) // This will open the SMS app
        } catch (e: Exception) {
            Toast.makeText(this, "SMS app not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun groupIngredientsByMeal(shoppingLists: ArrayList<ShoppingList>): Map<String, ArrayList<Ingredient>> {
        return shoppingLists.groupBy { it.forMeal }
            .mapValues { entry -> ArrayList(entry.value.flatMap { it.items }) }
    }

    private fun setupActionBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_shopping_list)
        setSupportActionBar(toolbar)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_primary_color_back_24dp)
            it.title = getString(R.string.nav_shopping_list) // Optional: Set a title
        }

        toolbar.setNavigationOnClickListener {
            navigateToMainContent()
        }
    }

    private fun navigateToMainContent() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Override of deprecated onBackPressed function")
    override fun onBackPressed() {
        super.onBackPressed()
        navigateToMainContent()
    }
}
