package com.example.kalkulatorwyplat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kalkulatorwyplat.data.dao.SubscriptionDao
import com.example.kalkulatorwyplat.data.entity.SubscriptionEntity

@Database(entities = [SubscriptionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kalkulator_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}