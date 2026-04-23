package Package.NEXA.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data access object used with objects in the roomDB
 * Basic interface used for RoomDb that will fetch profiles, delete, update etc
 * Basic sql operations
 */
@Dao
interface userProfileRoomAccess
{
    /**
     * Function to get specific device/user by MAC address
     */
    @Query("Select * from userProfiles where address = :address Limit 1")
    suspend fun fetchProfileByAddress(address : String) : UserProfile?

    /**
     * Function to get specific userProfile from their username
     */
    @Query("SELECT * FROM userProfiles WHERE username = :username LIMIT 1")
    suspend fun fetchProfileByUsername(username: String): UserProfile?

    /**
     * Function to get insert a UserProfile into the Database
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile) //Insert a new profile into the DB

    /**
     * Function to get specific all userProfiles from the DB
     */
    @Query("Select * from userProfiles")
    suspend fun fetchAllProfiles(): List<UserProfile> //Will fetch all profiles from the DB

    @Query("SELECT * FROM userProfiles")
    fun fetchAllProfilesFlow(): Flow<List<UserProfile>>
    @Delete
    suspend fun delete(profile: UserProfile)

    @Query("Update userProfiles set publicKey = :pubKey where address = :address")
    suspend fun updatePubKey(address: String, pubKey: String)
}