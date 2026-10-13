package com.kunalchauhan.safetrail.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name")
    fun getAll(): Flow<List<Contact>>

    @Insert
    suspend fun insert(contact: Contact)

    @Delete
    suspend fun delete(contact: Contact)
}