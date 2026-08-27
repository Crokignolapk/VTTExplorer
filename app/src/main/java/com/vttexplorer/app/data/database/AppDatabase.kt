package com.vttexplorer.app.data.database

import androidx.room.*
import com.vttexplorer.app.domain.model.RideEntity
import com.vttexplorer.app.domain.model.RideSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT id, name, dateMillis, distanceMeters, durationSeconds, elevationGain FROM rides ORDER BY dateMillis DESC")
    fun getAllSummaries(): Flow<List<RideSummary>>

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getById(id: Long): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ride: RideEntity): Long

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE rides SET name = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)
}

@Database(entities = [RideEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
}
