package com.example.housing.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.housing.data.AppDatabase
import com.example.housing.data.BackupManager
import com.example.housing.data.HousingRepository
import com.example.housing.model.Category
import com.example.housing.model.DetailedPurchaseRecord
import com.example.housing.model.GlobalStudentSummary
import com.example.housing.model.ProposedTransfer
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import com.example.housing.model.StudentPurchaseBreakdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PurchaseSort(val labelAr: String) {
    DATE_DESC("الأحدث أولاً"),
    DATE_ASC("الأقدم أولاً"),
    AMOUNT_DESC("المبلغ الأكبر"),
    AMOUNT_ASC("المبلغ الأقل")
}

enum class PurchaseStatusFilter(val labelAr: String) {
    ALL("الكل"),
    UNSETTLED("غير مكتملة"),
    SETTLED("مكتملة")
}

data class DashboardUiState(
    val totalSpending: Long = 0L,
    val purchasesCount: Int = 0,
    val unsettledCount: Int = 0,
    val totalOutstandingDebts: Long = 0L,
    val totalOutstandingReceivables: Long = 0L,
    val recentPurchases: List<DetailedPurchaseRecord> = emptyList(),
    val activeStudentCount: Int = 0
)

class HousingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = HousingRepository(database)

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    val allStudents: StateFlow<List<Student>> = repository.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStudents: StateFlow<List<Student>> = repository.getActiveStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDetailedPurchases: StateFlow<List<DetailedPurchaseRecord>> =
        repository.getAllDetailedPurchasesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val globalStudentSummaries: StateFlow<List<GlobalStudentSummary>> =
        repository.getGlobalStudentSummariesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------- Dashboard ----------------
    val dashboardState: StateFlow<DashboardUiState> = combine(
        allDetailedPurchases,
        activeStudents
    ) { purchases, students ->
        val totalSpending = purchases.sumOf { it.purchase.totalAmount }
        val purchasesCount = purchases.size
        val unsettledCount = purchases.count { !it.isSettled }

        // Total outstanding across all purchases
        var totalDebts = 0L
        var totalReceivables = 0L
        for (p in purchases) {
            for (bal in p.balances) {
                if (bal.currentBalance > 0L) totalReceivables += bal.currentBalance
                else if (bal.currentBalance < 0L) totalDebts += -bal.currentBalance
            }
        }

        val recent = purchases.take(5)

        DashboardUiState(
            totalSpending = totalSpending,
            purchasesCount = purchasesCount,
            unsettledCount = unsettledCount,
            totalOutstandingDebts = totalDebts,
            totalOutstandingReceivables = totalReceivables,
            recentPurchases = recent,
            activeStudentCount = students.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    // ---------------- Purchases List Filters ----------------
    val searchQuery = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Long?>(null)
    val statusFilter = MutableStateFlow(PurchaseStatusFilter.ALL)
    val sortOption = MutableStateFlow(PurchaseSort.DATE_DESC)

    val filteredPurchases: StateFlow<List<DetailedPurchaseRecord>> = combine(
        allDetailedPurchases,
        searchQuery,
        selectedCategoryId,
        statusFilter,
        sortOption
    ) { purchases, query, catId, status, sort ->
        var list = purchases

        if (query.isNotBlank()) {
            list = list.filter { it.purchase.title.contains(query.trim(), ignoreCase = true) }
        }

        if (catId != null) {
            list = list.filter { it.purchase.categoryId == catId }
        }

        list = when (status) {
            PurchaseStatusFilter.ALL -> list
            PurchaseStatusFilter.UNSETTLED -> list.filter { !it.isSettled }
            PurchaseStatusFilter.SETTLED -> list.filter { it.isSettled }
        }

        list = when (sort) {
            PurchaseSort.DATE_DESC -> list.sortedWith(
                compareByDescending<DetailedPurchaseRecord> { it.purchase.purchaseDate }
                    .thenByDescending { it.purchase.createdAt }
            )
            PurchaseSort.DATE_ASC -> list.sortedWith(
                compareBy<DetailedPurchaseRecord> { it.purchase.purchaseDate }
                    .thenBy { it.purchase.createdAt }
            )
            PurchaseSort.AMOUNT_DESC -> list.sortedByDescending { it.purchase.totalAmount }
            PurchaseSort.AMOUNT_ASC -> list.sortedBy { it.purchase.totalAmount }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------- Student Actions ----------------
    fun addStudent(name: String, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            val res = repository.addStudent(name)
            onResult(res)
        }
    }

    fun renameStudent(studentId: Long, newName: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.renameStudent(studentId, newName)
            onResult(res)
        }
    }

    fun toggleStudentActive(studentId: Long, isActive: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.setStudentActive(studentId, isActive)
            onResult(res)
        }
    }

    fun deleteStudentSafely(student: Student, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteStudentSafely(student)
            onResult(res)
        }
    }

    // ---------------- Category Actions ----------------
    fun addCategory(name: String, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            val res = repository.addCategory(name)
            onResult(res)
        }
    }

    fun renameCategory(categoryId: Long, newName: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.renameCategory(categoryId, newName)
            onResult(res)
        }
    }

    fun deleteCategory(categoryId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteCategory(categoryId)
            onResult(res)
        }
    }

    // ---------------- Purchase Actions ----------------
    fun createPurchase(
        title: String,
        totalAmount: Long,
        purchaseDate: Long,
        categoryId: Long,
        notes: String?,
        splitMode: String,
        members: List<PurchaseMember>,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createPurchase(
                title = title,
                totalAmount = totalAmount,
                purchaseDate = purchaseDate,
                categoryId = categoryId,
                notes = notes,
                splitMode = splitMode,
                members = members
            )
            onResult(res)
        }
    }

    fun updatePurchase(
        purchaseId: Long,
        title: String,
        totalAmount: Long,
        purchaseDate: Long,
        categoryId: Long,
        notes: String?,
        splitMode: String,
        members: List<PurchaseMember>,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updatePurchaseWithMembers(
                purchaseId = purchaseId,
                title = title,
                totalAmount = totalAmount,
                purchaseDate = purchaseDate,
                categoryId = categoryId,
                notes = notes,
                splitMode = splitMode,
                newMembers = members
            )
            onResult(res)
        }
    }

    fun deletePurchase(purchaseId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deletePurchase(purchaseId)
            onResult(res)
        }
    }

    // ---------------- Payment Actions ----------------
    fun recordPayment(
        purchaseId: Long,
        payerId: Long,
        receiverId: Long,
        amount: Long,
        onResult: (Result<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordPayment(purchaseId, payerId, receiverId, amount)
            onResult(res)
        }
    }

    fun recordMultiplePayments(
        purchaseId: Long,
        transfers: List<ProposedTransfer>,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordMultiplePayments(purchaseId, transfers)
            onResult(res)
        }
    }

    fun cancelPayment(paymentId: Long, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.cancelPayment(paymentId)
            onResult(res)
        }
    }

    fun editPayment(
        paymentId: Long,
        newAmount: Long,
        newPayerId: Long,
        newReceiverId: Long,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.editPayment(paymentId, newAmount, newPayerId, newReceiverId)
            onResult(res)
        }
    }

    // ---------------- Breakdown Lookup ----------------
    suspend fun getStudentBreakdown(studentId: Long): List<StudentPurchaseBreakdown> {
        return repository.getStudentBreakdown(studentId)
    }

    // ---------------- Backup & Restore ----------------
    suspend fun exportBackupJson(): String {
        return BackupManager.exportToJson(database)
    }

    suspend fun importBackupJson(jsonString: String): Result<Unit> {
        return BackupManager.importFromJson(database, jsonString)
    }

    fun deleteAllData(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteAllData()
            onResult(res)
        }
    }
}
