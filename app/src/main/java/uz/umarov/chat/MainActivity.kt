package uz.umarov.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.umarov.chat.adapters.UserAdapter
import uz.umarov.chat.databinding.ActivityMainBinding
import uz.umarov.chat.models.User

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var userList: ArrayList<User>
    private lateinit var adapter: UserAdapter
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userList = ArrayList()
        adapter = UserAdapter(this, userList)

        val coroutineScope = CoroutineScope(Dispatchers.Main)

        coroutineScope.launch {

            binding.progressBar.visibility = View.VISIBLE
            binding.rv.visibility = View.INVISIBLE

            val scaleAnimation = ScaleAnimation(
                0f, 1f,  // Start and end scale X
                0f, 1f,  // Start and end scale Y
                Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot X
                Animation.RELATIVE_TO_SELF, 0.5f  // Pivot Y
            ).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
            }

            delay(1000)

            binding.refresh.setOnClickListener {

                coroutineScope.launch {

                    binding.progressBar.visibility = View.VISIBLE
                    binding.rv.visibility = View.INVISIBLE

                    filterUserList(binding.searchEditText.text.toString().trim())

                    binding.progressBar.visibility = View.INVISIBLE
                    binding.rv.visibility = View.VISIBLE
                    binding.rv.startAnimation(scaleAnimation)
                }
            }

            val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
            val navigationView: NavigationView = findViewById(R.id.navigationView)

            binding.menu.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }
            navigationView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_item1 -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }

                    R.id.nav_item2 -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }

                    else -> false
                }
            }

            mAuth = FirebaseAuth.getInstance()
            mDbRef = FirebaseDatabase.getInstance().reference

            binding.rv.layoutManager = LinearLayoutManager(this@MainActivity)
            binding.rv.adapter = adapter

            mDbRef.child("user").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()
                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(User::class.java)
                        if (mAuth.currentUser?.uid != currentUser?.uid) {
                            userList.add(currentUser!!)
                        }
                    }

                    userList.sortByDescending { user ->
                        user.name?.equals("Umarov (Owner)", ignoreCase = true)
                    }
                    adapter.notifyDataSetChanged()

                    binding.progressBar.visibility = View.INVISIBLE
                    binding.rv.visibility = View.VISIBLE
                    binding.rv.startAnimation(scaleAnimation)
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.INVISIBLE
                }
            })

            // Get the current user's name
            val currentUserId = mAuth.currentUser?.uid
            currentUserId?.let {
                mDbRef.child("user").child(it)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val currentUser = snapshot.getValue(User::class.java)
                            currentUser?.name?.let { name ->
                                val toolbarTitle = "ChatApp ($name)"
                                binding.titleToolbar.text = toolbarTitle
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle database error
                        }
                    })
            }
        }

        binding.search.setOnClickListener {
            if (binding.searchEditText.visibility == View.VISIBLE) {
                binding.searchEditText.visibility = View.GONE
            } else {
                binding.searchEditText.visibility = View.VISIBLE
            }
        }
        binding.searchEditText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                }

                override fun afterTextChanged(s: Editable?) {
                    val searchText = s.toString().trim()
                    filterUserList(searchText)
                }
            })

        binding.clearSearch.setOnClickListener {
            binding.searchEditText.text.clear()
        }
    }

    private fun filterUserList(query: String) {
        val filteredList = userList.filter { user ->
            user.name!!.contains(query, ignoreCase = true)
        }
        adapter.filterList(filteredList)
    }
}