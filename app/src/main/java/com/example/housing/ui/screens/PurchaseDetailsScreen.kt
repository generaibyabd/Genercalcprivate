package com.example.housing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.BalanceStatus
import com.example.housing.model.DetailedPurchaseRecord
import com.example.housing.model.Payment
import com.example.housing.model.ProposedTransfer
import com.example.housing.model.StudentBalance
import com.example.housing.ui.HousingViewModel
import com.example.housing.ui.components.AppTopBar
import com.example.housing.ui.components.PurchaseStatusBadge
import com.example.housing.ui.components.StudentBalanceBadge
import androidx.compose.foundation.clickable
import com.example.ui.theme.CreditorGreen
import com.example.ui.theme.DebtorRed
import com.example.ui.theme.SettledGray
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseDetailsScreen(
    purchaseId: Long,
    viewModel: HousingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val allPurchases by viewModel.allDetailedPurchases.collectAsStateWithLifecycle()
    val record = allPurchases.find { it.purchase.id == purchaseId }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Settlement dialog states
    var transfersToPreview by remember { mutableStateOf<List<ProposedTransfer>?>(null) }
    var previewDialogTitle by remember { mutableStateOf("") }

    // Partial payment dialog state
    var partialPaymentPayer by remember { mutableStateOf<StudentBalance?>(null) }

    // Payment editing dialog state
    var paymentToEdit by remember { mutableStateOf<Payment?>(null) }

    // Payment cancel confirm dialog
    var paymentToCancel by remember { mutableStateOf<Payment?>(null) }

    if (record == null) {
        Scaffold(
            topBar = { AppTopBar(title = "تفاصيل عملية الشراء", onBack = onNavigateBack) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("عملية الشراء غير موجودة أو تم حذفها.")
            }
        }
        return
    }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.purchase.purchaseDate))

    Scaffold(
        topBar = {
            AppTopBar(
                title = record.purchase.title,
                onBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(record.purchase.id) },
                        modifier = Modifier.testTag("purchase_edit_top_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل العملية")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "خيارات إضافية")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("تعديل العملية") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToEdit(record.purchase.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف العملية", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("purchase_details_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Purchase Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = record.purchase.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${record.category?.name ?: "عام"} • $dateStr",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PurchaseStatusBadge(isSettled = record.isSettled)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إجمالي مبلغ الشراء:",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinanceCalculations.formatCurrency(record.purchase.totalAmount),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!record.purchase.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ملاحظات: ${record.purchase.notes}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // B. Financial Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "الملخص المالي للعملية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        SummaryRow("إجمالي المساهمات الأصلية", FinanceCalculations.formatCurrency(record.summary.totalContributions))
                        SummaryRow("إجمالي حصص المصروف الموزعة", FinanceCalculations.formatCurrency(record.summary.totalShares))
                        SummaryRow("إجمالي المبالغ المسددة لاحقاً", FinanceCalculations.formatCurrency(record.summary.totalSettled))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المبلغ المتبقي غير المسدد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = FinanceCalculations.formatCurrency(record.summary.remainingOutstanding),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (record.summary.remainingOutstanding > 0) DebtorRed else CreditorGreen
                            )
                        }
                    }
                }
            }

            // Prominent Settle Entire Purchase Action
            item {
                Button(
                    onClick = {
                        val transfers = FinanceCalculations.generateSettlementTransfers(record.balances)
                        previewDialogTitle = "تسوية كامل عملية الشراء"
                        transfersToPreview = transfers
                    },
                    enabled = !record.isSettled && record.summary.remainingOutstanding > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settle_entire_purchase_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (record.isSettled) "العملية مسددة بالكامل" else "تسوية كامل العملية تلقائياً",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // C. Individual Student Accounts
            item {
                Text(
                    text = "حسابات الطلاب في هذه العملية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(record.balances) { studentBal ->
                StudentPurchaseAccountCard(
                    balance = studentBal,
                    onPartialPayment = { partialPaymentPayer = studentBal },
                    onFullSettlement = {
                        val transfers = FinanceCalculations.generateSingleStudentSettlementTransfers(
                            studentBal.studentId,
                            record.balances
                        )
                        previewDialogTitle = "سداد كامل دين الطالب (${studentBal.studentName})"
                        transfersToPreview = transfers
                    }
                )
            }

            // D. Payment History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل الدفعات (${record.payments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (record.payments.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "لا توجد دفعات مسجلة بعد لهذه العملية.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(record.payments) { payment ->
                    PaymentHistoryItemCard(
                        payment = payment,
                        balances = record.balances,
                        onEdit = { paymentToEdit = payment },
                        onCancel = { paymentToCancel = payment }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // ---------------- Dialogs ----------------

    // 1. Delete Purchase Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("حذف عملية الشراء") },
            text = {
                Text(
                    "هل أنت متأكد من رغبتك في حذف عملية الشراء (${record.purchase.title})؟\n\n" +
                            "تحذير: سيؤدي حذف العملية إلى إزالة كافة سجلات الدفعات المرتبطة بها نهائياً وتحديث أرصدة الطلاب."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePurchase(record.purchase.id) { res ->
                            res.onSuccess {
                                onNavigateBack()
                            }.onFailure { err ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(err.message ?: "فشل حذف العملية.")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف العملية")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Settlement Confirmation Preview Dialog
    transfersToPreview?.let { transfers ->
        AlertDialog(
            onDismissRequest = { transfersToPreview = null },
            title = { Text(previewDialogTitle) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (transfers.isEmpty()) {
                        Text("لا توجد مبالغ متبقية للتسوية.")
                    } else {
                        Text(
                            text = "سيتم تسجيل التحويلات التالية لتسوية الحسابات:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        transfers.forEach { transfer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${transfer.payerName}  ⬅️  ${transfer.receiverName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = FinanceCalculations.formatCurrency(transfer.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (transfers.isNotEmpty()) {
                    Button(
                        onClick = {
                            val list = transfers
                            transfersToPreview = null
                            viewModel.recordMultiplePayments(record.purchase.id, list) { res ->
                                res.onSuccess {
                                    scope.launch { snackbarHostState.showSnackbar("تمت التسوية بنجاح.") }
                                }.onFailure { err ->
                                    scope.launch { snackbarHostState.showSnackbar(err.message ?: "فشل تسجيل التسوية.") }
                                }
                            }
                        }
                    ) {
                        Text("تأكيد وتسجيل التسوية")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { transfersToPreview = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Partial Payment Dialog
    partialPaymentPayer?.let { payer ->
        var amountText by remember { mutableStateOf("") }
        val creditors = record.balances.filter { it.currentBalance > 0L }
        var selectedReceiverId by remember { mutableStateOf(creditors.firstOrNull()?.studentId) }
        var validationMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { partialPaymentPayer = null },
            title = { Text("تسجيل دفعة جزئية: ${payer.studentName}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "الدين المتبقي على الطالب: ${FinanceCalculations.formatCurrency(payer.remainingDebt)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DebtorRed
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) amountText = input
                        },
                        label = { Text("مبلغ الدفعة (د.ع)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("partial_payment_amount_input"),
                        singleLine = true
                    )

                    if (creditors.size > 1) {
                        Text(
                            text = "اختر المستلم (من الدائنين):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        creditors.forEach { cred ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedReceiverId = cred.studentId }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(
                                    selected = selectedReceiverId == cred.studentId,
                                    onClick = { selectedReceiverId = cred.studentId }
                                )
                                Text(
                                    text = "${cred.studentName} (مستحق له: ${FinanceCalculations.formatCurrency(cred.remainingEntitlement)})",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (creditors.size == 1) {
                        Text(
                            text = "المستلم: ${creditors.first().studentName} (مستحق له: ${FinanceCalculations.formatCurrency(creditors.first().remainingEntitlement)})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (validationMsg != null) {
                        Text(
                            text = validationMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toLongOrNull() ?: 0L
                        val receiverId = selectedReceiverId
                        if (receiverId == null) {
                            validationMsg = "يرجى تحديد المستلم."
                            return@Button
                        }
                        val err = FinanceCalculations.validatePayment(
                            amount = amount,
                            payerId = payer.studentId,
                            receiverId = receiverId,
                            balances = record.balances
                        )
                        if (err != null) {
                            validationMsg = err
                            return@Button
                        }

                        partialPaymentPayer = null
                        viewModel.recordPayment(
                            purchaseId = record.purchase.id,
                            payerId = payer.studentId,
                            receiverId = receiverId,
                            amount = amount
                        ) { res ->
                            res.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("تم تسجيل الدفعة بنجاح.") }
                            }.onFailure { e ->
                                scope.launch { snackbarHostState.showSnackbar(e.message ?: "فشل تسجيل الدفعة.") }
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_partial_payment_btn")
                ) {
                    Text("تسجيل الدفعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { partialPaymentPayer = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 4. Edit Payment Dialog
    paymentToEdit?.let { payment ->
        var editAmountText by remember { mutableStateOf(payment.amount.toString()) }
        var editPayerId by remember { mutableStateOf(payment.payerStudentId) }
        var editReceiverId by remember { mutableStateOf(payment.receiverStudentId) }
        var editErrorMsg by remember { mutableStateOf<String?>(null) }

        val studentList = record.balances

        AlertDialog(
            onDismissRequest = { paymentToEdit = null },
            title = { Text("تعديل الدفعة #${payment.id}") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) editAmountText = input
                        },
                        label = { Text("المبلغ المعدل (د.ع)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("الدافع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    studentList.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editPayerId = s.studentId },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = editPayerId == s.studentId,
                                onClick = { editPayerId = s.studentId }
                            )
                            Text(s.studentName, fontSize = 13.sp)
                        }
                    }

                    Text("المستلم:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    studentList.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editReceiverId = s.studentId },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = editReceiverId == s.studentId,
                                onClick = { editReceiverId = s.studentId }
                            )
                            Text(s.studentName, fontSize = 13.sp)
                        }
                    }

                    if (editErrorMsg != null) {
                        Text(
                            text = editErrorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = editAmountText.toLongOrNull() ?: 0L
                        if (newAmount <= 0) {
                            editErrorMsg = "المبلغ يجب أن يكون أكبر من صفر."
                            return@Button
                        }
                        if (editPayerId == editReceiverId) {
                            editErrorMsg = "لا يمكن للطالب الدفع لنفسه."
                            return@Button
                        }

                        viewModel.editPayment(
                            paymentId = payment.id,
                            newAmount = newAmount,
                            newPayerId = editPayerId,
                            newReceiverId = editReceiverId
                        ) { res ->
                            res.onSuccess {
                                paymentToEdit = null
                                scope.launch { snackbarHostState.showSnackbar("تم حفظ تعديل الدفعة بنجاح.") }
                            }.onFailure { err ->
                                editErrorMsg = err.message
                            }
                        }
                    }
                ) {
                    Text("حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToEdit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 5. Cancel Payment Confirm Dialog
    paymentToCancel?.let { payment ->
        val payerName = record.balances.find { it.studentId == payment.payerStudentId }?.studentName ?: "الطالب"
        val receiverName = record.balances.find { it.studentId == payment.receiverStudentId }?.studentName ?: "الطالب"

        AlertDialog(
            onDismissRequest = { paymentToCancel = null },
            title = { Text("إلغاء الدفعة") },
            text = {
                Text(
                    "هل أنت متأكد من إلغاء الدفعة بقيمة ${FinanceCalculations.formatCurrency(payment.amount)} من ($payerName) إلى ($receiverName)؟\n\n" +
                            "سيتم وضع علامة ملغاة على السجل واستبعادها من كافة الحسابات المالية."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pId = payment.id
                        paymentToCancel = null
                        viewModel.cancelPayment(pId) { res ->
                            res.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("تم إلغاء الدفعة بنجاح.") }
                            }.onFailure { err ->
                                scope.launch { snackbarHostState.showSnackbar(err.message ?: "فشل إلغاء الدفعة.") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، إلغاء الدفعة")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToCancel = null }) {
                    Text("تراجع")
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StudentPurchaseAccountCard(
    balance: StudentBalance,
    onPartialPayment: () -> Unit,
    onFullSettlement: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_account_${balance.studentId}"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = balance.studentName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                StudentBalanceBadge(
                    status = balance.status,
                    amount = if (balance.currentBalance < 0) -balance.currentBalance else balance.currentBalance
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "المساهمة الأصلية",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = FinanceCalculations.formatCurrency(balance.contribution),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "الحصة من المصروف",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = FinanceCalculations.formatCurrency(balance.share),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "دفعات أرسلها",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = FinanceCalculations.formatCurrency(balance.paymentsSent),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "دفعات استلمها",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = FinanceCalculations.formatCurrency(balance.paymentsReceived),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Payment actions if student has debt
            if (balance.currentBalance < 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPartialPayment,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pay_partial_btn_${balance.studentId}"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسجيل دفعة جزئية", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onFullSettlement,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pay_full_btn_${balance.studentId}"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سداد كامل المتبقي", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryItemCard(
    payment: Payment,
    balances: List<StudentBalance>,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    val isCancelled = payment.cancelledAt != null
    val payerName = balances.find { it.studentId == payment.payerStudentId }?.studentName ?: "طالب #${payment.payerStudentId}"
    val receiverName = balances.find { it.studentId == payment.receiverStudentId }?.studentName ?: "طالب #${payment.receiverStudentId}"

    val timeFormat = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault())
    val dateStr = timeFormat.format(Date(payment.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_history_card_${payment.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$payerName  ⬅️  $receiverName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = FinanceCalculations.formatCurrency(payment.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isCancelled) SettledGray else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                if (isCancelled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DebtorRed.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ملغاة",
                            color = DebtorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Cancel, contentDescription = "إلغاء", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
