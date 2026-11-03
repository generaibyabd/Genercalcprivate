package com.example.housing.logic

import com.example.housing.model.BalanceStatus
import com.example.housing.model.Payment
import com.example.housing.model.ProposedTransfer
import com.example.housing.model.Purchase
import com.example.housing.model.PurchaseFinancialSummary
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import com.example.housing.model.StudentBalance
import java.text.NumberFormat
import java.util.Locale

object FinanceCalculations {

    /**
     * Format monetary amounts in Iraqi Dinar (e.g., "30,000 د.ع")
     */
    fun formatCurrency(amount: Long): String {
        val formatted = NumberFormat.getNumberInstance(Locale.US).format(amount)
        return "$formatted د.ع"
    }

    /**
     * Deterministically calculates equal shares for selected participants.
     * Any remainder (totalAmount % count) is distributed 1 dinar at a time
     * in stable participant order (sorted by participant ID).
     */
    fun calculateEqualShares(totalAmount: Long, participantIds: List<Long>): Map<Long, Long> {
        if (participantIds.isEmpty() || totalAmount <= 0L) return emptyMap()

        val sortedIds = participantIds.sorted()
        val count = sortedIds.size
        val baseShare = totalAmount / count
        val remainder = (totalAmount % count).toInt()

        val result = mutableMapOf<Long, Long>()
        sortedIds.forEachIndexed { index, studentId ->
            val share = if (index < remainder) baseShare + 1L else baseShare
            result[studentId] = share
        }
        return result
    }

    /**
     * Calculates the balance for every student involved in a purchase.
     * Only active payments (cancelledAt == null) are included.
     */
    fun calculatePurchaseBalances(
        purchase: Purchase,
        members: List<PurchaseMember>,
        payments: List<Payment>,
        students: Map<Long, Student>
    ): List<StudentBalance> {
        // Collect all student IDs who are either members or part of payment records
        val involvedStudentIds = buildSet {
            members.forEach { add(it.studentId) }
            payments.forEach {
                add(it.payerStudentId)
                add(it.receiverStudentId)
            }
        }.sorted()

        val memberMap = members.associateBy { it.studentId }
        val activePayments = payments.filter { it.cancelledAt == null }

        return involvedStudentIds.map { studentId ->
            val member = memberMap[studentId]
            val contribution = member?.contributionAmount ?: 0L
            val share = member?.allocatedShare ?: 0L

            val paymentsSent = activePayments
                .filter { it.payerStudentId == studentId }
                .sumOf { it.amount }

            val paymentsReceived = activePayments
                .filter { it.receiverStudentId == studentId }
                .sumOf { it.amount }

            val studentName = students[studentId]?.name ?: "طالب $studentId"

            StudentBalance(
                studentId = studentId,
                studentName = studentName,
                contribution = contribution,
                share = share,
                paymentsSent = paymentsSent,
                paymentsReceived = paymentsReceived
            )
        }
    }

    /**
     * Computes the overall financial summary for a purchase.
     */
    fun computePurchaseSummary(
        purchase: Purchase,
        balances: List<StudentBalance>,
        payments: List<Payment>
    ): PurchaseFinancialSummary {
        val totalContributions = balances.sumOf { it.contribution }
        val totalShares = balances.sumOf { it.share }

        val activePayments = payments.filter { it.cancelledAt == null }
        val totalSettled = activePayments.sumOf { it.amount }

        // Outstanding remaining is the sum of positive current balances (or sum of abs(negative))
        val remainingOutstanding = balances.filter { it.currentBalance > 0L }.sumOf { it.currentBalance }
        val isSettled = balances.all { it.currentBalance == 0L }

        return PurchaseFinancialSummary(
            totalAmount = purchase.totalAmount,
            totalContributions = totalContributions,
            totalShares = totalShares,
            totalSettled = totalSettled,
            remainingOutstanding = remainingOutstanding,
            isSettled = isSettled
        )
    }

    /**
     * Deterministic two-pointer settlement algorithm between debtors and creditors
     * within a single purchase.
     */
    fun generateSettlementTransfers(balances: List<StudentBalance>): List<ProposedTransfer> {
        data class MutableAccount(val studentId: Long, val name: String, var amount: Long)

        val debtors = balances
            .filter { it.currentBalance < 0L }
            .map { MutableAccount(it.studentId, it.studentName, -it.currentBalance) }
            .sortedBy { it.studentId }

        val creditors = balances
            .filter { it.currentBalance > 0L }
            .map { MutableAccount(it.studentId, it.studentName, it.currentBalance) }
            .sortedBy { it.studentId }

        val transfers = mutableListOf<ProposedTransfer>()
        var debtorIdx = 0
        var creditorIdx = 0

        while (debtorIdx < debtors.size && creditorIdx < creditors.size) {
            val debtor = debtors[debtorIdx]
            val creditor = creditors[creditorIdx]

            val transferAmount = minOf(debtor.amount, creditor.amount)
            if (transferAmount > 0L) {
                transfers.add(
                    ProposedTransfer(
                        payerId = debtor.studentId,
                        payerName = debtor.name,
                        receiverId = creditor.studentId,
                        receiverName = creditor.name,
                        amount = transferAmount
                    )
                )
                debtor.amount -= transferAmount
                creditor.amount -= transferAmount
            }

            if (debtor.amount == 0L) debtorIdx++
            if (creditor.amount == 0L) creditorIdx++
        }

        return transfers
    }

    /**
     * Deterministic matching for a single student settling their full remaining debt
     * across multiple creditors.
     */
    fun generateSingleStudentSettlementTransfers(
        payerId: Long,
        balances: List<StudentBalance>
    ): List<ProposedTransfer> {
        val payer = balances.find { it.studentId == payerId } ?: return emptyMap<Nothing, Nothing>().let { emptyList() }
        if (payer.currentBalance >= 0L) return emptyList()

        var remainingDebt = -payer.currentBalance
        val creditors = balances
            .filter { it.currentBalance > 0L }
            .map { it.studentId to (it.studentName to it.currentBalance) }
            .sortedBy { it.first }

        val transfers = mutableListOf<ProposedTransfer>()

        for ((creditorId, nameAndAmount) in creditors) {
            if (remainingDebt <= 0L) break
            val (creditorName, entitlement) = nameAndAmount
            val transferAmount = minOf(remainingDebt, entitlement)
            if (transferAmount > 0L) {
                transfers.add(
                    ProposedTransfer(
                        payerId = payer.studentId,
                        payerName = payer.studentName,
                        receiverId = creditorId,
                        receiverName = creditorName,
                        amount = transferAmount
                    )
                )
                remainingDebt -= transferAmount
            }
        }

        return transfers
    }

    /**
     * Validates a partial payment attempt.
     * Returns null if valid, or an Arabic error message if invalid.
     */
    fun validatePayment(
        amount: Long,
        payerId: Long,
        receiverId: Long,
        balances: List<StudentBalance>
    ): String? {
        if (amount <= 0L) {
            return "مبلغ الدفعة يجب أن يكون أكبر من صفر."
        }
        if (payerId == receiverId) {
            return "لا يمكن للطالب الدفع لنفسه."
        }
        val payer = balances.find { it.studentId == payerId }
            ?: return "الطالب الدافع غير موجود في هذه العملية."
        val receiver = balances.find { it.studentId == receiverId }
            ?: return "الطالب المستلم غير موجود في هذه العملية."

        if (payer.currentBalance >= 0L) {
            return "الطالب (${payer.studentName}) ليس عليه ديون متبقية في هذه العملية."
        }
        val payerDebt = -payer.currentBalance
        if (amount > payerDebt) {
            return "مبلغ الدفعة (${formatCurrency(amount)}) يتجاوز الدين المتبقي على الطالب (${formatCurrency(payerDebt)})."
        }

        if (receiver.currentBalance <= 0L) {
            return "الطالب (${receiver.studentName}) ليس له مستحقات متبقية للاستلام في هذه العملية."
        }
        val receiverEntitlement = receiver.currentBalance
        if (amount > receiverEntitlement) {
            return "مبلغ الدفعة (${formatCurrency(amount)}) يتجاوز المستحق المتبقي للطالب (${formatCurrency(receiverEntitlement)})."
        }

        return null
    }

    /**
     * Validates purchase members creation/update fields.
     */
    fun validatePurchaseForm(
        title: String,
        totalAmount: Long,
        selectedParticipantIds: Set<Long>,
        contributions: Map<Long, Long>,
        shares: Map<Long, Long>,
        splitMode: String
    ): String? {
        if (title.trim().isEmpty()) {
            return "يرجى إدخال عنوان لعملية الشراء."
        }
        if (totalAmount <= 0L) {
            return "مبلغ الشراء الإجمالي يجب أن يكون أكبر من صفر."
        }
        if (selectedParticipantIds.isEmpty()) {
            return "يجب اختيار مشارك واحد على الأقل في تقاسم المصروف."
        }

        val totalContributions = contributions.values.sum()
        if (totalContributions != totalAmount) {
            val diff = totalAmount - totalContributions
            val diffText = if (diff > 0) "المتبقي للمساهمات: ${formatCurrency(diff)}" else "المساهمات تزيد بمقدار: ${formatCurrency(-diff)}"
            return "مجموع المساهمات (${formatCurrency(totalContributions)}) لا يساوي إجمالي الشراء (${formatCurrency(totalAmount)}). $diffText"
        }

        val totalShares = shares.values.sum()
        if (totalShares != totalAmount) {
            val diff = totalAmount - totalShares
            val diffText = if (diff > 0) "المتبقي للحصص: ${formatCurrency(diff)}" else "الحصص تزيد بمقدار: ${formatCurrency(-diff)}"
            return "مجموع حصص المصروف (${formatCurrency(totalShares)}) لا يساوي إجمالي الشراء (${formatCurrency(totalAmount)}). $diffText"
        }

        if (splitMode == "CUSTOM") {
            for (id in selectedParticipantIds) {
                val share = shares[id] ?: 0L
                if (share <= 0L) {
                    return "في التقسيم المخصص، يجب أن تكون حصة كل مشارك محدد أكبر من صفر."
                }
            }
        }

        return null
    }

    /**
     * Validates proposed purchase changes against existing active payments.
     * Simulates the payments in chronological order starting from initial balances.
     * Returns Pair(isValid, errorMessage)
     */
    fun validatePurchaseEditWithPayments(
        newMembers: List<PurchaseMember>,
        existingPayments: List<Payment>,
        students: Map<Long, Student>
    ): Pair<Boolean, String?> {
        val activePayments = existingPayments
            .filter { it.cancelledAt == null }
            .sortedBy { it.createdAt }

        if (activePayments.isEmpty()) {
            return Pair(true, null)
        }

        // Tracking running balances
        val balanceMap = mutableMapOf<Long, Long>()
        for (member in newMembers) {
            val initialBalance = member.contributionAmount - member.allocatedShare
            balanceMap[member.studentId] = initialBalance
        }

        // Simulate each payment
        for (payment in activePayments) {
            val payerId = payment.payerStudentId
            val receiverId = payment.receiverStudentId
            val payerName = students[payerId]?.name ?: "طالب $payerId"
            val receiverName = students[receiverId]?.name ?: "طالب $receiverId"

            val payerBalance = balanceMap[payerId]
                ?: return Pair(
                    false,
                    "الدفعة رقم #${payment.id} بقيمة ${formatCurrency(payment.amount)} من ($payerName) أصبحت غير صالحة لأن الدافع لم يعد ضمن أطراف العملية."
                )

            val receiverBalance = balanceMap[receiverId]
                ?: return Pair(
                    false,
                    "الدفعة رقم #${payment.id} بقيمة ${formatCurrency(payment.amount)} إلى ($receiverName) أصبحت غير صالحة لأن المستلم لم يعد ضمن أطراف العملية."
                )

            val payerDebt = -payerBalance
            if (payerDebt < payment.amount) {
                return Pair(
                    false,
                    "الدفعة رقم #${payment.id} بقيمة ${formatCurrency(payment.amount)} تتجاوز الدين المتبقي على ($payerName) في هذا التسلسل (${formatCurrency(if (payerDebt > 0) payerDebt else 0L)})."
                )
            }

            val receiverEntitlement = receiverBalance
            if (receiverEntitlement < payment.amount) {
                return Pair(
                    false,
                    "الدفعة رقم #${payment.id} بقيمة ${formatCurrency(payment.amount)} تتجاوز المستحق المتبقي لـ ($receiverName) في هذا التسلسل (${formatCurrency(if (receiverEntitlement > 0) receiverEntitlement else 0L)})."
                )
            }

            // Apply payment effect
            balanceMap[payerId] = payerBalance + payment.amount
            balanceMap[receiverId] = receiverBalance - payment.amount
        }

        return Pair(true, null)
    }
}
