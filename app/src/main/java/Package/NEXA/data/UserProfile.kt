package Package.NEXA.data

import Package.NEXA.logic.Message

import java.util.UUID
//import kotlinx.parcelize.Parcelize
import androidx.room.*

/**
 * Class that holds the Users id, username, publicKey and their avatar path.
 * Will be used to store in Room DB.
 */

/**
 * Class that contains the Entity to be stored the User Profile Database
 */
@Entity(tableName = "userProfiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) //Generate id for table
    val localID : Int = 0,
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val publicKey: String,
    var chatHistory: HashMap<String, MutableList<Message>> = hashMapOf(), //Need to change format to JSON in order to store in room.
    val address: String? = null //MAC address
)
{
    /**
     * Function to get the message history of the Author
     * @param author The author from which we want to get the message history for
     */
    fun returnMessageHistory(author: String): MutableList<Message> {
        return chatHistory.getOrPut(author) { mutableListOf() } // if author exists in the hashmap --> returns chat history. if no history --> creates new chat history
    }


}

