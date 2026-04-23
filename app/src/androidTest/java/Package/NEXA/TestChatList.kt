package Package.NEXA

import Package.NEXA.data.UserProfile
import Package.NEXA.logic.ConnectionManager
import Package.NEXA.logic.Message
import Package.NEXA.ui.chat.ChatList
import Package.NEXA.ui.chat.ChatScreen
import Package.NEXA.views.UserViewModel
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.junit.Assert.assertEquals
import androidx.navigation.testing.TestNavHostController

class FakeUserViewModel : UserViewModel() {
    // override only what you need for the test
    override fun selectUser(user: UserProfile) {
        // do nothing, just a stub
    }
    val addedMessages = mutableListOf<Message>()

    override fun addMessage(fromUser: String, message: Message) {
        addedMessages.add(message)
        addedMessages.add(message)
    }
}
@Composable
fun FakeChatListWithContacts(
    contacts: List<UserProfile>,
    navController: NavHostController = rememberNavController(),
    sharedUserViewModel: UserViewModel = FakeUserViewModel()
) {
    val testChat = linkedMapOf(
        "Bob" to mutableListOf<Message>(Message(UUID.randomUUID(),"Me","hi", Message.Status.SENT,true),
            Message(UUID.randomUUID(),"Bob","what's up", Message.Status.SENT,false))
    )
    val dummyUser0 = UserProfile(1111, UUID.randomUUID().toString(),"Me", "", testChat, "00:11:22:33:44:55")
    ChatList(
        navController = navController,
        sharedUserViewModel = sharedUserViewModel,
        // contactsProvider = { contacts }
    )
}

    @RunWith(AndroidJUnit4::class)
class TestChatList {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
        lateinit var navController: TestNavHostController
        val testChat = linkedMapOf(
            "Alice" to mutableListOf<Message>(Message(UUID.randomUUID(),"Alice","hi", Message.Status.RECEIVED,false),
                Message(UUID.randomUUID(),"Bob","what's up", Message.Status.SENT,true))
        )
        val dummyUser1 = UserProfile(1111, UUID.randomUUID().toString(),"Bob", "", testChat, "00:11:22:33:44:56")
        val dummyUser2 = UserProfile(1111, UUID.randomUUID().toString(),"Alice", "", testChat, "00:11:22:33:44:57")
    @Test
    fun testNoContactsDisplayed() {

        val sharedUserViewModel: UserViewModel = FakeUserViewModel()
        composeTestRule.setContent {
            ChatList(
                navController = rememberNavController(),
                sharedUserViewModel=sharedUserViewModel
            )
        }

        // Assert that the no contacts message is displayed
        composeTestRule.onNodeWithText("No contacts yet. Discover a device to start a chat.").assertIsDisplayed()
    }
    @Test
    fun testContactsShown(){
        val sharedUserViewModel: UserViewModel = FakeUserViewModel()
        composeTestRule.setContent {
            FakeChatListWithContacts(contacts = listOf(dummyUser1, dummyUser2), sharedUserViewModel = sharedUserViewModel)
        }
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
    }
        @Test
        fun testLastMessageShown(){
            val sharedUserViewModel: UserViewModel = FakeUserViewModel()
            composeTestRule.setContent {

                FakeChatListWithContacts(contacts = listOf(dummyUser1), sharedUserViewModel = sharedUserViewModel)
            }
            composeTestRule.onNodeWithText("what's up").assertIsDisplayed()
        }
        @Test
        fun testScreenChangesToChatScreen(){

            val fakeUserViewModel = FakeUserViewModel()
            val navController = TestNavHostController(composeTestRule.activity).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
                navigatorProvider.addNavigator(DialogNavigator())
            }

            composeTestRule.setContent {
                val connectionManager = ConnectionManager(LocalContext.current)

                NavHost(
                    navController = navController,
                    startDestination = "chatList"
                ){
                    composable("chatList"){
                        FakeChatListWithContacts(contacts = listOf(dummyUser1, dummyUser2), navController = navController,sharedUserViewModel = fakeUserViewModel)
                    }
                    composable("chat/00:11:22:33:44:57"){
                        ChatScreen(user = dummyUser2, dummyUser2.address!!,connectionManager,userViewModel = fakeUserViewModel)
                    }
                }
            }
            composeTestRule.onNodeWithText("Alice").performClick()
            assertEquals("chat/00:11:22:33:44:57",navController.currentDestination?.route)
        }
}