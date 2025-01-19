package com.example.mealmate.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.mealmate.activities.CreateMealBoardActivity
import com.example.mealmate.activities.MainActivity
import com.example.mealmate.activities.MealListActivity
import com.example.mealmate.activities.MyProfileActivity
import com.example.mealmate.activities.SignInActivity
import com.example.mealmate.activities.SignUpActivity
import com.example.mealmate.models.MealBoard
import com.example.mealmate.models.User
import com.example.mealmate.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FireStoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User) {
        mFireStore.collection(
            Constants.USERS
        ).document(
            getCurrentUserId()
        ).set(
            userInfo,
            SetOptions.merge()
        ).addOnSuccessListener {
            activity.userRegisterSuccess()
        }.addOnFailureListener { e ->
            Log.e("FireStore", "Error in setting user data", e)
        }
    }

    fun getMealBoardDetails(activity: MealListActivity, documentId: String) {
        mFireStore.collection(Constants.MEAL_BOARD)
            .document(documentId)
            .get()
            .addOnSuccessListener { documents ->
                Log.i(activity.javaClass.simpleName, documents.toString())
                activity.mealBoardDetails(documents.toObject(MealBoard::class.java)!!)
            }
            .addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting meal board list.",
                    exception
                )
            }
    }

    fun createMealBoard(activity: CreateMealBoardActivity, mealBoard: MealBoard, documentId: String) {
        mFireStore.collection(Constants.MEAL_BOARD).document(documentId).set(mealBoard, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Meal Board created successfully")
                Toast.makeText(activity, "Meal Board created successfully", Toast.LENGTH_SHORT)
                    .show()
                activity.mealBoardCreatedSuccessfully()
            }.addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating a meal board.",
                    exception
                )
            }
    }


    fun getMealBoardsList(activity: MainActivity) {
        mFireStore.collection(Constants.MEAL_BOARD)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener { documents ->
                Log.e(activity.javaClass.simpleName, documents.documents.toString())
                val mealBoardList: ArrayList<MealBoard> = ArrayList()
                for (document in documents) {
                    val mealBoard = document.toObject(MealBoard::class.java)
                    mealBoard.documentId = document.id
                    mealBoardList.add(mealBoard)
                }

                activity.populateBoardListToUI(mealBoardList)
            }
            .addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting meal board list.",
                    exception
                )
            }
    }



    fun updateUserProfileData(activity: MyProfileActivity, userHashMap: HashMap<String, Any>) {
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Profile data updated successfully")
                Toast.makeText(activity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                activity.profileUpdateSuccess()
            }.addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error while creating a meal board.", e)
                Toast.makeText(activity, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    fun loadUserData(activity: Activity, readMealBoardList: Boolean = false) {
        mFireStore.collection(
            Constants.USERS
        ).document(
            getCurrentUserId()
        ).get().addOnSuccessListener { document ->
            val loggedInUser = document.toObject(User::class.java)!!

            when (activity) {
                is SignInActivity -> {
                    activity.signInSuccess(loggedInUser)
                }

                is MainActivity -> {
                    activity.updateNavigationUserDetails(loggedInUser, readMealBoardList)
                }

                is MyProfileActivity -> {
                    activity.setUserDataInUI(loggedInUser)
                }
            }

        }.addOnFailureListener { e ->
            when (activity) {
                is SignInActivity -> {
                    activity.hideProgressDialog()
                }

                is MainActivity -> {
                    activity.hideProgressDialog()
                }
            }
            Log.e("FireStore", "Error in getting user data", e)
        }
    }



    fun getCurrentUserId(): String {

        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserId = ""

        if (currentUser != null) {
            currentUserId = currentUser.uid
        }
        return currentUserId
    }

    fun getMealBoardDocumentId(): String {
        return mFireStore.collection(Constants.MEAL_BOARD).document().id
    }



    fun updateMealBoardDetails(
        documentId: String,
        mealBoardHashMap: HashMap<String, Any>,
        activity: Activity
    ) {
        mFireStore.collection(Constants.MEAL_BOARD)
            .document(documentId)
            .update(mealBoardHashMap)
            .addOnSuccessListener {
                when (activity) {
                    is CreateMealBoardActivity -> activity.mealBoardUpdateSuccess()
                    is MealListActivity -> activity.mealBoardUpdateSuccess()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "Failed to update meal board: ${e.message}", Toast.LENGTH_LONG).show()
                when (activity) {
                    is CreateMealBoardActivity -> activity.hideProgressDialog()
                    is MealListActivity -> activity.hideProgressDialog()
                }
            }
    }


}