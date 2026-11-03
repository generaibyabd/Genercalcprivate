package com.example.housing.data

import androidx.room.withTransaction
import com.example.housing.model.Category
import com.example.housing.model.Payment
import com.example.housing.model.Purchase
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    private const val BACKUP_VERSION = 1

    /**
     * Serializes all application tables into a complete JSON backup string.
     */
    suspend fun exportToJson(database: AppDatabase): String {
        val studentDao = database.studentDao()
        val categoryDao = database.categoryDao()
        val purchaseDao = database.purchaseDao()
        val purchaseMemberDao = database.purchaseMemberDao()
        val paymentDao = database.paymentDao()

        val students = studentDao.getAllStudentsSync()
        val categories = categoryDao.getAllCategoriesSync()
        val purchases = purchaseDao.getAllPurchasesSync()
        val members = purchaseMemberDao.getAllMembersSync()
        val payments = paymentDao.getAllPaymentsSync()

        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        root.put("appName", "حساب السكن")

        // Students array
        val studentsArray = JSONArray()
        for (s in students) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("isActive", s.isActive)
            obj.put("createdAt", s.createdAt)
            studentsArray.put(obj)
        }
        root.put("students", studentsArray)

        // Categories array
        val categoriesArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("isDefault", c.isDefault)
            obj.put("sortOrder", c.sortOrder)
            categoriesArray.put(obj)
        }
        root.put("categories", categoriesArray)

        // Purchases array
        val purchasesArray = JSONArray()
        for (p in purchases) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("title", p.title)
            obj.put("totalAmount", p.totalAmount)
            obj.put("purchaseDate", p.purchaseDate)
            obj.put("categoryId", p.categoryId)
            obj.put("notes", p.notes ?: JSONObject.NULL)
            obj.put("splitMode", p.splitMode)
            obj.put("createdAt", p.createdAt)
            obj.put("updatedAt", p.updatedAt)
            purchasesArray.put(obj)
        }
        root.put("purchases", purchasesArray)

        // Members array
        val membersArray = JSONArray()
        for (m in members) {
            val obj = JSONObject()
            obj.put("purchaseId", m.purchaseId)
            obj.put("studentId", m.studentId)
            obj.put("isParticipant", m.isParticipant)
            obj.put("allocatedShare", m.allocatedShare)
            obj.put("contributionAmount", m.contributionAmount)
            membersArray.put(obj)
        }
        root.put("members", membersArray)

        // Payments array
        val paymentsArray = JSONArray()
        for (pay in payments) {
            val obj = JSONObject()
            obj.put("id", pay.id)
            obj.put("purchaseId", pay.purchaseId)
            obj.put("payerStudentId", pay.payerStudentId)
            obj.put("receiverStudentId", pay.receiverStudentId)
            obj.put("amount", pay.amount)
            obj.put("createdAt", pay.createdAt)
            obj.put("cancelledAt", pay.cancelledAt ?: JSONObject.NULL)
            paymentsArray.put(obj)
        }
        root.put("payments", paymentsArray)

        return root.toString(2)
    }

    /**
     * Validates and imports a JSON backup string into the database atomically.
     * Replaces existing data completely.
     */
    suspend fun importFromJson(database: AppDatabase, jsonString: String): Result<Unit> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("version")) {
                return Result.failure(IllegalArgumentException("ملف النسخة الاحتياطية غير صالح: ينقصه رقم الإصدار."))
            }
            val version = root.getInt("version")
            if (version > BACKUP_VERSION) {
                return Result.failure(IllegalArgumentException("إصدار النسخة الاحتياطية ($version) أحدث من هذا التطبيق."))
            }

            // Parse Students
            val students = mutableListOf<Student>()
            val studentsArray = root.optJSONArray("students") ?: JSONArray()
            for (i in 0 until studentsArray.length()) {
                val obj = studentsArray.getJSONObject(i)
                students.add(
                    Student(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        isActive = obj.optBoolean("isActive", true),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Parse Categories
            val categories = mutableListOf<Category>()
            val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.getJSONObject(i)
                categories.add(
                    Category(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        isDefault = obj.optBoolean("isDefault", false),
                        sortOrder = obj.optInt("sortOrder", i + 1)
                    )
                )
            }

            // Parse Purchases
            val purchases = mutableListOf<Purchase>()
            val purchasesArray = root.optJSONArray("purchases") ?: JSONArray()
            for (i in 0 until purchasesArray.length()) {
                val obj = purchasesArray.getJSONObject(i)
                purchases.add(
                    Purchase(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        totalAmount = obj.getLong("totalAmount"),
                        purchaseDate = obj.getLong("purchaseDate"),
                        categoryId = obj.getLong("categoryId"),
                        notes = if (obj.isNull("notes")) null else obj.getString("notes"),
                        splitMode = obj.optString("splitMode", "EQUAL"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Parse Members
            val members = mutableListOf<PurchaseMember>()
            val membersArray = root.optJSONArray("members") ?: JSONArray()
            for (i in 0 until membersArray.length()) {
                val obj = membersArray.getJSONObject(i)
                members.add(
                    PurchaseMember(
                        purchaseId = obj.getLong("purchaseId"),
                        studentId = obj.getLong("studentId"),
                        isParticipant = obj.getBoolean("isParticipant"),
                        allocatedShare = obj.getLong("allocatedShare"),
                        contributionAmount = obj.getLong("contributionAmount")
                    )
                )
            }

            // Parse Payments
            val payments = mutableListOf<Payment>()
            val paymentsArray = root.optJSONArray("payments") ?: JSONArray()
            for (i in 0 until paymentsArray.length()) {
                val obj = paymentsArray.getJSONObject(i)
                payments.add(
                    Payment(
                        id = obj.getLong("id"),
                        purchaseId = obj.getLong("purchaseId"),
                        payerStudentId = obj.getLong("payerStudentId"),
                        receiverStudentId = obj.getLong("receiverStudentId"),
                        amount = obj.getLong("amount"),
                        createdAt = obj.getLong("createdAt"),
                        cancelledAt = if (obj.isNull("cancelledAt")) null else obj.getLong("cancelledAt")
                    )
                )
            }

            // Atomic database replacement
            database.withTransaction {
                val studentDao = database.studentDao()
                val categoryDao = database.categoryDao()
                val purchaseDao = database.purchaseDao()
                val purchaseMemberDao = database.purchaseMemberDao()
                val paymentDao = database.paymentDao()

                paymentDao.deleteAllPayments()
                purchaseMemberDao.deleteAllMembers()
                purchaseDao.deleteAllPurchases()
                categoryDao.deleteAllCategories()
                studentDao.deleteAllStudents()

                if (students.isNotEmpty()) studentDao.insertAllStudents(students)
                if (categories.isNotEmpty()) {
                    categoryDao.insertAllCategories(categories)
                } else {
                    for (cat in AppDatabase.DEFAULT_CATEGORIES) {
                        categoryDao.insertCategory(cat)
                    }
                }
                if (purchases.isNotEmpty()) purchaseDao.insertAllPurchases(purchases)
                if (members.isNotEmpty()) purchaseMemberDao.insertAllMembers(members)
                if (payments.isNotEmpty()) paymentDao.insertAllPayments(payments)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
