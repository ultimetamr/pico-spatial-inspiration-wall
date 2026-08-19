package com.spatialapps.inspirationwall.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WallDao {
    @Query("SELECT * FROM walls ORDER BY updatedAt DESC")
    fun observeWalls(): Flow<List<WallEntity>>

    @Query("SELECT * FROM groups ORDER BY wallId, orderIndex")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM cards WHERE deleted = 0 ORDER BY wallId, zIndex")
    fun observeCards(): Flow<List<CardEntity>>

    @Upsert
    suspend fun upsertWall(wall: WallEntity)

    @Upsert
    suspend fun upsertGroup(group: GroupEntity)

    @Upsert
    suspend fun upsertCard(card: CardEntity)

    @Upsert
    suspend fun upsertCards(cards: List<CardEntity>)

    @Query("SELECT COUNT(*) FROM walls")
    suspend fun wallCount(): Int

    @Query("UPDATE cards SET deleted = 1, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun softDeleteCard(cardId: String, updatedAt: Long)

    @Query("UPDATE cards SET deleted = 0, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun restoreCard(cardId: String, updatedAt: Long)

    @Query("DELETE FROM cards WHERE deleted = 1 AND updatedAt < :deadline")
    suspend fun purgeDeleted(deadline: Long)
}

@Database(
    entities = [WallEntity::class, GroupEntity::class, CardEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WallDatabase : RoomDatabase() {
    abstract fun wallDao(): WallDao

    companion object {
        @Volatile private var instance: WallDatabase? = null

        fun get(context: Context): WallDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WallDatabase::class.java,
                "inspiration-wall.db",
            ).build().also { instance = it }
        }
    }
}
