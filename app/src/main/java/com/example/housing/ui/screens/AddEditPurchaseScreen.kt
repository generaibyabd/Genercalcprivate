package com.example.housing.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.Category
import com.example.housing.model.DetailedPurchaseRecord
import com.example.housing.model.PurchaseMember
import com.example.housing.model.Student
import com.example.housing.ui.HousingViewModel
import com.example.housing.ui.components.AppTopBar
import com.example.ui.theme.CreditorGreen
import com.example.ui.theme.DebtorRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun <K : Any, V : Any> snapshotStateMapSaver(): Saver<SnapshotStateMap<K, V>, Any> =
    listSaver(
        save = { stateMap -> stateMap.toList() },
        restore = { it.toMutableStateMap() }
    )

@Composable
private fun <K : Any, V : Any> rememberMutableStateMapOf(
    vararg pairs: Pair<K, V>
): SnapshotStateMap<K, V> {
    return rememberSaveable(saver = snapshotStateMapSaver()) {
        pairs.toList().toMutableStateMap()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPurchaseScreen(
    viewModel: HousingViewModel,
    purchaseIdToEdit: Long?,
    onNavigateBack: () -> Unit,
    onPurchaseSaved: (Long) -> Unit
) {
    val isEditMode = purchaseIdToEdit != null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allPurchases by viewModel.allDetailedPurchases.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allStudentsList by viewModel.allStudents.collectAsStateWithLifecycle()
    val activeStudentsList by viewModel.activeStudents.collectAsStateWithLifecycle()

    val existingRecord = remember(purchaseIdToEdit, allPurchases) {
        if (purchaseIdToEdit != null) allPurchases.find { it.purchase.id == purchaseIdToEdit } else null
    }

    // Form fields
    var title by rememberSaveable { mutableStateOf("") }
    var totalAmountInput by rememberSaveable { mutableStateOf("") }
    var purchaseDate by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var notes by rememberSaveable { mutableStateOf("") }
    var splitMode by rememberSaveable { mutableStateOf("EQUAL") } // "EQUAL" or "CUSTOM"

    // Map studentId -> isParticipant
    val participantMap = rememberMutableStateMapOf<Long, Boolean>()
    // Map studentId -> contribution text
    val contributionInputs = rememberMutableStateMapOf<Long, String>()
    // Map studentId -> custom share text
    val customShareInputs = rememberMutableStateMapOf<Long, String>()

    // Init form when in Edit mode or when categories load
    var initialized by remember { mutableStateOf(false) }

    // Students to display in form: in edit mode include anyone who had shares/contributions, otherwise active students
    val relevantStudents = remember(allStudentsList, activeStudentsList, existingRecord) {
        if (existingRecord != null) {
            val memberIds = existingRecord.members.map { it.studentId }.toSet()
            allStudentsList.filter { it.isActive || memberIds.contains(it.id) }
        } else {
            activeStudentsList
        }
    }

    LaunchedEffect(existingRecord, categories, relevantStudents) {
        if (!initialized) {
            if (existingRecord != null) {
                val p = existingRecord.purchase
                title = p.title
                totalAmountInput = p.totalAmount.toString()
                purchaseDate = p.purchaseDate
                selectedCategoryId = p.categoryId
                notes = p.notes ?: ""
                splitMode = p.splitMode

                // Populate members
                for (m in existingRecord.members) {
                    participantMap[m.studentId] = m.isParticipant
                    contributionInputs[m.studentId] = if (m.contributionAmount > 0) m.contributionAmount.toString() else ""
                    customShareInputs[m.studentId] = if (m.allocatedShare > 0) m.allocatedShare.toString() else ""
                }
                initialized = true
            } else if (categories.isNotEmpty()) {
                selectedCategoryId = categories.firstOrNull { it.name == "طعام" }?.id ?: categories.first().id
                // Default select all active students as participants
                for (s in relevantStudents) {
                    participantMap[s.id] = true
                }
                initialized = true
            }
        }
    }

    val totalAmount = remember(totalAmountInput) {
        totalAmountInput.toLongOrNull() ?: 0L
    }

    val selectedParticipantIds by remember {
        derivedStateOf {
            participantMap.filterValues { it }.keys.toSet()
        }
    }

    // Calculated shares in EQUAL mode
    val equalShares = remember(totalAmount, selectedParticipantIds) {
        FinanceCalculations.calculateEqualShares(totalAmount, selectedParticipantIds.toList())
    }

    // Resolved shares map
    val finalSharesMap by remember(splitMode, equalShares, customShareInputs, selectedParticipantIds) {
        derivedStateOf {
            if (splitMode == "EQUAL") {
                equalShares
            } else {
                selectedParticipantIds.associateWith { studentId ->
                    customShareInputs[studentId]?.toLongOrNull() ?: 0L
                }
            }
        }
    }

    // Resolved contributions map
    val finalContributionsMap by remember(contributionInputs, relevantStudents) {
        derivedStateOf {
            relevantStudents.associate { s ->
                val amount = contributionInputs[s.id]?.toLongOrNull() ?: 0L
                s.id to amount
            }
        }
    }

    val sumContributions = remember(finalContributionsMap) {
        finalContributionsMap.values.sum()
    }

    val sumShares = remember(finalSharesMap) {
        finalSharesMap.values.sum()
    }

    // Validation
    val validationError by remember(
        title,
        totalAmount,
        selectedParticipantIds,
        finalContributionsMap,
        finalSharesMap,
        splitMode
    ) {
        derivedStateOf {
            FinanceCalculations.validatePurchaseForm(
                title = title,
                totalAmount = totalAmount,
                selectedParticipantIds = selectedParticipantIds,
                contributions = finalContributionsMap,
                shares = finalSharesMap,
                splitMode = splitMode
            )
        }
    }

    // Date Picker Dialog
    val calendar = Calendar.getInstance().apply { timeInMillis = purchaseDate }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            purchaseDate = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) "تعديل عملية الشراء" else "إضافة عملية شراء جديدة",
                onBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("add_edit_purchase_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Purchase Basic Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "معلومات العملية الأساسية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان الشراء (مثال: مسواك الخضار الأسبوعي)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("purchase_title_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = totalAmountInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) {
                                    totalAmountInput = input
                                }
                            },
                            label = { Text("المبلغ الإجمالي (د.ع)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("purchase_amount_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Date Selector
                        OutlinedTextField(
                            value = dateFormat.format(Date(purchaseDate)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("تاريخ العملية") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "اختيار التاريخ",
                                    modifier = Modifier.clickable { datePickerDialog.show() }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() },
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Category Dropdown
                        var categoryExpanded by remember { mutableStateOf(false) }
                        val selectedCategory = categories.find { it.id == selectedCategoryId }

                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.name ?: "اختر التصنيف",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("التصنيف") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCategoryId = cat.id
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("ملاحظات إضافية (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Section 2: Expense Participants & Splitting Mode
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تقاسم المصروف بين المشاركين",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedParticipantIds.size} من أصل ${relevantStudents.size}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Split Mode Toggle
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = splitMode == "EQUAL",
                                onClick = {
                                    splitMode = "EQUAL"
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("تقسيم متساوي")
                            }
                            SegmentedButton(
                                selected = splitMode == "CUSTOM",
                                onClick = {
                                    // When switching to CUSTOM, initialize with current equal shares
                                    for (sId in selectedParticipantIds) {
                                        val existingCustom = customShareInputs[sId]
                                        if (existingCustom.isNullOrEmpty()) {
                                            customShareInputs[sId] = (equalShares[sId] ?: 0L).toString()
                                        }
                                    }
                                    splitMode = "CUSTOM"
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("تقسيم مخصص")
                            }
                        }

                        // Participants list
                        relevantStudents.forEach { student ->
                            val isSelected = participantMap[student.id] ?: false
                            val shareAmount = finalSharesMap[student.id] ?: 0L

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            participantMap[student.id] = checked
                                            if (checked && splitMode == "CUSTOM") {
                                                if (customShareInputs[student.id].isNullOrEmpty()) {
                                                    customShareInputs[student.id] = "0"
                                                }
                                            }
                                        },
                                        modifier = Modifier.testTag("participant_checkbox_${student.id}")
                                    )
                                    Text(
                                        text = student.name + if (!student.isActive) " (مؤرشف)" else "",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }

                                if (isSelected) {
                                    if (splitMode == "EQUAL") {
                                        Text(
                                            text = FinanceCalculations.formatCurrency(shareAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        OutlinedTextField(
                                            value = customShareInputs[student.id] ?: "",
                                            onValueChange = { input ->
                                                if (input.all { it.isDigit() }) {
                                                    customShareInputs[student.id] = input
                                                }
                                            },
                                            placeholder = { Text("0") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .width(130.dp)
                                                .testTag("custom_share_input_${student.id}"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Running shares summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع الحصص: ${FinanceCalculations.formatCurrency(sumShares)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sumShares == totalAmount && totalAmount > 0) CreditorGreen else DebtorRed
                            )
                            val diff = totalAmount - sumShares
                            if (diff != 0L) {
                                Text(
                                    text = if (diff > 0) "المتبقي: ${FinanceCalculations.formatCurrency(diff)}"
                                    else "الزيادة: ${FinanceCalculations.formatCurrency(-diff)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DebtorRed
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Original Contributions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "المساهمات الأصلية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        relevantStudents.forEach { student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = student.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = contributionInputs[student.id] ?: "",
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            contributionInputs[student.id] = input
                                        }
                                    },
                                    placeholder = { Text("0 د.ع") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .width(140.dp)
                                        .testTag("contribution_input_${student.id}"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Quick action: Single Contributor pays total
                        if (totalAmount > 0 && sumContributions != totalAmount) {
                            Text(
                                text = "تعبئة سريعة:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                relevantStudents.take(3).forEach { s ->
                                    OutlinedButton(
                                        onClick = {
                                            contributionInputs.clear()
                                            contributionInputs[s.id] = totalAmount.toString()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = s.name, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // Running contributions summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع المساهمات: ${FinanceCalculations.formatCurrency(sumContributions)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sumContributions == totalAmount && totalAmount > 0) CreditorGreen else DebtorRed
                            )
                            val diff = totalAmount - sumContributions
                            if (diff != 0L) {
                                Text(
                                    text = if (diff > 0) "المتبقي: ${FinanceCalculations.formatCurrency(diff)}"
                                    else "الزيادة: ${FinanceCalculations.formatCurrency(-diff)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DebtorRed
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Validation Summary Card
            item {
                if (validationError != null) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationError ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = CreditorGreen.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CreditorGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "البيانات المالية متطابقة وصحيحة وجاهزة للحفظ.",
                                fontSize = 13.sp,
                                color = CreditorGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Section 5: Save Action Button
            item {
                Button(
                    onClick = {
                        val categoryId = selectedCategoryId ?: return@Button

                        val membersToSave = relevantStudents.mapNotNull { student ->
                            val isPart = participantMap[student.id] ?: false
                            val share = if (isPart) (finalSharesMap[student.id] ?: 0L) else 0L
                            val contrib = finalContributionsMap[student.id] ?: 0L

                            if (isPart || contrib > 0L) {
                                PurchaseMember(
                                    purchaseId = purchaseIdToEdit ?: 0L,
                                    studentId = student.id,
                                    isParticipant = isPart,
                                    allocatedShare = share,
                                    contributionAmount = contrib
                                )
                            } else null
                        }

                        if (isEditMode) {
                            viewModel.updatePurchase(
                                purchaseId = purchaseIdToEdit!!,
                                title = title,
                                totalAmount = totalAmount,
                                purchaseDate = purchaseDate,
                                categoryId = categoryId,
                                notes = notes,
                                splitMode = splitMode,
                                members = membersToSave
                            ) { result ->
                                result.onSuccess {
                                    onPurchaseSaved(purchaseIdToEdit)
                                }.onFailure { err ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(err.message ?: "فشل حفظ التعديلات.")
                                    }
                                }
                            }
                        } else {
                            viewModel.createPurchase(
                                title = title,
                                totalAmount = totalAmount,
                                purchaseDate = purchaseDate,
                                categoryId = categoryId,
                                notes = notes,
                                splitMode = splitMode,
                                members = membersToSave
                            ) { result ->
                                result.onSuccess { newId ->
                                    onPurchaseSaved(newId)
                                }.onFailure { err ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(err.message ?: "فشل إنشاء عملية الشراء.")
                                    }
                                }
                            }
                        }
                    },
                    enabled = validationError == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("purchase_save_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isEditMode) "حفظ التعديلات" else "حفظ عملية الشراء",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
