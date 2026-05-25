package com.aura.database.pds.dao

import androidx.room.Dao
import androidx.room.Query
import com.aura.database.pds.entities.RecordEntity

@Dao
interface ProfileDao {

    //Get the local user's profile.
    @Query("""
        SELECT * FROM records 
        WHERE collection = 'com.aura.records.profile' AND rkey = 'self'
    """)
    suspend fun getMyProfile(): RecordEntity?

    //Check if a profile exists
    @Query("""
        SELECT COUNT(*) FROM records 
        WHERE collection = 'com.aura.records.profile' AND rkey = 'self'
    """)
    suspend fun hasProfile(): Boolean
}

//TODO: Delete this file?