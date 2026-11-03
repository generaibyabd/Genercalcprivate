package com.example.housing.model

enum class BalanceStatus(val labelAr: String) {
    CREDITOR("له"),
    DEBTOR("عليه"),
    SETTLED("مسدد")
}

data class StudentBalance(
    val studentId: Long,
    val studentName: String,
    val contribution: Long,
    val share: Long,
    val paymentsSent: Long,
    val paymentsReceived: Long
) {
    val initialBalance: Long = contribution - share
    val currentBalance: Long = initialBalance + paymentsSent - paymentsReceived

    val status: BalanceStatus
        get() = when {
            currentBalance > 0L -> BalanceStatus.CREDITOR
            currentBalance < 0L -> BalanceStatus.DEBTOR
            else -> BalanceStatus.SETTLED
        }

    val remainingDebt: Long
        get() = if (currentBalance < 0L) -currentBalance else 0L

    val remainingEntitlement: Long
        get() = if (currentBalance > 0L) currentBalance else 0L
}

data class PurchaseFinancialSummary(
    val totalAmount: Long,
    val totalContributions: Long,
    val totalShares: Long,
    val totalSettled: Long,
    val remainingOutstanding: Long,
    val isSettled: Boolean
)

data class ProposedTransfer(
    val payerId: Long,
    val payerName: String,
    val receiverId: Long,
    val receiverName: String,
    val amount: Long
)

data class GlobalStudentSummary(
    val studentId: Long,
    val studentName: String,
    val isActive: Boolean,
    val totalContributions: Long,
    val totalShares: Long,
    val totalPaymentsSent: Long,
    val totalPaymentsReceived: Long,
    val totalReceivables: Long, // Sum of positive current balances across all purchases
    val totalDebts: Long        // Sum of absolute negative current balances across all purchases
) {
    val netPosition: Long get() = totalReceivables - totalDebts
}

data class StudentPurchaseBreakdown(
    val purchaseId: Long,
    val purchaseTitle: String,
    val purchaseDate: Long,
    val categoryName: String,
    val contribution: Long,
    val share: Long,
    val paymentsSent: Long,
    val paymentsReceived: Long,
    val currentBalance: Long,
    val status: BalanceStatus
)

data class DetailedPurchaseRecord(
    val purchase: Purchase,
    val category: Category?,
    val members: List<PurchaseMember>,
    val payments: List<Payment>,
    val balances: List<StudentBalance>,
    val summary: PurchaseFinancialSummary,
    val isSettled: Boolean
)
