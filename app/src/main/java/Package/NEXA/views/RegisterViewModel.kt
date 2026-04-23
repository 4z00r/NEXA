package Package.NEXA.views

import Package.NEXA.cryptography.KeyManager
import Package.NEXA.data.UserProfile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Register view model class to create a new user profile
 * @constructor Create empty Register view model
 * @property userProfile this will hold the user who is currently logged in. Null is default.
 * @property user this is the private/read only version of userProfile so that it can be protected.
 */
class RegisterViewModel: ViewModel()
{
    val userProfile = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = userProfile

    /**
     * Register user
     * @param username username of the user
     */
    fun regUser(username : String)
    {
        viewModelScope.launch {
            KeyManager.generateKeyPair() //Generates the keys
            val publicKey = KeyManager.getPublicKey64() ?:""
            //Builds the user profile with needed attributes and the path.
            val profile = UserProfile(
                username = username,
                publicKey = publicKey,
            )
            //This then updates who is logged in.
            userProfile.value = profile
        }

    }
}