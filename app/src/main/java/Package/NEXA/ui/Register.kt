package Package.NEXA.ui

import Package.NEXA.views.RegisterViewModel
import Package.NEXA.data.DbProfileProvider
import Package.NEXA.views.UserViewModel
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Registration screen for initial user registration
 * The user enters a username - which is passed into the registerViewModel to create a user profile
 * The user profile is then fetched from the registerViewModel and inserted into the Room database
 * @param navController navigation controller
 * @param userViewModel user view model
 * @param registerViewModel register view model
 */
@Composable
fun RegistrationScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    registerViewModel: RegisterViewModel = viewModel()
){
    var username by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }          // error flag for username input
    var error by remember { mutableStateOf(false) }          // error flag for username input
    val userProfile by registerViewModel.userProfile.collectAsState()     // user profile that has been created and stored in registerViewModel
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(                          // text field for entering username
            value = username,
            onValueChange = { input ->
                username = input
                error = input.any{!it.isLetterOrDigit() && !it.isWhitespace()&& it!='_' }  // checks if input is letter,digit,space or underscore
            },      // saves the inputted username
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii),
            isError = inputError!=null
        )
        if (error) {   // if input is invalid, display error message until input is valid
            Text(
                text = "Only letters,numbers and _ allowed",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { registerViewModel.regUser(username) }, // triggers registration w/ inputted username
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && !error                // button only allows user to press register button if username is entered and valid
        ) { Text("Register") }

        Spacer(modifier = Modifier.height(16.dp))

    }

    // When the registerViewModel produces a user, persist it then navigate to chat list
    LaunchedEffect(userProfile) {
        userProfile?.let { profileObj ->
            // Insert profile into UserProfile Room DB
            val appContext = context.applicationContext
            val db = DbProfileProvider.getDB(appContext)
            val dao = db.userProfileRoomAccess()
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                dao.insertProfile(profileObj)       // inserts the profile
            }

            // Save username for later lookups
            context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit {
                    putString("username", profileObj.username)         // saves username persistently (used in main activity)
                }
            userViewModel.setCurrentUser(profileObj)                   // sets the current user in the user view model

            // Navigate to chat list after registration is complete
            navController.navigate("chatList") {
                popUpTo("register") { inclusive = true } // removes register screen from back stack
                launchSingleTop = true // prevents copies of same screen being created
                restoreState = true    // restores state of screen
            }
        }
    }
}
