package Package.NEXA.data

import androidx.room.*

/**
 * Actual db that stores UserProfiles and associated objects
 */
@Database(entities = [UserProfile::class, Waiting::class], version = 2, exportSchema = false)
@TypeConverters(Converters ::class)
abstract class NexaProfileDB : RoomDatabase()
{
    /**
     * Function that access the UserProfileRoomAccess class (DAO)
     */
    abstract fun userProfileRoomAccess() : userProfileRoomAccess

    /**
     * Function that access the waitingRoomAccess class (DAO)
     */
    abstract fun waitingRoomAccess(): WaitingRoomAccess
}