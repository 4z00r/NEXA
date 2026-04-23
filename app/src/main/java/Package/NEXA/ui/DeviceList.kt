package Package.NEXA.ui

import Package.NEXA.data.UserProfile
import Package.NEXA.logic.ConnectionManager
import Package.NEXA.views.DeviceViewModel
import Package.NEXA.logic.Discover
import Package.NEXA.views.UserViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Device list screen to display list of discoverable devices
 * The user can select a device to connect to -> which then navigates to the chat screen
 * User can also make themselves discoverable to allow for incoming messages from other users
 * @param user user profile
 * @param navController navigation controller
 * @param connectionManager connection manager
 * @param discover discover class
 * @param context context
 * @param deviceViewModel device view model
 * @param userViewModel shared user view model
 */
@SuppressLint("MissingPermission") //suppress for device.name
@Composable
fun DeviceListScreen(
    user: UserProfile,
    navController: NavController,
    connectionManager: ConnectionManager,
    discover: Discover,
    context: Context = LocalContext.current,
    deviceViewModel: DeviceViewModel,
    userViewModel: UserViewModel
) {
    val devices by deviceViewModel.devices.collectAsState(initial = emptyMap())       // fetches the devices+user profile associated with it from the device view model

    Log.d("Device List Screen", user.username)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Discover button
        Button(
            onClick = {
                discover.findDevices()      // when button clicked -> start discovery
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Discover Devices") }

        Spacer(modifier = Modifier.height(16.dp))

        // Receive Connections button
        Button(
            onClick = {
                // Stop scanning before listening to avoid conflicts with incoming connections
                connectionManager.endSearch()
                connectionManager.bluetoothAdapter?.name = "NEXA-"+user.username   // adds prefix NEXA to device name for identification of registered users
                connectionManager.makeDiscoverable()                                // makes device discoverable
                Log.d("Device List Profile Broadcasted", connectionManager.bluetoothAdapter?.name.toString())
                connectionManager.listenForConnections()                           // starts listening for incoming connections
                Toast.makeText(context, "Waiting for incoming connections…", Toast.LENGTH_SHORT)
                    .show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Receive Connections") }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()

        LazyColumn {
            items(devices.toList()) { (profile, device) ->    // loops through devices and creates a list item for each
                Text(
                    text = "${device.name ?: "Unknown"} - ${device.address}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            userViewModel.selectUser(profile)                    // when clicked -> selected user stored in sharedUserViewModel
                            deviceViewModel.addUser(profile.username,device)       // add user to device view model
                            navController.navigate("chat/${device.address}")   // pass MAC address
                        }
                        .padding(16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        navController.navigate("chat/${device.address}")    // when user clicked -> opens chat with the user
                    }) {
                        Text("Connect")
                    }
                }
                Divider()
            }
        }
    }

}
