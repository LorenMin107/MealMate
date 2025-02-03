package com.example.mealmate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.mealmate.R
import com.example.mealmate.adapters.MealBoardItemsAdapter
import com.example.mealmate.firebase.FireStoreClass
import com.example.mealmate.models.MealBoard
import com.example.mealmate.models.User
import com.example.mealmate.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var fabCreateBoard: FloatingActionButton
    private lateinit var rvMealBoardList: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvNoMealBoardsAvailable: TextView

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }

    private lateinit var mUserName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Draw behind the status bar
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // Set the status bar color to match the app bar
        window.statusBarColor = getColor(R.color.colorPrimary)

        // Apply light or dark status bar content
        val isLightStatusBar = false
        ViewCompat.getWindowInsetsController(findViewById(R.id.drawer_layout))?.isAppearanceLightStatusBars =
            isLightStatusBar

        fabCreateBoard = findViewById(R.id.fab_create_board)
        rvMealBoardList = findViewById(R.id.rv_meal_boards_list)
        tvNoMealBoardsAvailable = findViewById(R.id.tv_no_meal_boards_available)

        setupActionBar()

        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        FireStoreClass().loadUserData(this, true)

        fabCreateBoard.setOnClickListener {
            if (this::mUserName.isInitialized) { // Ensure mUserName is initialized
                val intent = Intent(this, CreateMealBoardActivity::class.java)
                intent.putExtra(Constants.NAME, mUserName)
                startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
            } else {
                Toast.makeText(
                    this,
                    "User data not loaded yet. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun populateBoardListToUI(mealBoardsList: ArrayList<MealBoard>) {
        hideProgressDialog()
        if (mealBoardsList.isNotEmpty()) {
            rvMealBoardList.visibility = View.VISIBLE
            tvNoMealBoardsAvailable.visibility = View.GONE

            rvMealBoardList.layoutManager = LinearLayoutManager(this)
            rvMealBoardList.setHasFixedSize(true)

            val adapter = MealBoardItemsAdapter(this, mealBoardsList, rvMealBoardList)
            rvMealBoardList.adapter = adapter

            adapter.setOnClickListener(object : MealBoardItemsAdapter.OnClickListener {
                override fun onClick(position: Int, model: MealBoard) {
                    val intent = Intent(this@MainActivity, MealListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })

        } else {
            rvMealBoardList.visibility = View.GONE
            tvNoMealBoardsAvailable.visibility = View.VISIBLE
        }
    }

    private fun setupActionBar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_main_activity)
        setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        toolbar.setNavigationOnClickListener {
            toggleDrawer()
        }

    }

    private fun toggleDrawer() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)

        } else {
            super.onBackPressed()
        }
    }

    fun updateNavigationUserDetails(user: User, readMealBoardList: Boolean) {

        mUserName = user.name

        val navView: NavigationView = findViewById(R.id.nav_view)
        val navHeader: View = navView.getHeaderView(0)
        val navUserImage: CircleImageView = navHeader.findViewById(R.id.nav_user_image)

        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage)

        val tvUsername: TextView = navHeader.findViewById(R.id.tv_username)
        tvUsername.text = user.name

        if (readMealBoardList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FireStoreClass().getMealBoardsList(this)
        }

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FireStoreClass().loadUserData(this)
        } else if (resultCode == RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE) {
            FireStoreClass().getMealBoardsList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_my_profile -> {
                startActivityForResult(
                    Intent(this, MyProfileActivity::class.java),
                    MY_PROFILE_REQUEST_CODE
                )
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }

            R.id.nav_shopping_list -> {
                // Navigate to ShoppingListActivity
                val intent = Intent(this, ShoppingListActivity::class.java)
                startActivity(intent)
            }
        }

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

}
