package com.example.housing.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.housing.model.Category
import com.example.housing.model.Payment
import com.example.housing.model.Purchase
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students")
    suspend fun getAllStudentsSync(): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): Student?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    @Query("SELECT COUNT(*) FROM purchase_members WHERE studentId = :studentId")
    suspend fun countStudentInPurchases(studentId: Long): Int

    @Query("SELECT COUNT(*) FROM payments WHERE payerStudentId = :studentId OR receiverStudentId = :studentId")
    suspend fun countStudentInPayments(studentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStudents(students: List<Student>)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT COUNT(*) FROM purchases WHERE categoryId = :categoryId")
    suspend fun countCategoryPurchases(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<Category>)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC, createdAt DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases")
    suspend fun getAllPurchasesSync(): List<Purchase>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: Long): Purchase?

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    fun getPurchaseFlow(id: Long): Flow<Purchase?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Update
    suspend fun updatePurchase(purchase: Purchase)

    @Delete
    suspend fun deletePurchase(purchase: Purchase)

    @Query("UPDATE purchases SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignPurchasesCategory(oldCategoryId: Long, newCategoryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchases(purchases: List<Purchase>)

    @Query("DELETE FROM purchases")
    suspend fun deleteAllPurchases()
}

@Dao
interface PurchaseMemberDao {
    @Query("SELECT * FROM purchase_members WHERE purchaseId = :purchaseId")
    fun getMembersForPurchase(purchaseId: Long): Flow<List<PurchaseMember>>

    @Query("SELECT * FROM purchase_members WHERE purchaseId = :purchaseId")
    suspend fun getMembersForPurchaseSync(purchaseId: Long): List<PurchaseMember>

    @Query("SELECT * FROM purchase_members")
    fun getAllMembersFlow(): Flow<List<PurchaseMember>>

    @Query("SELECT * FROM purchase_members")
    suspend fun getAllMembersSync(): List<PurchaseMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<PurchaseMember>)

    @Query("DELETE FROM purchase_members WHERE purchaseId = :purchaseId")
    suspend fun deleteMembersForPurchase(purchaseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMembers(members: List<PurchaseMember>)

    @Query("DELETE FROM purchase_members")
    suspend fun deleteAllMembers()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE purchaseId = :purchaseId ORDER BY createdAt ASC, id ASC")
    fun getPaymentsForPurchase(purchaseId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE purchaseId = :purchaseId ORDER BY createdAt ASC, id ASC")
    suspend fun getPaymentsForPurchaseSync(purchaseId: Long): List<Payment>

    @Query("SELECT * FROM payments ORDER BY createdAt ASC, id ASC")
    fun getAllPaymentsFlow(): Flow<List<Payment>>

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsSync(): List<Payment>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Long): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<Payment>)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}
