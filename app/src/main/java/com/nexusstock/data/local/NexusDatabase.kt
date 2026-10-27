package com.nexusstock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProductEntity::class], version = 1, exportSchema = false)
abstract class NexusDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        fun create(context: Context): NexusDatabase =
            Room.databaseBuilder(context, NexusDatabase::class.java, "nexusstock.db").build()
    }
}
