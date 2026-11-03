package com.example.housing.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.housing.model.Category
import com.example.housing.model.Payment
import com.example.housing.model.Purchase
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Student::class,
        Category::class,
        Purchase::class,
        PurchaseMember::class,
        Payment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseMemberDao(): PurchaseMemberDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "housing_expenses.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            Category(name = "طعام", isDefault = true, sortOrder = 1),
            Category(name = "شراب", isDefault = true, sortOrder = 2),
            Category(name = "منظفات", isDefault = true, sortOrder = 3),
            Category(name = "مستلزمات", isDefault = true, sortOrder = 4),
            Category(name = "أخرى", isDefault = true, sortOrder = 5)
        )
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val categoryDao = database.categoryDao()
                    for (cat in DEFAULT_CATEGORIES) {
                        categoryDao.insertCategory(cat)
                    }
                }
            }
        }
    }
}
