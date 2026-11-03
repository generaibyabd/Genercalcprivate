package com.example.housing.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoryId")]
)
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Long,
    val purchaseDate: Long,
    val categoryId: Long,
    val notes: String? = null,
    val splitMode: String = "EQUAL", // EQUAL or CUSTOM
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_members",
    primaryKeys = ["purchaseId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("purchaseId"), Index("studentId")]
)
data class PurchaseMember(
    val purchaseId: Long,
    val studentId: Long,
    val isParticipant: Boolean,
    val allocatedShare: Long,
    val contributionAmount: Long
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["payerStudentId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["receiverStudentId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("purchaseId"), Index("payerStudentId"), Index("receiverStudentId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val payerStudentId: Long,
    val receiverStudentId: Long,
    val amount: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val cancelledAt: Long? = null
)
