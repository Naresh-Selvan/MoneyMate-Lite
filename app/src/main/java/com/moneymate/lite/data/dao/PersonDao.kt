package com.moneymate.lite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneymate.lite.data.entity.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons WHERE fileId = :fileId AND isDeleted = 0 ORDER BY sortOrder")
    fun getPersonsByFile(fileId: Long): Flow<List<Person>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Query("UPDATE persons SET isDeleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("SELECT * FROM persons WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedPersons(): Flow<List<Person>>

    @Query("UPDATE persons SET isDeleted = 0 WHERE id = :id")
    suspend fun restorePerson(id: Long)

    @Query("SELECT * FROM persons WHERE fileId = :fileId AND isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedPersonsByFile(fileId: Long): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): Person?
}
