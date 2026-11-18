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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(persons: List<Person>)

    @Update
    suspend fun update(person: Person)

    @Query("UPDATE persons SET isDeleted = 1, lastModified = :modified WHERE id = :id")
    suspend fun softDelete(id: Long, modified: Long = System.currentTimeMillis())

    @Query("UPDATE persons SET isDeleted = 1, lastModified = :modified WHERE sharedId = :sharedId")
    suspend fun softDeleteBySharedId(sharedId: String, modified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM persons WHERE fileId = :fileId AND isDeleted = 0 ORDER BY sortOrder")
    suspend fun getActivePersonsList(fileId: Long): List<Person>

    @Query("UPDATE persons SET sortOrder = sortOrder + 1 WHERE fileId = :fileId AND isDeleted = 0 AND sortOrder >= :fromSortOrder")
    suspend fun shiftSortOrdersUp(fileId: Long, fromSortOrder: Int)

    @Query("UPDATE persons SET sortOrder = sortOrder - 1 WHERE fileId = :fileId AND isDeleted = 0 AND sortOrder > :fromSortOrder")
    suspend fun shiftSortOrdersDown(fileId: Long, fromSortOrder: Int)

    @Query("UPDATE persons SET sortOrder = sortOrder - 1 WHERE fileId = :fileId AND isDeleted = 0 AND sortOrder > :oldSortOrder AND sortOrder <= :newSortOrder")
    suspend fun shiftSortOrdersDownBetween(fileId: Long, oldSortOrder: Int, newSortOrder: Int)

    @Query("UPDATE persons SET sortOrder = sortOrder + 1 WHERE fileId = :fileId AND isDeleted = 0 AND sortOrder >= :newSortOrder AND sortOrder < :oldSortOrder")
    suspend fun shiftSortOrdersUpBetween(fileId: Long, newSortOrder: Int, oldSortOrder: Int)

    @androidx.room.Transaction
    suspend fun insertPersonAtPosition(person: Person, position: Int): Long {
        val targetSortOrder = position - 1
        shiftSortOrdersUp(person.fileId, targetSortOrder)
        return insert(person.copy(sortOrder = targetSortOrder))
    }

    @androidx.room.Transaction
    suspend fun updatePersonWithPosition(person: Person, oldSortOrder: Int, newSortOrder: Int) {
        if (oldSortOrder != newSortOrder) {
            if (oldSortOrder < newSortOrder) {
                shiftSortOrdersDownBetween(person.fileId, oldSortOrder, newSortOrder)
            } else {
                shiftSortOrdersUpBetween(person.fileId, newSortOrder, oldSortOrder)
            }
        }
        update(person.copy(sortOrder = newSortOrder, lastModified = System.currentTimeMillis()))
    }

    @androidx.room.Transaction
    suspend fun deletePersonWithShift(id: Long) {
        val person = getPersonById(id)
        if (person != null) {
            softDelete(id)
            shiftSortOrdersDown(person.fileId, person.sortOrder)
        }
    }

    @Query("SELECT * FROM persons WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedPersons(): Flow<List<Person>>

    @Query("UPDATE persons SET isDeleted = 0, lastModified = :modified WHERE id = :id")
    suspend fun restorePerson(id: Long, modified: Long = System.currentTimeMillis())

    @Query("SELECT * FROM persons WHERE fileId = :fileId AND isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedPersonsByFile(fileId: Long): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): Person?

    @Query("SELECT * FROM persons WHERE sharedId = :sharedId LIMIT 1")
    suspend fun getPersonBySharedId(sharedId: String): Person?

    @Query("SELECT * FROM persons WHERE sharedId = :sharedId LIMIT 1")
    fun getPersonBySharedIdFlow(sharedId: String): Flow<Person?>

    @Query("SELECT * FROM persons WHERE isDeleted = 0")
    suspend fun getAllPersonsList(): List<Person>

    @Query("SELECT * FROM persons")
    suspend fun getAllPersonsIncludingDeleted(): List<Person>

    @Query("""
        UPDATE persons 
        SET fileId = (SELECT id FROM loan_files WHERE loan_files.sharedId = persons.fileSharedId LIMIT 1) 
        WHERE fileSharedId IS NOT NULL 
          AND fileSharedId != '' 
          AND (SELECT id FROM loan_files WHERE loan_files.sharedId = persons.fileSharedId LIMIT 1) IS NOT NULL
    """)
    suspend fun relinkPersonsToLocalFiles(): Int

    @Query("DELETE FROM persons")
    suspend fun clearAll()
}
