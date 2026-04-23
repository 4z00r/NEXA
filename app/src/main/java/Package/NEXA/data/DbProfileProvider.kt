package Package.NEXA.data
import android.content.Context
import androidx.room.*

/**
 * Object to ensure a single instance of the Profile Database is created
 */
object DbProfileProvider {
    @Volatile
    private var INSTANCE: NexaProfileDB? = null

    /**
     * Function to return the NexaProfileDB
     * @param context The current context of the system
     */
    fun getDB(context: Context): NexaProfileDB {
        return INSTANCE ?: synchronized(this) { //check if an instance of the DB is already created
            val instance = Room.databaseBuilder(  //uses built in Room library function to create the DB
                context.applicationContext,
                NexaProfileDB::class.java, //References the Room Database class
                "NexaUserProfile.db"   // unified DB file name
            ).fallbackToDestructiveMigration().build()
            INSTANCE = instance
            instance
        }
    }
}
