package com.example.housing.data

import androidx.room.withTransaction
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.Category
import com.example.housing.model.DetailedPurchaseRecord
import com.example.housing.model.GlobalStudentSummary
import com.example.housing.model.Payment
import com.example.housing.model.ProposedTransfer
import com.example.housing.model.Purchase
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import com.example.housing.model.StudentPurchaseBreakdown
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class HousingRepository(private val database: AppDatabase) {

    private val studentDao = database.studentDao()
    private val categoryDao = database.categoryDao()
    private val purchaseDao = database.purchaseDao()
    private val purchaseMemberDao = database.purchaseMemberDao()
    private val paymentDao = database.paymentDao()

    // ---------------- Students ----------------

    fun getAllStudents(): Flow<List<Student>> = studentDao.getAllStudents()
    fun getActiveStudents(): Flow<List<Student>> = studentDao.getActiveStudents()

    suspend fun addStudent(name: String): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الطالب."))
        }
        val student = Student(name = trimmed, isActive = true)
        return try {
            val id = studentDao.insertStudent(student)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameStudent(studentId: Long, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم الطالب الجديد."))
        }
        val existing = studentDao.getStudentById(studentId)
            ?: return Result.failure(IllegalArgumentException("الطالب غير موجود."))
        return try {
            studentDao.updateStudent(existing.copy(name = trimmed))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setStudentActive(studentId: Long, isActive: Boolean): Result<Unit> {
        val existing = studentDao.getStudentById(studentId)
            ?: return Result.failure(IllegalArgumentException("الطالب غير موجود."))
        return try {
            studentDao.updateStudent(existing.copy(isActive = isActive))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStudentSafely(student: Student): Result<Unit> {
        val purchasesCount = studentDao.countStudentInPurchases(student.id)
        val paymentsCount = studentDao.countStudentInPayments(student.id)
        if (purchasesCount > 0 || paymentsCount > 0) {
            return Result.failure(
                IllegalStateException("لا يمكن حذف الطالب نهائياً لوجود سجلات مالية مرتبطة به. يمكنك أرشفته بدلاً من ذلك.")
            )
        }
        return try {
            studentDao.deleteStudent(student)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------- Categories ----------------

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun ensureDefaultCategories() {
        val existing = categoryDao.getAllCategoriesSync()
        if (existing.isEmpty()) {
            for (cat in AppDatabase.DEFAULT_CATEGORIES) {
                categoryDao.insertCategory(cat)
            }
        }
    }

    suspend fun addCategory(name: String): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم التصنيف."))
        }
        val existing = categoryDao.getCategoryByName(trimmed)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("يوجد تصنيف بهذا الاسم مسبقاً."))
        }
        return try {
            val maxOrder = categoryDao.getAllCategoriesSync().maxOfOrNull { it.sortOrder } ?: 0
            val id = categoryDao.insertCategory(Category(name = trimmed, isDefault = false, sortOrder = maxOrder + 1))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameCategory(categoryId: Long, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم التصنيف الجديد."))
        }
        val existing = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalArgumentException("التصنيف غير موجود."))
        val duplicate = categoryDao.getCategoryByName(trimmed)
        if (duplicate != null && duplicate.id != categoryId) {
            return Result.failure(IllegalArgumentException("يوجد تصنيف آخر يحمل نفس هذا الاسم."))
        }
        return try {
            categoryDao.updateCategory(existing.copy(name = trimmed))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: Long): Result<Unit> {
        return database.withTransaction {
            val category = categoryDao.getCategoryById(categoryId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("التصنيف غير موجود."))

            // Find or create 'أخرى' fallback category
            var fallbackCategory = categoryDao.getCategoryByName("أخرى")
            if (fallbackCategory == null) {
                val fallbackId = categoryDao.insertCategory(Category(name = "أخرى", isDefault = true, sortOrder = 99))
                fallbackCategory = categoryDao.getCategoryById(fallbackId)
            }

            if (fallbackCategory != null && fallbackCategory.id != categoryId) {
                purchaseDao.reassignPurchasesCategory(categoryId, fallbackCategory.id)
            }

            categoryDao.deleteCategory(category)
            Result.success(Unit)
        }
    }

    // ---------------- Purchases & Members ----------------

    fun getAllPurchases(): Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    fun getPurchaseFlow(id: Long): Flow<Purchase?> = purchaseDao.getPurchaseFlow(id)

    fun getMembersForPurchase(purchaseId: Long): Flow<List<PurchaseMember>> =
        purchaseMemberDao.getMembersForPurchase(purchaseId)

    fun getPaymentsForPurchase(purchaseId: Long): Flow<List<Payment>> =
        paymentDao.getPaymentsForPurchase(purchaseId)

    suspend fun createPurchase(
        title: String,
        totalAmount: Long,
        purchaseDate: Long,
        categoryId: Long,
        notes: String?,
        splitMode: String,
        members: List<PurchaseMember>
    ): Result<Long> {
        return try {
            database.withTransaction {
                val purchase = Purchase(
                    title = title.trim(),
                    totalAmount = totalAmount,
                    purchaseDate = purchaseDate,
                    categoryId = categoryId,
                    notes = notes?.trim()?.ifEmpty { null },
                    splitMode = splitMode,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val purchaseId = purchaseDao.insertPurchase(purchase)
                val membersWithId = members.map { it.copy(purchaseId = purchaseId) }
                purchaseMemberDao.insertMembers(membersWithId)
                Result.success(purchaseId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePurchaseWithMembers(
        purchaseId: Long,
        title: String,
        totalAmount: Long,
        purchaseDate: Long,
        categoryId: Long,
        notes: String?,
        splitMode: String,
        newMembers: List<PurchaseMember>
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val existing = purchaseDao.getPurchaseById(purchaseId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("عملية الشراء غير موجودة."))

                val existingPayments = paymentDao.getPaymentsForPurchaseSync(purchaseId)
                val allStudents = studentDao.getAllStudentsSync().associateBy { it.id }

                val (isValid, errorMsg) = FinanceCalculations.validatePurchaseEditWithPayments(
                    newMembers = newMembers,
                    existingPayments = existingPayments,
                    students = allStudents
                )
                if (!isValid) {
                    return@withTransaction Result.failure(IllegalStateException(errorMsg ?: "التعديل يتعارض مع سجل الدفعات الحالي."))
                }

                val updatedPurchase = existing.copy(
                    title = title.trim(),
                    totalAmount = totalAmount,
                    purchaseDate = purchaseDate,
                    categoryId = categoryId,
                    notes = notes?.trim()?.ifEmpty { null },
                    splitMode = splitMode,
                    updatedAt = System.currentTimeMillis()
                )

                purchaseDao.updatePurchase(updatedPurchase)
                purchaseMemberDao.deleteMembersForPurchase(purchaseId)
                purchaseMemberDao.insertMembers(newMembers.map { it.copy(purchaseId = purchaseId) })

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePurchase(purchaseId: Long): Result<Unit> {
        return try {
            database.withTransaction {
                val existing = purchaseDao.getPurchaseById(purchaseId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("عملية الشراء غير موجودة."))

                // Room foreign keys cascade delete members and payments, but we ensure explicit clean deletion
                purchaseMemberDao.deleteMembersForPurchase(purchaseId)
                purchaseDao.deletePurchase(existing)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------- Payments ----------------

    suspend fun recordPayment(
        purchaseId: Long,
        payerId: Long,
        receiverId: Long,
        amount: Long
    ): Result<Long> {
        return try {
            database.withTransaction {
                val purchase = purchaseDao.getPurchaseById(purchaseId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("عملية الشراء غير موجودة."))
                val members = purchaseMemberDao.getMembersForPurchaseSync(purchaseId)
                val payments = paymentDao.getPaymentsForPurchaseSync(purchaseId)
                val students = studentDao.getAllStudentsSync().associateBy { it.id }

                val balances = FinanceCalculations.calculatePurchaseBalances(purchase, members, payments, students)
                val error = FinanceCalculations.validatePayment(amount, payerId, receiverId, balances)
                if (error != null) {
                    return@withTransaction Result.failure(IllegalArgumentException(error))
                }

                val payment = Payment(
                    purchaseId = purchaseId,
                    payerStudentId = payerId,
                    receiverStudentId = receiverId,
                    amount = amount,
                    createdAt = System.currentTimeMillis(),
                    cancelledAt = null
                )
                val id = paymentDao.insertPayment(payment)
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordMultiplePayments(
        purchaseId: Long,
        transfers: List<ProposedTransfer>
    ): Result<Unit> {
        if (transfers.isEmpty()) return Result.success(Unit)
        return try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val payments = transfers.mapIndexed { index, transfer ->
                    Payment(
                        purchaseId = purchaseId,
                        payerStudentId = transfer.payerId,
                        receiverStudentId = transfer.receiverId,
                        amount = transfer.amount,
                        createdAt = now + index,
                        cancelledAt = null
                    )
                }
                paymentDao.insertPayments(payments)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelPayment(paymentId: Long): Result<Unit> {
        return try {
            val payment = paymentDao.getPaymentById(paymentId)
                ?: return Result.failure(IllegalArgumentException("الدفعة غير موجودة."))
            if (payment.cancelledAt != null) {
                return Result.failure(IllegalStateException("هذه الدفعة ملغاة بالفعل مسبقاً."))
            }
            paymentDao.updatePayment(payment.copy(cancelledAt = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editPayment(
        paymentId: Long,
        newAmount: Long,
        newPayerId: Long,
        newReceiverId: Long
    ): Result<Unit> {
        return try {
            database.withTransaction {
                val payment = paymentDao.getPaymentById(paymentId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("الدفعة غير موجودة."))
                if (payment.cancelledAt != null) {
                    return@withTransaction Result.failure(IllegalStateException("لا يمكن تعديل دفعة ملغاة."))
                }
                if (newAmount <= 0L) {
                    return@withTransaction Result.failure(IllegalArgumentException("مبلغ الدفعة يجب أن يكون أكبر من صفر."))
                }
                if (newPayerId == newReceiverId) {
                    return@withTransaction Result.failure(IllegalArgumentException("لا يمكن للطالب الدفع لنفسه."))
                }

                val purchaseId = payment.purchaseId
                val purchase = purchaseDao.getPurchaseById(purchaseId)
                    ?: return@withTransaction Result.failure(IllegalArgumentException("عملية الشراء غير موجودة."))
                val members = purchaseMemberDao.getMembersForPurchaseSync(purchaseId)
                val allPayments = paymentDao.getPaymentsForPurchaseSync(purchaseId)
                val students = studentDao.getAllStudentsSync().associateBy { it.id }

                // Simulate all active payments replacing the targeted payment with the new values
                val proposedPayments = allPayments.map {
                    if (it.id == paymentId) {
                        it.copy(
                            amount = newAmount,
                            payerStudentId = newPayerId,
                            receiverStudentId = newReceiverId
                        )
                    } else it
                }

                val (isValid, errorMsg) = FinanceCalculations.validatePurchaseEditWithPayments(
                    newMembers = members,
                    existingPayments = proposedPayments,
                    students = students
                )

                if (!isValid) {
                    return@withTransaction Result.failure(IllegalStateException(errorMsg ?: "تعديل الدفعة يتعارض مع التسلسل المالي."))
                }

                paymentDao.updatePayment(
                    payment.copy(
                        amount = newAmount,
                        payerStudentId = newPayerId,
                        receiverStudentId = newReceiverId
                    )
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------- Reactive Aggregations ----------------

    /**
     * Flow of all purchases with detailed financial status and balances
     */
    fun getAllDetailedPurchasesFlow(): Flow<List<DetailedPurchaseRecord>> {
        return combine(
            purchaseDao.getAllPurchases(),
            categoryDao.getAllCategories(),
            purchaseMemberDao.getAllMembersFlow(),
            paymentDao.getAllPaymentsFlow(),
            studentDao.getAllStudents()
        ) { purchases, categories, allMembers, allPayments, allStudents ->
            val categoryMap = categories.associateBy { it.id }
            val studentMap = allStudents.associateBy { it.id }
            val membersByPurchase = allMembers.groupBy { it.purchaseId }
            val paymentsByPurchase = allPayments.groupBy { it.purchaseId }

            purchases.map { purchase ->
                val members = membersByPurchase[purchase.id] ?: emptyList()
                val payments = paymentsByPurchase[purchase.id] ?: emptyList()
                val balances = FinanceCalculations.calculatePurchaseBalances(
                    purchase = purchase,
                    members = members,
                    payments = payments,
                    students = studentMap
                )
                val summary = FinanceCalculations.computePurchaseSummary(purchase, balances, payments)

                DetailedPurchaseRecord(
                    purchase = purchase,
                    category = categoryMap[purchase.categoryId],
                    members = members,
                    payments = payments,
                    balances = balances,
                    summary = summary,
                    isSettled = summary.isSettled
                )
            }
        }
    }

    /**
     * Flow of global financial summary per student
     */
    fun getGlobalStudentSummariesFlow(): Flow<List<GlobalStudentSummary>> {
        return combine(
            studentDao.getAllStudents(),
            getAllDetailedPurchasesFlow()
        ) { students, detailedPurchases ->
            students.map { student ->
                var totalContributions = 0L
                var totalShares = 0L
                var totalPaymentsSent = 0L
                var totalPaymentsReceived = 0L
                var totalReceivables = 0L
                var totalDebts = 0L

                for (record in detailedPurchases) {
                    val studentBalance = record.balances.find { it.studentId == student.id }
                    if (studentBalance != null) {
                        totalContributions += studentBalance.contribution
                        totalShares += studentBalance.share
                        totalPaymentsSent += studentBalance.paymentsSent
                        totalPaymentsReceived += studentBalance.paymentsReceived

                        if (studentBalance.currentBalance > 0L) {
                            totalReceivables += studentBalance.currentBalance
                        } else if (studentBalance.currentBalance < 0L) {
                            totalDebts += -studentBalance.currentBalance
                        }
                    }
                }

                GlobalStudentSummary(
                    studentId = student.id,
                    studentName = student.name,
                    isActive = student.isActive,
                    totalContributions = totalContributions,
                    totalShares = totalShares,
                    totalPaymentsSent = totalPaymentsSent,
                    totalPaymentsReceived = totalPaymentsReceived,
                    totalReceivables = totalReceivables,
                    totalDebts = totalDebts
                )
            }
        }
    }

    /**
     * Get per-purchase breakdown for a specific student
     */
    suspend fun getStudentBreakdown(studentId: Long): List<StudentPurchaseBreakdown> {
        val detailedPurchases = getAllDetailedPurchasesFlow().first()
        return detailedPurchases.mapNotNull { record ->
            val bal = record.balances.find { it.studentId == studentId }
            if (bal != null && (bal.contribution > 0L || bal.share > 0L || bal.paymentsSent > 0L || bal.paymentsReceived > 0L)) {
                StudentPurchaseBreakdown(
                    purchaseId = record.purchase.id,
                    purchaseTitle = record.purchase.title,
                    purchaseDate = record.purchase.purchaseDate,
                    categoryName = record.category?.name ?: "عام",
                    contribution = bal.contribution,
                    share = bal.share,
                    paymentsSent = bal.paymentsSent,
                    paymentsReceived = bal.paymentsReceived,
                    currentBalance = bal.currentBalance,
                    status = bal.status
                )
            } else null
        }
    }

    // ---------------- Backup & Delete All ----------------

    suspend fun deleteAllData(): Result<Unit> {
        return try {
            database.withTransaction {
                paymentDao.deleteAllPayments()
                purchaseMemberDao.deleteAllMembers()
                purchaseDao.deleteAllPurchases()
                categoryDao.deleteAllCategories()
                studentDao.deleteAllStudents()

                // Re-seed default categories
                for (cat in AppDatabase.DEFAULT_CATEGORIES) {
                    categoryDao.insertCategory(cat)
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
