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
