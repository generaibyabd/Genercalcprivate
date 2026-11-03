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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.DetailedPurchaseRecord
import com.example.housing.ui.HousingViewModel
import com.example.housing.ui.PurchaseSort
import com.example.housing.ui.PurchaseStatusFilter
import com.example.housing.ui.components.AppTopBar
import com.example.housing.ui.components.PurchaseStatusBadge
import com.example.ui.theme.DebtorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesScreen(
    viewModel: HousingViewModel,
    onNavigateToAddPurchase: () -> Unit,
    onNavigateToPurchaseDetails: (Long) -> Unit
) {
    val purchases by viewModel.filteredPurchases.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "المشتريات",
                actions = {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("purchases_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "ترتيب المشتريات"
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        PurchaseSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.labelAr,
                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.sortOption.value = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddPurchase,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("purchases_add_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة عملية شراء"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("بحث عن عملية شراء بالاسم...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("purchases_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PurchaseStatusFilter.entries) { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { viewModel.statusFilter.value = filter },
                        label = { Text(filter.labelAr) }
                    )
                }
            }

            // Categories Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.selectedCategoryId.value = null },
                        label = { Text("كل التصنيفات") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = {
                            viewModel.selectedCategoryId.value =
                                if (selectedCategoryId == category.id) null else category.id
                        },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Purchases List
            if (purchases.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد نتائج مطابقة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جرب تغيير كلمات البحث أو تصفيات الحالة والتصنيف.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(purchases) { record ->
                        PurchaseItemFullCard(
                            record = record,
                            onClick = { onNavigateToPurchaseDetails(record.purchase.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseItemFullCard(
    record: DetailedPurchaseRecord,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.purchase.purchaseDate))

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("purchase_item_${record.purchase.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PurchaseStatusBadge(isSettled = record.isSettled)
                }
            }

            // Balances summary preview
            Spacer(modifier = Modifier.height(10.dp))
            if (record.isSettled) {
                Text(
                    text = "تم سداد كافة حصص ومستحقات هذه العملية بالكامل.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val debtorsCount = record.balances.count { it.currentBalance < 0L }
                    val creditorsCount = record.balances.count { it.currentBalance > 0L }

                    Text(
                        text = "$debtorsCount عليهم ديون • $creditorsCount لهم مستحقات",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "المتبقي: ${FinanceCalculations.formatCurrency(record.summary.remainingOutstanding)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DebtorRed
                    )
                }
            }
        }
    }
}
