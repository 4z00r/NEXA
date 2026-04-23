package Package.NEXA

import Package.NEXA.data.UserProfile
import Package.NEXA.logic.ConnectionManager
import Package.NEXA.logic.Message
import Package.NEXA.ui.chat.ChatScreen
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@get:Rule
val composeTestRule = createAndroidComposeRule<ComponentActivity>()
class FakeConnectionManager(context: Context) : ConnectionManager(context) {
    var sentMessages = mutableListOf<Message>()
    override fun sendMessage(message: Message, device: BluetoothDevice) {
        sentMessages.add(message)
    }
}
@RunWith(AndroidJUnit4::class)
class TestChatScreen {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testTextState() {
        val dummyUser = UserProfile(1, "id1", "Bob", "",  hashMapOf(), "00:11:22:33:44:55")
        val fakeViewModel = FakeUserViewModel()
        val fakeConnectionManager = FakeConnectionManager(composeTestRule.activity)

        composeTestRule.setContent {
            ChatScreen(
                user = dummyUser,
                address = dummyUser.address!!,
                connectionManager = fakeConnectionManager,
                userViewModel = fakeViewModel
            )
        }

        // Input placeholder and send button should be visible
        composeTestRule.onNodeWithText("Type a message...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
    }

    @Test
    fun testTypingAndSendingMessage() {
        val dummyUser = UserProfile(1, "id1", "Bob", "",  hashMapOf(), "00:11:22:33:44:55")
        val fakeViewModel = FakeUserViewModel()
        val fakeConnectionManager = FakeConnectionManager(composeTestRule.activity)

        composeTestRule.setContent {
            ChatScreen(
                user = dummyUser,
                address = dummyUser.address!!,
                connectionManager = fakeConnectionManager,
                userViewModel = fakeViewModel
            )
        }
        val testMsg = "Hello!"
        composeTestRule.onNodeWithText("Type a message...").performTextInput(testMsg)
        // Type a message

        composeTestRule.waitForIdle()
        // Click Send
        composeTestRule.onNodeWithText("Send").performClick()
        composeTestRule.waitForIdle()

        // Assert message shown in LazyColumn
        composeTestRule.onNodeWithText(testMsg).performScrollTo().assertIsDisplayed()

        // Assert fakeConnectionManager captured the sent message
        assertEquals(1, fakeConnectionManager.sentMessages.size)
        assertEquals("Hello!", fakeConnectionManager.sentMessages.first().message)

        // Assert UserViewModel was updated
        //assertTrue(fakeViewModel.addedMessages.any { it.message == "Hello!" })
    }

    @Test
    fun testIncomingMessageDisplayed() {
        val dummyUser = UserProfile(1, "id1", "Bob", "", hashMapOf(), "00:11:22:33:44:55")
        val fakeViewModel = FakeUserViewModel()
        val fakeConnectionManager = FakeConnectionManager(composeTestRule.activity)
        lateinit var callback: ConnectionManager.ConnectionCallBack
        composeTestRule.setContent {
            val rememberedCallback = remember { mutableStateOf<ConnectionManager.ConnectionCallBack?>(null) }
            ChatScreen(
                user = dummyUser,
                address = dummyUser.address!!,
                connectionManager = fakeConnectionManager.apply {
                    connectionCallBack = object : ConnectionManager.ConnectionCallBack {
                        override fun onMsgRecieve(message: Message) {
                            rememberedCallback.value?.onMsgRecieve(message)
                        }
                    }
                },
                userViewModel = fakeViewModel
            )
            callback = object : ConnectionManager.ConnectionCallBack {
                override fun onMsgRecieve(message: Message) {
                    // simulate incoming messages
                }
            }
            rememberedCallback.value = callback
        }
        val incoming = Message(UUID.randomUUID(), "Alice", "Hi Bob", Message.Status.RECEIVED, false)
        composeTestRule.runOnUiThread {
            fakeConnectionManager.connectionCallBack?.onMsgRecieve(incoming)
        }
        composeTestRule.onNodeWithText("Hi Bob").assertIsDisplayed()
    }
}