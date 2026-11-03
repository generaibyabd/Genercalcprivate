package com.example.housing.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.housing.model.Category
import com.example.housing.model.Student
import com.example.housing.ui.HousingViewModel
import com.example.housing.ui.components.AppTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    viewModel: HousingViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    // Dialog states
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var studentToRename by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToRename by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    // SAF Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(json.toByteArray())
                        }
                    }
                    snackbarHostState.showSnackbar("تم تصدير النسخة الاحتياطية بنجاح.")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("فشل تصدير النسخة: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader().readText()
                        }
                    }
                    if (json.isNullOrBlank()) {
                        snackbarHostState.showSnackbar("الملف المحدد فارغ.")
                    } else {
                        pendingImportJson = json
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("فشل قراءة الملف: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "الإعدادات") },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settings_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Student Management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إدارة الطلاب (${allStudents.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = { showAddStudentDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_student_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة طالب", fontSize = 12.sp)
                            }
                        }

                        if (allStudents.isEmpty()) {
                            Text(
                                text = "لم تتم إضافة أي طلاب بعد.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            allStudents.forEach { student ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = student.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (student.isActive) "نشط" else "مؤرشف",
                                            fontSize = 11.sp,
                                            color = if (student.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { studentToRename = student }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل الاسم", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleStudentActive(student.id, !student.isActive) { res ->
                                                    res.onFailure {
                                                        scope.launch { snackbarHostState.showSnackbar("فشل تغيير حالة الطالب.") }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (student.isActive) Icons.Default.Archive else Icons.Default.Unarchive,
                                                contentDescription = if (student.isActive) "أرشفة" else "تنشيط",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(onClick = { studentToDelete = student }, modifier = Modifier.size(32.dp)) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف نهائي",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Category Management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إدارة التصنيفات (${allCategories.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Button(
                                onClick = { showAddCategoryDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_category_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة تصنيف", fontSize = 12.sp)
                            }
                        }

                        allCategories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name + if (category.isDefault) " (افتراضي)" else "",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )

                                Row {
                                    IconButton(onClick = { categoryToRename = category }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(16.dp))
                                    }
                                    if (category.name != "أخرى") {
                                        IconButton(onClick = { categoryToDelete = category }, modifier = Modifier.size(32.dp)) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Backup & Restore
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("النسخ الاحتياطي والاستعادة", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "احفظ نسخة كاملة بصيغة JSON لجميع الطلاب، التصنيفات، العمليات وسجلات الدفعات، أو استعد بياناتك في أي وقت.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { exportLauncher.launch("housing_backup_${System.currentTimeMillis()}.json") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير نسخة")
                            }

                            Button(
                                onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_backup_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استعادة نسخة")
                            }
                        }
                    }
                }
            }

            // Section 4: Danger Zone (Delete All Data)
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "منطقة الخطر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "حذف كافة البيانات نهائياً وتفريغ قاعدة البيانات بالكامل. لا يمكن التراجع عن هذه الخطوة.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showDeleteAllDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_data_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف كافة البيانات في التطبيق")
                        }
                    }
                }
            }

            // Section 5: App Information
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "حساب السكن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "الإصدار 1.0.0 • يعمل محلياً 100% دون اتصال",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "العملة: الدينار العراقي (IQD)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ---------------- Dialogs ----------------

    // Add Student Dialog
    if (showAddStudentDialog) {
        var studentName by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = { Text("إضافة طالب جديد") },
            text = {
                Column {
                    OutlinedTextField(
                        value = studentName,
                        onValueChange = { studentName = it },
                        label = { Text("اسم الطالب") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_student_name_input")
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addStudent(studentName) { res ->
                            res.onSuccess {
                                showAddStudentDialog = false
                                scope.launch { snackbarHostState.showSnackbar("تمت إضافة الطالب بنجاح.") }
                            }.onFailure { err ->
                                errorMsg = err.message
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_student_button")
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Rename Student Dialog
    studentToRename?.let { student ->
        var newName by remember { mutableStateOf(student.name) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { studentToRename = null },
            title = { Text("تعديل اسم الطالب") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم الطالب الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameStudent(student.id, newName) { res ->
                            res.onSuccess {
                                studentToRename = null
                                scope.launch { snackbarHostState.showSnackbar("تم تعديل الاسم بنجاح.") }
                            }.onFailure { err ->
                                errorMsg = err.message
                            }
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToRename = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Student Dialog
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("حذف الطالب") },
            text = {
                Text("هل تريد حذف الطالب (${student.name}) نهائياً؟\n\nإذا كان الطالب مشاركاً في أي عمليات أو دفعات مسجلة، فلن يمكن حذفه نهائياً حفاظاً على سلامة الحسابات المالية، ويمكنك أرشفته بدلاً من ذلك.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = student
                        studentToDelete = null
                        viewModel.deleteStudentSafely(s) { res ->
                            res.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("تم حذف الطالب بنجاح.") }
                            }.onFailure { err ->
                                scope.launch { snackbarHostState.showSnackbar(err.message ?: "تعذر حذف الطالب.") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("إضافة تصنيف جديد") },
            text = {
                Column {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        label = { Text("اسم التصنيف") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_category_name_input")
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCategory(categoryName) { res ->
                            res.onSuccess {
                                showAddCategoryDialog = false
                                scope.launch { snackbarHostState.showSnackbar("تمت إضافة التصنيف بنجاح.") }
                            }.onFailure { err ->
                                errorMsg = err.message
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_category_button")
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Rename Category Dialog
    categoryToRename?.let { category ->
        var newCatName by remember { mutableStateOf(category.name) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = { Text("تعديل اسم التصنيف") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("اسم التصنيف الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameCategory(category.id, newCatName) { res ->
                            res.onSuccess {
                                categoryToRename = null
                                scope.launch { snackbarHostState.showSnackbar("تم تعديل التصنيف بنجاح.") }
                            }.onFailure { err ->
                                errorMsg = err.message
                            }
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete Category Dialog
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("حذف التصنيف") },
            text = {
                Text(
                    "هل أنت متأكد من حذف التصنيف (${category.name})؟\n\n" +
                            "ملاحظة: سيتم نقل كافة عمليات الشراء المرتبطة بهذا التصنيف تلقائياً إلى تصنيف (أخرى)."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cId = category.id
                        categoryToDelete = null
                        viewModel.deleteCategory(cId) { res ->
                            res.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("تم حذف التصنيف وإعادة تعيين العمليات إلى 'أخرى'.") }
                            }.onFailure { err ->
                                scope.launch { snackbarHostState.showSnackbar(err.message ?: "فشل حذف التصنيف.") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف التصنيف")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Import Backup Confirmation Dialog
    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية") },
            text = {
                Text(
                    "تحذير هام: ستؤدي عملية الاستعادة إلى استبدال كافة البيانات الحالية في التطبيق بالبيانات الموجودة في ملف النسخة الاحتياطية بشكل كامل.\n\n" +
                            "هل تريد المتابعة واستبدال البيانات؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toImport = json
                        pendingImportJson = null
                        scope.launch {
                            val res = viewModel.importBackupJson(toImport)
                            res.onSuccess {
                                snackbarHostState.showSnackbar("تمت استعادة النسخة الاحتياطية بنجاح.")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar("فشلت الاستعادة: ${err.message}")
                            }
                        }
                    }
                ) {
                    Text("تأكيد الاستبدال والاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Delete All Data Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("حذف كافة البيانات نهائياً", color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    "هل أنت متأكد تماماً من رغبتك في حذف كافة بيانات التطبيق نهائياً؟\n\n" +
                            "سيتم مسح جميع الطلاب والتصنيفات والمشتريات وسجلات الدفعات ولن يمكن استرجاعها إلا في حال وجود نسخة احتياطية سابقة."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        viewModel.deleteAllData { res ->
                            res.onSuccess {
                                scope.launch { snackbarHostState.showSnackbar("تم حذف كافة البيانات بنجاح.") }
                            }.onFailure { err ->
                                scope.launch { snackbarHostState.showSnackbar(err.message ?: "فشل حذف البيانات.") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف كل شيء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
