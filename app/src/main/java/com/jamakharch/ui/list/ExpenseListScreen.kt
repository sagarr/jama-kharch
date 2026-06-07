package com.jamakharch.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.jamakharch.category.Category
import com.jamakharch.data.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseListVM,
    onEditExpense: (Long) -> Unit,
    onBack: () -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    val hasScanned by viewModel.hasScanned.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(scanMessage) {
        scanMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("All Expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isScanning,
            onRefresh = { viewModel.scanSms() }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (!hasScanned && expenses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pull down to scan\nyour bank transactions",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (expenses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses found in this period",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val grouped = expenses.groupBy { expense ->
                        val cal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
                        String.format(
                            Locale.US, "%04d-%02d-%02d",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    }.toSortedMap(compareByDescending { it })

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        grouped.forEach { (dateKey, dateExpenses) ->
                            item {
                                val parts = dateKey.split("-")
                                val cal = Calendar.getInstance()
                                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                                val formatted = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
                                    .format(Date(cal.timeInMillis))

                                Text(
                                    text = formatted,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp)
                                )
                            }

                            items(dateExpenses, key = { it.id }) { expense ->
                                ExpenseItem(
                                    expense = expense,
                                    onClick = { onEditExpense(expense.id) }
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isScanning,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun ExpenseItem(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    val category = try {
        Category.valueOf(expense.category)
    } catch (_: IllegalArgumentException) {
        Category.OTHERS
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${"%.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(
                        color = categoryColor(category).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = categoryColor(category)
                )
            }
        }
    }
}

fun categoryColor(category: Category): Color = when (category) {
    Category.GROCERY -> Color(0xFF2E7D32)
    Category.MEDICINE -> Color(0xFFC62828)
    Category.FUEL -> Color(0xFFE65100)
    Category.FOOD -> Color(0xFFF57F17)
    Category.TRANSPORT -> Color(0xFF4A148C)
    Category.SHOPPING -> Color(0xFF1565C0)
    Category.ENTERTAINMENT -> Color(0xFF6A1B9A)
    Category.UTILITIES -> Color(0xFF37474F)
    Category.HEALTH -> Color(0xFF00838F)
    Category.INVESTMENT -> Color(0xFF00695C)
    Category.TRAVEL -> Color(0xFF0277BD)
    Category.SCHOOL -> Color(0xFFAD1457)
    Category.OTHERS -> Color(0xFF757575)
}
