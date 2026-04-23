package Package.NEXA

import Package.NEXA.cryptography.KeyManager
import Package.NEXA.logic.ConnectionManager
import Package.NEXA.logic.Discover
import Package.NEXA.ui.DeviceListScreen
import Package.NEXA.ui.NavBar
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import Package.NEXA.ui.RegistrationScreen
import Package.NEXA.ui.chat.ChatList
import Package.NEXA.ui.chat.ChatScreen
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import Package.NEXA.data.DbProfileProvider
import Package.NEXA.views.DeviceViewModel
import Package.NEXA.views.DeviceViewModelFactory
import Package.NEXA.views.UserViewModel
import Package.NEXA.ui.ProfileScreen
import androidx.activity.viewModels
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main activity class used to launch the app
 * @constructor Create empty Main activity
 * @property discover discover object used to find other bluetooth devices
 * @property connectionManager connection manager object to create connection between two users
 * @property deviceViewModel view model to select current user + 1 other user
 * @property db initialising the room db for user profiles
 * @property userProfiles user profile dao
 * @property TAG tag for logging
 */
class MainActivity() : ComponentActivity(), Discover.DiscoverCallBack {
    private lateinit var discover: Discover
    private lateinit var connectionManager: ConnectionManager
    private lateinit var deviceViewModel: DeviceViewModel
    private val db by lazy { DbProfileProvider.getDB(this) }
    private val userProfiles by lazy { db.userProfileRoomAccess() }
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // instantiates connectionManager and discover objects
        connectionManager = ConnectionManager(this)
        discover = Discover(this, connectionManager)
        // factory instantiates deviceViewModel w/ dao for user profiles passed as parameter
        val factory = DeviceViewModelFactory(userProfiles)
        deviceViewModel = ViewModelProvider(this,factory)[DeviceViewModel::class.java] // Get the ViewModel scoped to this Activity
        val userViewModel: UserViewModel by viewModels()

        discover.setDiscoverCallBack(this) // for DiscoverCallBack interface implementation
        discover.addContact()      // sends out devices intent to connect

        lifecycleScope.launch(Dispatchers.IO) {
            if (KeyManager.getPrivKey() == null) {
                KeyManager.generateKeyPair()            // generates key pair if it doesn't already exist
                Log.d(TAG, "KeyPair generated")
            } else {
                Log.d(TAG, "KeyPair already exists")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {      // checks if build version of device is greater than or equal to android 12 (API level 31)
            ActivityCompat.requestPermissions(                     // needs to request permissions if android 12 or above
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1001
            )
        } else {
            ActivityCompat.requestPermissions(          // Android 11 and below permissions needed
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                1002
            )
        }

        setContent {
            val context = LocalContext.current
            var savedUsername by remember { mutableStateOf<String?>(null) }   // current user's username if registered

            LaunchedEffect(Unit) {
                val sharedPref = context.getSharedPreferences("user_prefs", MODE_PRIVATE) // gets a shared preferences object
                savedUsername = sharedPref.getString("username", null)              /// gets username from shared preferences
                savedUsername?.let{ username->
                    val profile = userProfiles.fetchProfileByUsername(username)             // checks if username in db
                    profile?.let{ userViewModel.setCurrentUser(it) }                  // if in db, set as current user
                }
            }

            MaterialTheme {
                val user by userViewModel.currentUser.collectAsState()            // gets current user from user view model

                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()             // creates a nav controller for screens navigation
                    val connectionManager = ConnectionManager(this)
                    val context = LocalContext.current

                    Scaffold(
                        bottomBar = {
                            if (user!=null)
                                NavBar(navController)   // if user logged in -> create navigation bar
                        }
                    ) { innerPadding ->
                        NavHost(                // creates a nav host to build and host the navigation graph
                            navController = navController,
                            startDestination =
                                when {
                                    (savedUsername == null)-> "register"      // if user not logged in -> registration screen
                                    user ==null ->"loading"                 // if user logged in but not in db -> loading screen until db is updated
                                    else->"chatList"                        // if user logged in and in db -> chat list screen
                                },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            // navigation graph for different screens
                            composable("register") {
                                RegistrationScreen(navController,userViewModel)
                            }
                            composable("profileScreen") {
                                user?.let{ user->
                                    ProfileScreen(user)
                                }

                            }
                            composable("chatList") {
                                user?.let { user->
                                    ChatList(navController, userViewModel)
                                }

                            }
                            composable("deviceList") {
                                DeviceListScreen(
                                    user!!,
                                    navController,
                                    connectionManager,
                                    discover,
                                    context,
                                    deviceViewModel,
                                    userViewModel
                                )
                            }
                            composable("chat/{address}") { backStackEntry ->   // back stack stores address of other user
                                val address = backStackEntry.arguments?.getString("address")   // retrieves address from back stack
                                    ?: return@composable                // if address null then exit composable
                                ChatScreen(
                                    user!!,
                                    address = address,
                                    connectionManager = connectionManager,
                                    userViewModel = userViewModel
                                )
                            }
                            composable("loading") {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * User found callback method for when user profile found during discovery
     * @param user user profile username found
     * @param device device found
     */
    override fun userFound(user: String, device: BluetoothDevice) {
        deviceViewModel.addUser(user,device)     // adds user to device view model
        Log.d(TAG, "User found $user and added to view model")
    }

    /**
     * when program terminated - this method is executed
     * unregisters discoverReceiver
     */
    override fun onDestroy() {
        super.onDestroy()
        discover.deleteContact()
    }
}