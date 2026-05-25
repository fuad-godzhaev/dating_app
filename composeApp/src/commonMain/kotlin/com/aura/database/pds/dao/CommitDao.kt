package com.aura.database.pds.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aura.database.pds.entities.CommitEntity

@Dao
interface CommitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommit(commit: CommitEntity)

    @Query("SELECT * FROM commits WHERE id = 1")
    suspend fun getLatestCommit(): CommitEntity?
}