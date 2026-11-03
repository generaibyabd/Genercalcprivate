package com.example.housing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.housing.logic.FinanceCalculations
import com.example.housing.model.BalanceStatus
import com.example.ui.theme.CreditorGreen
import com.example.ui.theme.CreditorGreenContainer
import com.example.ui.theme.DebtorRed
import com.example.ui.theme.DebtorRedContainer
import com.example.ui.theme.SettledGray
import com.example.ui.theme.SettledGrayContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun PurchaseStatusBadge(isSettled: Boolean) {
    val bg = if (isSettled) CreditorGreenContainer else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (isSettled) CreditorGreen else MaterialTheme.colorScheme.onTertiaryContainer
    val icon = if (isSettled) Icons.Default.CheckCircle else Icons.Default.HourglassTop
    val text = if (isSettled) "مكتملة" else "غير مكتملة"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StudentBalanceBadge(
    status: BalanceStatus,
    amount: Long,
    showLabel: Boolean = true
) {
    val bg: Color
    val fg: Color
    val label: String
    when (status) {
        BalanceStatus.CREDITOR -> {
            bg = CreditorGreenContainer
            fg = CreditorGreen
            label = "له"
        }
        BalanceStatus.DEBTOR -> {
            bg = DebtorRedContainer
            fg = DebtorRed
            label = "عليه"
        }
        BalanceStatus.SETTLED -> {
            bg = SettledGrayContainer
            fg = SettledGray
            label = "مسدد"
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showLabel) {
                Text(
                    text = label,
                    color = fg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = FinanceCalculations.formatCurrency(amount),
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
