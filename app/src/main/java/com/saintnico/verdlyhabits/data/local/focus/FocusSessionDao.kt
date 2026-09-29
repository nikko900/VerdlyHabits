package com.saintnico.verdlyhabits.data.local.focus

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>
}
