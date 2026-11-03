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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.DetailedPurchaseRecord
import com.example.housing.model.GlobalStudentSummary
import com.example.housing.ui.DashboardUiState
import com.example.housing.ui.HousingViewModel
import com.example.housing.ui.components.AppTopBar
import com.example.housing.ui.components.PurchaseStatusBadge
import com.example.ui.theme.CreditorGreen
import com.example.ui.theme.DebtorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: HousingViewModel,
    onNavigateToAddPurchase: () -> Unit,
    onNavigateToPurchaseDetails: (Long) -> Unit,
    onNavigateToPurchasesList: () -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    val studentSummaries by viewModel.globalStudentSummaries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(title = "حساب السكن — الرئيسية")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPurchase,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("dashboard_add_purchase_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة عملية شراء"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("dashboard_scroll_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Financial Overview KPI Cards
            item {
                Text(
                    text = "ملخص المصاريف العامة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                FinancialOverviewCard(state = state)
            }

            // Section 2: Total Outstanding Debts & Receivables
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DebtReceivableCard(
                        title = "إجمالي الديون المطلوبة",
                        subtitle = "ديون تنتظر السداد",
                        amount = state.totalOutstandingDebts,
                        icon = Icons.Default.TrendingDown,
                        color = DebtorRed,
                        modifier = Modifier.weight(1f)
                    )
                    DebtReceivableCard(
                        title = "إجمالي المستحقات",
                        subtitle = "مبالغ مستحقة للدافعين",
                        amount = state.totalOutstandingReceivables,
                        icon = Icons.Default.TrendingUp,
                        color = CreditorGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 3: Compact Student Financial Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "أرصدة الطلاب (${studentSummaries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToAccounts) {
                        Text("عرض الكل")
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (studentSummaries.isEmpty()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "لم تتم إضافة طلاب بعد. يمكنك إضافة الطلاب من صفحة الإعدادات.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(studentSummaries) { studentSummary ->
                            StudentQuickSummaryCard(
                                summary = studentSummary,
                                onClick = onNavigateToAccounts
                            )
                        }
                    }
                }
            }

            // Section 4: Recent Purchases
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العمليات الأخيرة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToPurchasesList) {
                        Text("كل المشتريات")
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (state.recentPurchases.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد عمليات شراء مسجلة بعد",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "اضغط على زر الإضافة أدناه لتسجيل أول عملية شراء ومشاركتها.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.recentPurchases) { record ->
                    DashboardPurchaseItemCard(
                        record = record,
                        onClick = { onNavigateToPurchaseDetails(record.purchase.id) }
                    )
                }
            }

            // Bottom spacer for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun FinancialOverviewCard(state: DashboardUiState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إجمالي مصروفات السكن",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FinanceCalculations.formatCurrency(state.totalSpending),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${state.purchasesCount} عملية شراء",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${state.unsettledCount} غير مكتملة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtReceivableCard(
    title: String,
    subtitle: String,
    amount: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = FinanceCalculations.formatCurrency(amount),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun StudentQuickSummaryCard(
    summary: GlobalStudentSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = summary.studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (summary.totalReceivables > 0L) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "له: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CreditorGreen
                    )
                    Text(
                        text = FinanceCalculations.formatCurrency(summary.totalReceivables),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CreditorGreen
                    )
                }
            }
            if (summary.totalDebts > 0L) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "عليه: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DebtorRed
                    )
                    Text(
                        text = FinanceCalculations.formatCurrency(summary.totalDebts),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DebtorRed
                    )
                }
            }
            if (summary.totalReceivables == 0L && summary.totalDebts == 0L) {
                Text(
                    text = "الحساب مسدد",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun DashboardPurchaseItemCard(
    record: DetailedPurchaseRecord,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.purchase.purchaseDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("purchase_card_${record.purchase.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.purchase.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${record.category?.name ?: "عام"} • $dateStr",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FinanceCalculations.formatCurrency(record.purchase.totalAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PurchaseStatusBadge(isSettled = record.isSettled)
                }
            }

            if (!record.isSettled && record.summary.remainingOutstanding > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المتبقي للسداد: ${FinanceCalculations.formatCurrency(record.summary.remainingOutstanding)}",
                        fontSize = 12.sp,
                        color = DebtorRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
