package com.example.mealmate.activities

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mealmate.R
import com.example.mealmate.adapters.IngredientsAdapter
import com.example.mealmate.firebase.FireStoreClass
import com.example.mealmate.models.Ingredient
import com.example.mealmate.models.MealBoard
import com.example.mealmate.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import java.io.IOException

class CreateMealBoardActivity : BaseActivity() {

    private var mSelectedImageFileUri: Uri? = null
    private lateinit var ivMealBoardImage: ImageView

    private lateinit var mUserName: String
    private var mMealBoardImageURL: String = ""
    private lateinit var etMealBoardName: EditText
    private lateinit var btnSave: Button
    private var mMealBoardDetails: MealBoard? = null
    private var mMealBoardDocumentId: String = ""

    private lateinit var ingredientsAdapter: IngredientsAdapter
    private val mIngredientsList: ArrayList<Ingredient> = ArrayList()


    private lateinit var etCookingTime: EditText
    private lateinit var etProcedure: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_meal_board)
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

        ivMealBoardImage = findViewById(R.id.iv_meal_image)
        etMealBoardName = findViewById(R.id.et_meal_name)
        etCookingTime = findViewById(R.id.et_cooking_time)
        etProcedure = findViewById(R.id.et_procedure)
        btnSave = findViewById(R.id.btn_save)

        mUserName = getCurrentUserId()
        mMealBoardDocumentId = FireStoreClass().getMealBoardDocumentId()

        setupActionBar()
        setupIngredientsRecyclerView()

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }


        if (intent.hasExtra("mealBoardDetails")) {
            mMealBoardDetails = intent.getParcelableExtra("mealBoardDetails")
            mMealBoardDetails?.let {
                loadMealBoardDetails(it.documentId)
            }
            btnSave.text = getString(R.string.update_meal_board)
            updateActionBarTitle(getString(R.string.update_meal_title))
        } else {
            btnSave.text = getString(R.string.create_meal_board)
            updateActionBarTitle(getString(R.string.create_meal_title))
        }

        btnSave.setOnClickListener {
            if (mMealBoardDetails != null) {
                if (mSelectedImageFileUri != null) {
                    deleteOldImageAndUploadNew()
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    updateMealBoard()
                }
            } else {
                if (mSelectedImageFileUri != null) {
                    uploadBoardImage(false)
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    createMealBoard()
                }
            }
        }
        // "Add Another" button setup
        val btnAddAnotherIngredient: Button = findViewById(R.id.btn_add_another_ingredient)
        btnAddAnotherIngredient.setOnClickListener {
            showAddIngredientDialog()
        }
    }
    private fun showAddIngredientDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_ingredient, null)

        val etName = dialogView.findViewById<EditText>(R.id.et_ingredient_name)
        val etQuantity = dialogView.findViewById<EditText>(R.id.et_ingredient_quantity)
        val etUnit = dialogView.findViewById<EditText>(R.id.et_ingredient_unit)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Ingredient")
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString()
                val quantity = etQuantity.text.toString()
                val unit = etUnit.text.toString()

                if (name.isNotEmpty() && quantity.isNotEmpty() && unit.isNotEmpty()) {
                    val ingredient = Ingredient(name, quantity, unit)
                    mIngredientsList.add(ingredient)
                    ingredientsAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun deleteOldImageAndUploadNew() {
        mMealBoardDetails?.mealImage?.let { oldImageUrl ->
            val oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl)
            oldImageRef.delete().addOnSuccessListener {
                uploadBoardImage(true)
            }.addOnFailureListener {
                uploadBoardImage(true) // Continue even if deletion fails
            }
        } ?: run {
            uploadBoardImage(true) // Upload new image if there's no old image
        }
    }

    private fun updateMealBoard() {
        val mealBoardHashMap = HashMap<String, Any>()

        mMealBoardDetails?.let {
            if (mMealBoardImageURL.isNotEmpty() && mMealBoardImageURL != it.mealImage) {
                mealBoardHashMap["mealImage"] = mMealBoardImageURL
            }

            if (etMealBoardName.text.toString() != it.mealName) {
                mealBoardHashMap["mealName"] = etMealBoardName.text.toString()
            }

            if (mIngredientsList != it.ingredients) {
                mealBoardHashMap["ingredients"] = mIngredientsList
            }

            if (etCookingTime.text.toString() != it.cookingTime) {
                mealBoardHashMap["cookingTime"] = etCookingTime.text.toString()
            }
            if (etProcedure.text.toString() != it.procedure) {
                mealBoardHashMap["procedure"] = etProcedure.text.toString()
            }

            if (mealBoardHashMap.isNotEmpty()) {
                showProgressDialog(resources.getString(R.string.please_wait))
                FireStoreClass().updateMealBoardDetails(
                    it.documentId,
                    mealBoardHashMap,
                    onSuccess = {
                        hideProgressDialog()
                        mealBoardUpdateSuccess()
                    },
                    onFailure = { errorMessage ->
                        hideProgressDialog()
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                hideProgressDialog()
                Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun createMealBoard() {
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserId())

        val mealBoard = MealBoard(
            etMealBoardName.text.toString(),
            mMealBoardImageURL,
            mUserName,
            assignedUsersArrayList,
            mMealBoardDocumentId,
            mIngredientsList,
            etCookingTime.text.toString(),
            etProcedure.text.toString()
        )
        FireStoreClass().createMealBoard(this, mealBoard, mMealBoardDocumentId)
    }

    private fun uploadBoardImage(isUpdate: Boolean) {
        showProgressDialog(resources.getString(R.string.please_wait))

        mSelectedImageFileUri?.let {
            val sRef = FirebaseStorage.getInstance().reference.child(
                "BOARD_IMAGE" + System.currentTimeMillis() + "." + Constants.getFileExtension(
                    this, it
                )
            )

            sRef.putFile(it).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    mMealBoardImageURL = uri.toString()

                    if (isUpdate) {
                        updateMealBoard()
                    } else {
                        createMealBoard()
                    }
                }.addOnFailureListener { exception ->
                    hideProgressDialog()
                    Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { exception ->
                hideProgressDialog()
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
            }
        } ?: run {
            hideProgressDialog()
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    fun mealBoardCreatedSuccessfully() {
        hideProgressDialog()
        setResult(RESULT_OK)
        finish()
    }

    private fun setupActionBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_create_meal_board_activity)
        setSupportActionBar(toolbar)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_primary_color_back_24dp)
        }

        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
        toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.colorPrimary))

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        ivMealBoardImage.setOnClickListener {
            val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission.READ_MEDIA_IMAGES
            } else {
                permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    permissionToCheck
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Constants.showImageChooser(this)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permissionToCheck),
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private fun updateActionBarTitle(title: String) {
        supportActionBar?.title = title
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Constants.showImageChooser(this)
            } else {
                Toast.makeText(
                    this,
                    "Oops, you just denied the permission for storage. You can also allow it from settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data?.data != null) {
            mSelectedImageFileUri = data.data
            try {
                Glide.with(this)
                    .load(Uri.parse(mSelectedImageFileUri.toString()))
                    .apply(RequestOptions().centerCrop())
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(ivMealBoardImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadMealBoardDetails(documentId: String) {
        FireStoreClass().getMealBoardDetails(
            documentId,
            onSuccess = { mealBoard ->
                mMealBoardDetails = mealBoard
                etMealBoardName.setText(mealBoard.mealName)
                etCookingTime.setText(mealBoard.cookingTime)
                etProcedure.setText(mealBoard.procedure)
                Glide.with(this)
                    .load(mealBoard.mealImage)
                    .apply(RequestOptions().centerCrop())
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(ivMealBoardImage)

                mIngredientsList.clear()
                mIngredientsList.addAll(mealBoard.ingredients)
                ingredientsAdapter.notifyDataSetChanged()
            },
            onFailure = { errorMessage ->
                hideProgressDialog()
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupIngredientsRecyclerView() {
        val rvIngredientsList: RecyclerView = findViewById(R.id.rv_ingredients_list)
        rvIngredientsList.layoutManager = LinearLayoutManager(this)

        ingredientsAdapter = IngredientsAdapter(mIngredientsList, IngredientsAdapter.Mode.EDIT) { ingredient ->
            // Remove the ingredient from the list and update the adapter
            mIngredientsList.remove(ingredient)
            ingredientsAdapter.notifyDataSetChanged()
        }

        rvIngredientsList.adapter = ingredientsAdapter
    }
}
