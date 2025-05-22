@file:Suppress("DEPRECATION")

package com.example.mealmate.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlin.math.sqrt

class ShoppingListActivity : BaseActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private lateinit var groupedItems: MutableMap<String, ArrayList<Ingredient>>
    private lateinit var mealAdapter: MealAdapter

    private val SMS_PERMISSION_REQUEST_CODE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shopping_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        setupActionBar()

        FireStoreClass().getShoppingLists(
            onSuccess = { shoppingLists ->
                // Reset isSelected to false for all ingredients
                shoppingLists.forEach { shoppingList ->
                    shoppingList.items.forEach { it.isSelected = false }
                }

                groupedItems = groupIngredientsByMeal(shoppingLists).toMutableMap()
                val rvGroupedList = findViewById<RecyclerView>(R.id.rv_grouped_list)
                val tvNoItems = findViewById<TextView>(R.id.tv_no_items)
                val progressBar = findViewById<ProgressBar>(R.id.progress_bar)

                if (groupedItems.isEmpty()) {
                    rvGroupedList.visibility = RecyclerView.GONE
                    tvNoItems.visibility = TextView.VISIBLE
                } else {
                    rvGroupedList.visibility = RecyclerView.VISIBLE
                    tvNoItems.visibility = TextView.GONE
                    rvGroupedList.layoutManager = LinearLayoutManager(this)
                    mealAdapter = MealAdapter(groupedItems)
                    rvGroupedList.adapter = mealAdapter
                }

                // Clear Button Logic
                val btnClearShoppingList = findViewById<Button>(R.id.btn_clear_shopping_list)
                btnClearShoppingList.setOnClickListener {
                    runOnUiThread { progressBar.visibility = View.VISIBLE }
                    val mealsToRemove = mutableListOf<String>()
                    for ((meal, ingredients) in groupedItems) {
                        val iterator = ingredients.iterator()
                        while (iterator.hasNext()) {
                            val ingredient = iterator.next()
                            if (ingredient.isSelected) {
                                iterator.remove()
                            }
                        }
                        if (ingredients.isEmpty()) {
                            mealsToRemove.add(meal)
                        }
                    }

                    // Remove meals from adapter and FireStore
                    mealsToRemove.forEach { meal ->
                        val position = groupedItems.keys.indexOf(meal)
                        groupedItems.remove(meal)
                        mealAdapter.notifyItemRemoved(position)
                        val shoppingListToUpdate = shoppingLists.find { it.forMeal == meal }
                        shoppingListToUpdate?.let {
                            FireStoreClass().deleteShoppingList(it.id, {
                                // Handle success
                                showSuccessSnackBar("Shopping list cleared")
                            }, { errorMessage ->
                                // Handle failure
                                showErrorSnackBar(errorMessage)
                            })
                        }
                    }

                    // Update remaining shopping lists in FireStore
                    groupedItems.forEach { (mealName, updatedIngredients) ->
                        val shoppingListToUpdate = shoppingLists.find { it.forMeal == mealName }
                        shoppingListToUpdate?.let {
                            it.items.clear()
                            it.items.addAll(updatedIngredients)
                            FireStoreClass().updateShoppingList(it, {
                                // Handle success
                            }, { errorMessage ->
                                // Handle failure
                                showErrorSnackBar(errorMessage)
                            })
                        }
                    }

                    // Notify the adapter of the range change
                    mealAdapter.notifyItemRangeChanged(0, groupedItems.size)

                    if (groupedItems.isEmpty()) {
                        rvGroupedList.visibility = RecyclerView.GONE
                        tvNoItems.visibility = TextView.VISIBLE
                    }

                    runOnUiThread { progressBar.visibility = View.GONE }
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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Request permission for SMS
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.SEND_SMS),
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

            Log.d("ShakeDetection", "Acceleration: $acceleration")

            if (acceleration > 1.5) { // This is for Emulate shake detection. For a real app, use a threshold of 7.0
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastShakeTime > 1000) {
                    lastShakeTime = currentTime
                    Log.d("ShakeDetection", "Shake detected!")
                    sendShoppingListViaSMS(groupedItems)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private fun sendShoppingListViaSMS(groupedItems: Map<String, ArrayList<Ingredient>>) {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val shoppingListString = StringBuilder()
            for ((mealName, ingredients) in groupedItems) {
                shoppingListString.append("For $mealName\n")
                shoppingListString.append("Ingredients:\n")
                for (ingredient in ingredients) {
                    shoppingListString.append("- ${ingredient.name}: ${ingredient.quantity} ${ingredient.unit}\n")
                }
                shoppingListString.append("\n")
            }

            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("smsto:")
            smsIntent.putExtra(
                "sms_body",
                shoppingListString.toString()
            )

            try {
                startActivity(smsIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "SMS app not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "SMS permission is required", Toast.LENGTH_SHORT).show()
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

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}