package Package.NEXA.views

import Package.NEXA.data.UserProfile
import Package.NEXA.logic.Message
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * User view model class to hold the current user and selected user
 * @constructor Create empty User view model
 * @property selectedUser this will hold the user who is currently selected. Null is default.
 * @property _currentUser this is the private/read only version of userProfile so that it can be protected.
 * @property currentUser this is the public version of _currentUser.
 * @property messages this will hold the messages sent between users.
 */
open class UserViewModel : ViewModel() {
    private val selectedUser = MutableStateFlow<UserProfile?>(null)
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser
    private val messages = MutableStateFlow<Map<String, MutableList<Message>>>(emptyMap())

    val otherUser: StateFlow<UserProfile?> = selectedUser

    open fun selectUser(user: UserProfile) {
        selectedUser.value = user
    }

    /**
     * Sets the current user to given user profile
     * @param user user profile to set as current user
     */
    fun setCurrentUser(user: UserProfile) {
        _currentUser.value = user
    }

    /**
     * Adds a message to the messages map
     * @param fromUser user who sent the message
     * @param message message to add
     */
    open fun addMessage(fromUser: String, message: Message) {
        val updated = messages.value.toMutableMap()
        val list = updated.getOrPut(fromUser) { mutableListOf() }
        list.add(message)
        messages.value = updated
    }
}