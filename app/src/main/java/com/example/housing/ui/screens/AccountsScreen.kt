package com.example.housing.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.GlobalStudentSummary
import com.example.housing.model.StudentPurchaseBreakdown
import com.example.housing.ui.HousingViewModel
import com.example.housing.ui.components.AppTopBar
import com.example.housing.ui.components.StudentBalanceBadge
import com.example.ui.theme.CreditorGreen
import com.example.ui.theme.DebtorRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: HousingViewModel,
    onNavigateToPurchaseDetails: (Long) -> Unit
) {
    val studentSummaries by viewModel.globalStudentSummaries.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedStudentSummary by remember { mutableStateOf<GlobalStudentSummary?>(null) }
    var studentBreakdownList by remember { mutableStateOf<List<StudentPurchaseBreakdown>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            AppTopBar(title = "الحسابات العامة للطلاب")
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("accounts_scroll_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Explanatory Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "الأرصدة العامة هنا لأغراض العرض والتقرير فقط. مستحقات أي عملية شراء لا تلغي أو تعوض ديون عملية أخرى تلقائياً. اضغط على أي طالب لعرض تفاصيل عملياته المستقلة.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (studentSummaries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا يوجد طلاب مضافون بعد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                items(studentSummaries) { summary ->
                    GlobalStudentCard(
                        summary = summary,
                        onClick = {
                            selectedStudentSummary = summary
                            scope.launch {
                                studentBreakdownList = viewModel.getStudentBreakdown(summary.studentId)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Modal Bottom Sheet for Student Purchase Breakdown
    if (selectedStudentSummary != null) {
        val summary = selectedStudentSummary!!
        ModalBottomSheet(
            onDismissRequest = { selectedStudentSummary = null },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "كشف حساب: ${summary.studentName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (summary.isActive) "طالب نشط" else "طالب مؤرشف",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (studentBreakdownList.isEmpty()) {
                    Text(
                        text = "لا توجد عمليات شراء مرتبطة بهذا الطالب حتى الآن.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    Text(
                        text = "العمليات المرتبطة (${studentBreakdownList.size}) — اضغط على أي عملية لفتح تفاصيلها:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(studentBreakdownList) { item ->
                            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            val dateStr = dateFormat.format(Date(item.purchaseDate))

                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStudentSummary = null
                                        onNavigateToPurchaseDetails(item.purchaseId)
                                    },
                                shape = RoundedCornerShape(10.dp)
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.purchaseTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${item.categoryName} • $dateStr",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        StudentBalanceBadge(
                                            status = item.status,
                                            amount = if (item.currentBalance < 0) -item.currentBalance else item.currentBalance
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "المساهمة: ${FinanceCalculations.formatCurrency(item.contribution)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "الحصة: ${FinanceCalculations.formatCurrency(item.share)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "دفع: ${FinanceCalculations.formatCurrency(item.paymentsSent)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "استلم: ${FinanceCalculations.formatCurrency(item.paymentsReceived)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GlobalStudentCard(
    summary: GlobalStudentSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("global_student_card_${summary.studentId}"),
        shape = RoundedCornerShape(14.dp),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.studentName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!summary.isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(مؤرشف)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "عرض التفاصيل",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Receivables & Debts displayed clearly and separately
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "إجمالي ما له (مستحقات)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceCalculations.formatCurrency(summary.totalReceivables),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CreditorGreen
                        )
                    }
                }

                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "إجمالي ما عليه (ديون)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FinanceCalculations.formatCurrency(summary.totalDebts),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DebtorRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Aggregated breakdown numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مساهمات: ${FinanceCalculations.formatCurrency(summary.totalContributions)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "حصص: ${FinanceCalculations.formatCurrency(summary.totalShares)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "دفع: ${FinanceCalculations.formatCurrency(summary.totalPaymentsSent)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "استلم: ${FinanceCalculations.formatCurrency(summary.totalPaymentsReceived)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Optional net position clearly marked as informational
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صافي الموقف المالي (رقم إحصائي تقديري):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                val net = summary.netPosition
                Text(
                    text = (if (net > 0) "+ " else if (net < 0) "- " else "") + FinanceCalculations.formatCurrency(if (net < 0) -net else net),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (net > 0) CreditorGreen else if (net < 0) DebtorRed else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
