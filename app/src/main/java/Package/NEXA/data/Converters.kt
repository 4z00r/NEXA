package Package.NEXA.data
import androidx.room.*
import Package.NEXA.logic.Message
import com.google.gson.*
import com.google.gson.reflect.TypeToken

class Converters {

    /**
     * Function that converts the map containing the Chat History to JSON String
     * @param chat The chat history stored in the HashMap needed to be converted
     * Allows storage in the User Profile Database
     * converts the map containing the Chat History to JSON String
     */
    @TypeConverter
    fun fromChatHist(chat: HashMap<String, MutableList<Message>>?): String {
        return Gson().toJson(chat)
    }

    /**
     * Function that converts the map containing the Chat History to JSON String
     * @param chat The chat we wish to convert to a HashMap
     */
    @TypeConverter
    fun toChatHist(chat: String): HashMap<String, MutableList<Message>>{
        val map = object : TypeToken<HashMap<String, MutableList<Message>>>() {}.type //Ensure correct nested types are used
        return Gson().fromJson(chat, map)

    }
}