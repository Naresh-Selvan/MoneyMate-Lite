package com.moneymate.lite.data.repository

import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val personDao: PersonDao
) {
    fun getPersonsByFile(fileId: Long): Flow<List<Person>> = personDao.getPersonsByFile(fileId)

    suspend fun insert(person: Person): Long = withContext(Dispatchers.IO) {
        personDao.insert(person)
    }

    suspend fun insertAtPosition(person: Person, position: Int): Long = withContext(Dispatchers.IO) {
        personDao.insertPersonAtPosition(person, position)
    }

    suspend fun update(person: Person) = withContext(Dispatchers.IO) {
        personDao.update(person)
    }

    suspend fun updateWithPosition(person: Person, oldSortOrder: Int, newSortOrder: Int) = withContext(Dispatchers.IO) {
        personDao.updatePersonWithPosition(person, oldSortOrder, newSortOrder)
    }

    suspend fun softDelete(id: Long) = withContext(Dispatchers.IO) {
        personDao.softDelete(id)
    }

    suspend fun deleteWithShift(id: Long) = withContext(Dispatchers.IO) {
        personDao.deletePersonWithShift(id)
    }

    fun getDeletedPersons(): Flow<List<Person>> = personDao.getDeletedPersons()

    suspend fun restorePerson(id: Long) = withContext(Dispatchers.IO) {
        personDao.restorePerson(id)
    }

    fun getDeletedPersonsByFile(fileId: Long): Flow<List<Person>> = personDao.getDeletedPersonsByFile(fileId)

    suspend fun getPersonById(id: Long): Person? = withContext(Dispatchers.IO) {
        personDao.getPersonById(id)
    }
}
