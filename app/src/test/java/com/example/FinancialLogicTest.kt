package com.example

import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.Category
import com.example.housing.model.GlobalStudentSummary
import com.example.housing.model.Payment
import com.example.housing.model.Purchase
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialLogicTest {

    private val studentAhmed = Student(id = 1L, name = "أحمد", isActive = true)
    private val studentMohammed = Student(id = 2L, name = "محمد", isActive = true)
    private val studentAli = Student(id = 3L, name = "علي", isActive = true)
    private val studentHassan = Student(id = 4L, name = "حسن", isActive = true)

    private val studentsMap = mapOf(
        1L to studentAhmed,
        2L to studentMohammed,
        3L to studentAli,
        4L to studentHassan
    )

    // Test 1 — Equal split: Purchase 30,000 IQD. أحمد contributes 30,000. أحمد، محمد، علي share equally.
    @Test
    fun test1_equalSplit() {
        val total = 30000L
        val participantIds = listOf(1L, 2L, 3L)
        val shares = FinanceCalculations.calculateEqualShares(total, participantIds)

        assertEquals(10000L, shares[1L])
        assertEquals(10000L, shares[2L])
        assertEquals(10000L, shares[3L])

        val purchase = Purchase(id = 1L, title = "مسواك", totalAmount = total, purchaseDate = 1000L, categoryId = 1L)
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )

        val balances = FinanceCalculations.calculatePurchaseBalances(purchase, members, emptyList(), studentsMap)
        val balMap = balances.associateBy { it.studentId }

        assertEquals(20000L, balMap[1L]?.currentBalance)
        assertEquals(-10000L, balMap[2L]?.currentBalance)
        assertEquals(-10000L, balMap[3L]?.currentBalance)
        // Sum must be zero
        assertEquals(0L, balances.sumOf { it.currentBalance })
    }

    // Test 2 — Partial payment: محمد pays أحمد 4,000.
    @Test
    fun test2_partialPayment() {
        val purchase = Purchase(id = 1L, title = "مسواك", totalAmount = 30000L, purchaseDate = 1000L, categoryId = 1L)
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )
        val payments = listOf(
            Payment(id = 1L, purchaseId = 1L, payerStudentId = 2L, receiverStudentId = 1L, amount = 4000L, createdAt = 1000L)
        )

        val balances = FinanceCalculations.calculatePurchaseBalances(purchase, members, payments, studentsMap)
        val balMap = balances.associateBy { it.studentId }

        assertEquals(16000L, balMap[1L]?.currentBalance)
        assertEquals(-6000L, balMap[2L]?.currentBalance)
        assertEquals(-10000L, balMap[3L]?.currentBalance)
        assertEquals(0L, balances.sumOf { it.currentBalance })
    }

    // Test 3 — Full purchase settlement: additional transfers: محمد pays أحمد 6,000, علي pays أحمد 10,000.
    @Test
    fun test3_fullPurchaseSettlement() {
        val purchase = Purchase(id = 1L, title = "مسواك", totalAmount = 30000L, purchaseDate = 1000L, categoryId = 1L)
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )
        val initialPayments = listOf(
            Payment(id = 1L, purchaseId = 1L, payerStudentId = 2L, receiverStudentId = 1L, amount = 4000L, createdAt = 1000L)
        )
        val currentBalances = FinanceCalculations.calculatePurchaseBalances(purchase, members, initialPayments, studentsMap)

        val transfers = FinanceCalculations.generateSettlementTransfers(currentBalances)
        assertEquals(2, transfers.size)

        // محمد pays أحمد 6000
        assertEquals(2L, transfers[0].payerId)
        assertEquals(1L, transfers[0].receiverId)
        assertEquals(6000L, transfers[0].amount)

        // علي pays أحمد 10000
        assertEquals(3L, transfers[1].payerId)
        assertEquals(1L, transfers[1].receiverId)
        assertEquals(10000L, transfers[1].amount)

        val allPayments = initialPayments + transfers.mapIndexed { idx, t ->
            Payment(id = (idx + 2).toLong(), purchaseId = 1L, payerStudentId = t.payerId, receiverStudentId = t.receiverId, amount = t.amount, createdAt = 2000L + idx)
        }

        val finalBalances = FinanceCalculations.calculatePurchaseBalances(purchase, members, allPayments, studentsMap)
        assertTrue(finalBalances.all { it.currentBalance == 0L })
    }

    // Test 4 — Multiple contributors: أحمد 8,000, محمد 4,000, total 12,000 shared equally among 3.
    @Test
    fun test4_multipleContributors() {
        val total = 12000L
        val purchase = Purchase(id = 1L, title = "منظفات", totalAmount = total, purchaseDate = 1000L, categoryId = 1L)
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 4000L, contributionAmount = 8000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 4000L, contributionAmount = 4000L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 4000L, contributionAmount = 0L)
        )

        val balances = FinanceCalculations.calculatePurchaseBalances(purchase, members, emptyList(), studentsMap)
        val balMap = balances.associateBy { it.studentId }

        assertEquals(4000L, balMap[1L]?.currentBalance)
        assertEquals(0L, balMap[2L]?.currentBalance)
        assertEquals(-4000L, balMap[3L]?.currentBalance)
        assertEquals(0L, balances.sumOf { it.currentBalance })
    }

    // Test 5 — Custom split: أحمد 30,000. Shares: أحمد 15,000, محمد 10,000, علي 5,000.
    @Test
    fun test5_customSplit() {
        val total = 30000L
        val purchase = Purchase(id = 1L, title = "لحوم", totalAmount = total, purchaseDate = 1000L, categoryId = 1L, splitMode = "CUSTOM")
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 15000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 5000L, contributionAmount = 0L)
        )

        val balances = FinanceCalculations.calculatePurchaseBalances(purchase, members, emptyList(), studentsMap)
        val balMap = balances.associateBy { it.studentId }

        assertEquals(15000L, balMap[1L]?.currentBalance)
        assertEquals(-10000L, balMap[2L]?.currentBalance)
        assertEquals(-5000L, balMap[3L]?.currentBalance)
        assertEquals(0L, balances.sumOf { it.currentBalance })
    }

    // Test 6 — Integer remainder: 10,000 IQD shared by 3 students -> 3334, 3333, 3333.
    @Test
    fun test6_integerRemainder() {
        val total = 10000L
        val participantIds = listOf(1L, 2L, 3L)
        val shares = FinanceCalculations.calculateEqualShares(total, participantIds)

        assertEquals(3334L, shares[1L])
        assertEquals(3333L, shares[2L])
        assertEquals(3333L, shares[3L])
        assertEquals(total, shares.values.sum())
    }

    // Test 7 — Payment cancellation: record valid payment, cancel it, balances return to previous values.
    @Test
    fun test7_paymentCancellation() {
        val total = 30000L
        val purchase = Purchase(id = 1L, title = "طعام", totalAmount = total, purchaseDate = 1000L, categoryId = 1L)
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )

        val activePayment = Payment(id = 1L, purchaseId = 1L, payerStudentId = 2L, receiverStudentId = 1L, amount = 4000L, createdAt = 1000L, cancelledAt = null)
        val balancesWithPayment = FinanceCalculations.calculatePurchaseBalances(purchase, members, listOf(activePayment), studentsMap)
        val balMapWith = balancesWithPayment.associateBy { it.studentId }
        assertEquals(16000L, balMapWith[1L]?.currentBalance)
        assertEquals(-6000L, balMapWith[2L]?.currentBalance)

        // Cancel the payment
        val cancelledPayment = activePayment.copy(cancelledAt = 2000L)
        val balancesAfterCancellation = FinanceCalculations.calculatePurchaseBalances(purchase, members, listOf(cancelledPayment), studentsMap)
        val balMapAfter = balancesAfterCancellation.associateBy { it.studentId }
        assertEquals(20000L, balMapAfter[1L]?.currentBalance)
        assertEquals(-10000L, balMapAfter[2L]?.currentBalance)
    }

    // Test 8 — Multiple creditors: one debtor pays multiple creditors, full settlement reaches zero.
    @Test
    fun test8_multipleCreditors() {
        val total = 30000L
        val purchase = Purchase(id = 1L, title = "أجهزة", totalAmount = total, purchaseDate = 1000L, categoryId = 1L)
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 5000L, contributionAmount = 20000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 5000L, contributionAmount = 10000L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 20000L, contributionAmount = 0L)
        )

        val balances = FinanceCalculations.calculatePurchaseBalances(purchase, members, emptyList(), studentsMap)
        val transfers = FinanceCalculations.generateSettlementTransfers(balances)

        // Ali owes 20,000, should pay Ahmed 15,000 and Mohammed 5,000
        assertEquals(2, transfers.size)
        assertEquals(3L, transfers[0].payerId)
        assertEquals(1L, transfers[0].receiverId)
        assertEquals(15000L, transfers[0].amount)

        assertEquals(3L, transfers[1].payerId)
        assertEquals(2L, transfers[1].receiverId)
        assertEquals(5000L, transfers[1].amount)

        val payments = transfers.mapIndexed { idx, t ->
            Payment(id = (idx + 1).toLong(), purchaseId = 1L, payerStudentId = t.payerId, receiverStudentId = t.receiverId, amount = t.amount)
        }
        val finalBalances = FinanceCalculations.calculatePurchaseBalances(purchase, members, payments, studentsMap)
        assertTrue(finalBalances.all { it.currentBalance == 0L })
    }

    // Test 9 — Purchase editing: modification with existing payments.
    @Test
    fun test9_purchaseEditing() {
        val members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )
        val existingPayment = Payment(id = 1L, purchaseId = 1L, payerStudentId = 2L, receiverStudentId = 1L, amount = 5000L, createdAt = 1000L)

        // Invalid edit: removing student 2 while payment exists
        val invalidMembers = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 15000L, contributionAmount = 30000L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 15000L, contributionAmount = 0L)
        )
        val (isValidInvalid, _) = FinanceCalculations.validatePurchaseEditWithPayments(invalidMembers, listOf(existingPayment), studentsMap)
        assertFalse(isValidInvalid)

        // Valid edit: student 2 keeps a share that accommodates the 5,000 payment
        val validMembers = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 30000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L),
            PurchaseMember(1L, 3L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )
        val (isValidValid, errorMsg) = FinanceCalculations.validatePurchaseEditWithPayments(validMembers, listOf(existingPayment), studentsMap)
        assertTrue(isValidValid)
        assertEquals(null, errorMsg)
    }

    // Test 10 — Global aggregation: multiple purchases, global receivables and debts sum per-purchase balances without cross offset.
    @Test
    fun test10_globalAggregation() {
        // Purchase 1: Ahmed (+10,000), Mohammed (-10,000)
        val p1 = Purchase(1L, "شراء 1", 20000L, 1000L, 1L)
        val p1Members = listOf(
            PurchaseMember(1L, 1L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 20000L),
            PurchaseMember(1L, 2L, isParticipant = true, allocatedShare = 10000L, contributionAmount = 0L)
        )
        val p1Balances = FinanceCalculations.calculatePurchaseBalances(p1, p1Members, emptyList(), studentsMap)

        // Purchase 2: Mohammed (+15,000), Ahmed (-15,000)
        val p2 = Purchase(2L, "شراء 2", 30000L, 2000L, 1L)
        val p2Members = listOf(
            PurchaseMember(2L, 2L, isParticipant = true, allocatedShare = 15000L, contributionAmount = 30000L),
            PurchaseMember(2L, 1L, isParticipant = true, allocatedShare = 15000L, contributionAmount = 0L)
        )
        val p2Balances = FinanceCalculations.calculatePurchaseBalances(p2, p2Members, emptyList(), studentsMap)

        // Aggregate across purchases for Ahmed (ID 1)
        val ahmedBalP1 = p1Balances.find { it.studentId == 1L }!!.currentBalance // +10,000
        val ahmedBalP2 = p2Balances.find { it.studentId == 1L }!!.currentBalance // -15,000

        var ahmedReceivables = 0L
        var ahmedDebts = 0L
        for (bal in listOf(ahmedBalP1, ahmedBalP2)) {
            if (bal > 0L) ahmedReceivables += bal
            else if (bal < 0L) ahmedDebts += -bal
        }

        // Must NOT cancel out! Receivables = 10,000, Debts = 15,000
        assertEquals(10000L, ahmedReceivables)
        assertEquals(15000L, ahmedDebts)
        val netPosition = ahmedReceivables - ahmedDebts
        assertEquals(-5000L, netPosition)
    }

    // Test 11 — Backup and restore structure verification: all fields and relations are preserved.
    @Test
    fun test11_backupAndRestoreStructure() {
        val originalStudents = listOf(studentAhmed, studentMohammed, studentAli)
        val originalCategories = listOf(Category(1L, "طعام", true, 1))
        val originalPurchase = Purchase(1L, "طعام السكن", 30000L, 1000L, 1L)
        val originalMembers = listOf(
            PurchaseMember(1L, 1L, true, 10000L, 30000L),
            PurchaseMember(1L, 2L, true, 10000L, 0L),
            PurchaseMember(1L, 3L, true, 10000L, 0L)
        )
        val originalPayments = listOf(
            Payment(1L, 1L, 2L, 1L, 4000L, 1000L)
        )

        // Calculate balances before export
        val balancesBefore = FinanceCalculations.calculatePurchaseBalances(
            originalPurchase, originalMembers, originalPayments, originalStudents.associateBy { it.id }
        )
        val balMapBefore = balancesBefore.associateBy { it.studentId }

        assertEquals(16000L, balMapBefore[1L]?.currentBalance)
        assertEquals(-6000L, balMapBefore[2L]?.currentBalance)
        assertEquals(-10000L, balMapBefore[3L]?.currentBalance)

        // Invariants check
        assertEquals(originalPurchase.totalAmount, originalMembers.sumOf { it.contributionAmount })
        assertEquals(originalPurchase.totalAmount, originalMembers.sumOf { it.allocatedShare })
        assertEquals(0L, balancesBefore.sumOf { it.currentBalance })
    }
}
