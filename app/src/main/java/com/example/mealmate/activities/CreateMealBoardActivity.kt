package com.example.mealmate.activities

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mealmate.R
import com.example.mealmate.firebase.FireStoreClass
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
    private lateinit var btnCreate: Button
    private lateinit var mMealBoardDetails: MealBoard
    private var mMealBoardDocumentId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_meal_board)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        ivMealBoardImage = findViewById(R.id.iv_create_meal_board_image)
        etMealBoardName = findViewById(R.id.et_meal_name)
        btnCreate = findViewById(R.id.btn_create)

        // Initialize mUserName here
        mUserName = getCurrentUserId()

        // Generate a new document ID for the meal board
        mMealBoardDocumentId = FireStoreClass().getMealBoardDocumentId()

        setupActionBar()
        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }

        // Check if we are editing an existing meal board
        if (intent.hasExtra("mealBoardDetails")) {
            mMealBoardDetails = intent.getParcelableExtra("mealBoardDetails")!!
            etMealBoardName.setText(mMealBoardDetails.mealName)


            // Load existing image into the image view
            Glide.with(this)
                .load(mMealBoardDetails.mealImage)
                .apply(RequestOptions().centerCrop())
                .placeholder(R.drawable.ic_board_place_holder)
                .into(ivMealBoardImage)
        }

        btnCreate.setOnClickListener {
            if (intent.hasExtra("mealBoardDetails")) {
                if (mSelectedImageFileUri != null) {
                    uploadBoardImage(true)
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

    }

    private fun updateMealBoard() {
        val mealBoardHashMap = HashMap<String, Any>()

        // If the meal image has been changed, update it
        if (mMealBoardImageURL.isNotEmpty() && mMealBoardImageURL != mMealBoardDetails.mealImage) {
            mealBoardHashMap["mealImage"] = mMealBoardImageURL
        }

        // If the meal board name has been changed, update it
        if (etMealBoardName.text.toString() != mMealBoardDetails.mealName) {
            mealBoardHashMap["mealName"] = etMealBoardName.text.toString()
        }

        // If there are any changes, update Firestore
        if (mealBoardHashMap.isNotEmpty()) {
            FireStoreClass().updateMealBoardDetails(mMealBoardDetails.documentId, mealBoardHashMap, this)
            showProgressDialog(resources.getString(R.string.please_wait))
            mealBoardUpdateSuccess()
        } else {
            Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
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
            mMealBoardDocumentId
        )
        FireStoreClass().createMealBoard(this, mealBoard, mMealBoardDocumentId)
    }

    private fun uploadBoardImage(isUpdate: Boolean) {
        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null) {
            val sRef = FirebaseStorage.getInstance().reference.child(
                "BOARD_IMAGE" + System.currentTimeMillis() + "." + Constants.getFileExtension(
                    this,
                    mSelectedImageFileUri!!
                )
            )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener { uri ->
                    mMealBoardImageURL = uri.toString()

                    // Conditional check for create or update
                    if (isUpdate) {
                        updateMealBoard() // Call update when the image upload is complete
                    } else {
                        createMealBoard() // Call create when the image upload is complete
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
                hideProgressDialog()
            }
        }
    }


    fun mealBoardCreatedSuccessfully() {
        hideProgressDialog()
        setResult(RESULT_OK)
        finish()
    }

    private fun setupActionBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_create_board_activity)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.create_meal_title)
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        ivMealBoardImage.setOnClickListener {
            // Check permission based on the Android version
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

        btnCreate.setOnClickListener {
            val mealBoardName = etMealBoardName.text.toString().trim()

            // Check if the name is empty
            if (mealBoardName.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter a name for the meal board.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (mSelectedImageFileUri != null) {
                    uploadBoardImage(false)
                } else {
                    showProgressDialog(resources.getString(R.string.please_wait))
                    createMealBoard()
                }
            }
        }
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
        if (resultCode == RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data != null) {
            mSelectedImageFileUri = data.data!!
            try {

                Glide
                    .with(this)
                    .load(Uri.parse(mSelectedImageFileUri.toString()))
                    .apply(RequestOptions().centerCrop())
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(ivMealBoardImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}