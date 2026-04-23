package Package.NEXA.views

import Package.NEXA.data.userProfileRoomAccess
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// Note: Generative AI used here to create the DeviceViewModelFactory class
/**
 * Device view model factory class to create a new device view model
 * @property userProfileDao user profile dao
 * @constructor Create empty Device view model factory
 */
class DeviceViewModelFactory(
    private val userProfileDao: userProfileRoomAccess // data access object for user profiles
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) { // checks if view model is of type device view model
            @Suppress("UNCHECKED_CAST")                             // removes warning -> isAssignableFrom tells us it's safe to cast DeviceViewModel to T
            return DeviceViewModel(userProfileDao) as T          // creates a new device view model with the given data access object as parameter
        }
        throw IllegalArgumentException("Unknown ViewModel class") // throws exception if view model is not of type device view model
    }
}