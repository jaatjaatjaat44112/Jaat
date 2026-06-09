package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val statusText: String,
    val isMe: Boolean = false,
    val online: Boolean = true,
    val joinedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val senderId: String,
    val senderName: String,
    val senderImageUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMe: Boolean = false
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val memberName: String,
    val memberImageUrl: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY joinedTimestamp DESC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Query("SELECT * FROM members WHERE isMe = 1 LIMIT 1")
    suspend fun getMe(): MemberEntity?

    @Query("DELETE FROM members WHERE isMe = 1")
    suspend fun deleteMe()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Query("DELETE FROM stories")
    suspend fun deleteAllStories()
}

@Database(entities = [MemberEntity::class, MessageEntity::class, StoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun messageDao(): MessageDao
    abstract fun storyDao(): StoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phache_gang_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
