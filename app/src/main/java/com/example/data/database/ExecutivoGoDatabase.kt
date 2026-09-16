package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.entity.*

@Database(
    entities = [
        CompanyEntity::class,
        UserEntity::class,
        DriverEntity::class,
        PaymentMethodEntity::class,
        TripEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExecutivoGoDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun userDao(): UserDao
    abstract fun driverDao(): DriverDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: ExecutivoGoDatabase? = null

        fun getInstance(context: Context): ExecutivoGoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExecutivoGoDatabase::class.java,
                    "executivo_go_clean.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
