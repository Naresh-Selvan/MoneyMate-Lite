package com.moneymate.lite.di

import android.content.Context
import androidx.room.Room
import com.moneymate.lite.data.AppDatabase
import com.moneymate.lite.data.dao.ActivityLogDao
import com.moneymate.lite.data.dao.ApprovalRequestDao
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.dao.WorkerProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "moneymate_lite.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    try {
                        db.execSQL("""
                            UPDATE loans 
                            SET isCompleted = 1, completedAt = dateGiven, lastModified = strftime('%s', 'now') * 1000 
                            WHERE isCompleted = 0 AND isDeleted = 0 
                              AND id < (
                                  SELECT MAX(l2.id) 
                                  FROM loans l2 
                                  WHERE l2.personId = loans.personId 
                                    AND l2.isCompleted = 0 
                                    AND l2.isDeleted = 0
                              )
                        """.trimIndent())

                        // 1. Relink any orphan loans pointing to duplicate zero-balance person records to the primary person record
                        db.execSQL("""
                            UPDATE loans 
                            SET personId = (
                                SELECT p2.id 
                                FROM persons p2 
                                WHERE p2.fileId = (SELECT fileId FROM persons WHERE persons.id = loans.personId) 
                                  AND p2.isDeleted = 0 
                                  AND p2.id != loans.personId 
                                  AND TRIM(RTRIM(p2.name, '- ')) = TRIM(RTRIM((SELECT name FROM persons WHERE persons.id = loans.personId), '- ')) 
                                ORDER BY p2.sortOrder DESC, p2.id ASC 
                                LIMIT 1
                            ) 
                            WHERE isDeleted = 0 
                              AND personId IN (
                                  SELECT p1.id 
                                  FROM persons p1 
                                  WHERE p1.isDeleted = 0 
                                    AND p1.sortOrder = 0 
                                    AND EXISTS (
                                        SELECT 1 FROM persons p2 
                                        WHERE p2.fileId = p1.fileId 
                                          AND p2.isDeleted = 0 
                                          AND p2.id != p1.id 
                                          AND p2.sortOrder > 0 
                                          AND TRIM(RTRIM(p2.name, '- ')) = TRIM(RTRIM(p1.name, '- '))
                                    )
                              )
                        """.trimIndent())

                        // 2. Soft-delete duplicate person records (sortOrder = 0 or no active loans) where an active customer with same name exists
                        db.execSQL("""
                            UPDATE persons 
                            SET isDeleted = 1, syncStatus = 'PENDING_SYNC', lastModified = strftime('%s', 'now') * 1000 
                            WHERE isDeleted = 0 
                              AND TRIM(RTRIM(name, '- ')) IN (
                                  SELECT TRIM(RTRIM(p2.name, '- ')) 
                                  FROM persons p2 
                                  WHERE p2.fileId = persons.fileId 
                                    AND p2.isDeleted = 0 
                                    AND p2.id != persons.id 
                                    AND (
                                        p2.sortOrder > persons.sortOrder 
                                        OR EXISTS (SELECT 1 FROM loans l2 WHERE l2.personId = p2.id AND l2.isDeleted = 0)
                                    )
                              ) 
                              AND NOT EXISTS (
                                  SELECT 1 FROM loans l1 WHERE l1.personId = persons.id AND l1.isDeleted = 0
                              )
                        """.trimIndent())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideLoanFileDao(database: AppDatabase): LoanFileDao {
        return database.loanFileDao()
    }

    @Provides
    @Singleton
    fun providePersonDao(database: AppDatabase): PersonDao {
        return database.personDao()
    }

    @Provides
    @Singleton
    fun provideLoanDao(database: AppDatabase): LoanDao {
        return database.loanDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    @Singleton
    fun provideApprovalRequestDao(database: AppDatabase): ApprovalRequestDao {
        return database.approvalRequestDao()
    }

    @Provides
    @Singleton
    fun provideActivityLogDao(database: AppDatabase): ActivityLogDao {
        return database.activityLogDao()
    }

    @Provides
    @Singleton
    fun provideWorkerProfileDao(database: AppDatabase): WorkerProfileDao {
        return database.workerProfileDao()
    }
}
