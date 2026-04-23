package Package.NEXA.views

import Package.NEXA.data.UserProfile
import Package.NEXA.data.userProfileRoomAccess
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Device view model class to hold the list of devices and their corresponding user profiles
 * @constructor Create empty Device view model
 * @property userProfiles data access object for user profiles
 * @property _devices private mutable state of devices mapped to user profiles
 * @property devices public read-only state (exposed to Composables) of devices mapped to user profiles
 */
class DeviceViewModel(private val userProfiles: userProfileRoomAccess) : ViewModel() {
    private val _devices = MutableStateFlow<Map<UserProfile, BluetoothDevice>>(emptyMap())
    val devices: StateFlow<Map<UserProfile, BluetoothDevice>> = _devices

    /**
     * Adds a device to the list of devices
     * @param profile user profile of the device
     * @param device device to add
     */
    fun addDevice(profile: UserProfile, device: BluetoothDevice) {
        _devices.value = _devices.value + (profile to device)    // maps profile -> device
    }
    fun addUser(username:String,device: BluetoothDevice) {
        viewModelScope.launch {
            val profile = userProfiles.fetchProfileByUsername(username)   // fetches profile from db
            if(profile!=null) {   // if already in db
                addDevice(profile,device)       // add to map
            }
            else {            // if not in db create new profile for the device
                val newProfile =
                    UserProfile(username = username, publicKey = "", address = device.address)

                addDevice(newProfile,device)
                Log.d("Device View Model", "Profile added to device list: $username")
            }
        }
    }
}