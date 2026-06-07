package com.jamakharch

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jamakharch.ui.categorydetail.CategoryDetailScreen
import com.jamakharch.ui.categorydetail.CategoryDetailVM
import com.jamakharch.ui.edit.EditScreen
import com.jamakharch.ui.edit.EditVM
import com.jamakharch.ui.list.ExpenseListScreen
import com.jamakharch.ui.list.ExpenseListVM
import com.jamakharch.ui.settings.SettingsScreen
import com.jamakharch.ui.settings.SettingsVM
import com.jamakharch.ui.summary.SummaryScreen
import com.jamakharch.ui.summary.SummaryVM

sealed class Screen {
    data object Summary : Screen()
    data object List : Screen()
    data class Edit(val expenseId: Long) : Screen()
    data object Settings : Screen()
    data class CategoryDetail(
        val category: String,
        val monthStart: Long,
        val monthEnd: Long
    ) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val backStack = remember { mutableStateListOf<Screen>(Screen.Summary) }
                    val push: (Screen) -> Unit = { backStack.add(it) }
                    val pop: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }

                    var hasPermission by remember { mutableStateOf(checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) }

                    if (!hasPermission) {
                        PermissionScreen(onGranted = { hasPermission = true })
                    } else {
                        BackHandler(enabled = backStack.size > 1) { pop() }

                        when (val current = backStack.last()) {
                            is Screen.Summary -> {
                                val vm: SummaryVM = viewModel(factory = SummaryVM.Factory(this))
                                val scanVM: ExpenseListVM = viewModel(factory = ExpenseListVM.Factory(this))
                                SummaryScreen(
                                    viewModel = vm,
                                    scanVM = scanVM,
                                    onCategoryClick = { category, monthStart, monthEnd ->
                                        push(Screen.CategoryDetail(category, monthStart, monthEnd))
                                    },
                                    onGoToAllExpenses = { push(Screen.List) },
                                    onGoToSettings = { push(Screen.Settings) }
                                )
                            }
                            is Screen.List -> {
                                val vm: ExpenseListVM = viewModel(factory = ExpenseListVM.Factory(this))
                                ExpenseListScreen(
                                    viewModel = vm,
                                    onEditExpense = { id -> push(Screen.Edit(id)) },
                                    onBack = { pop() }
                                )
                            }
                            is Screen.CategoryDetail -> {
                                val vm: CategoryDetailVM = viewModel(
                                    key = "catdetail_${current.category}_${current.monthStart}",
                                    factory = CategoryDetailVM.Factory(
                                        this, current.category, current.monthStart, current.monthEnd
                                    )
                                )
                                CategoryDetailScreen(
                                    viewModel = vm,
                                    onBack = { pop() },
                                    onEditExpense = { id -> push(Screen.Edit(id)) }
                                )
                            }
                            is Screen.Settings -> {
                                val vm: SettingsVM = viewModel(factory = SettingsVM.Factory(this))
                                SettingsScreen(
                                    viewModel = vm,
                                    onBack = { pop() }
                                )
                            }
                            is Screen.Edit -> {
                                val vm: EditVM = viewModel(
                                    key = "edit_${current.expenseId}",
                                    factory = EditVM.Factory(this, current.expenseId)
                                )
                                EditScreen(
                                    viewModel = vm,
                                    onBack = { pop() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted()
        permissionRequested = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Jama Kharch",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This app reads your bank SMS messages to automatically track expenses.\n\nNo data leaves your phone. Everything stays local.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { launcher.launch(Manifest.permission.READ_SMS) }) {
            Text("Grant SMS Permission")
        }
        if (permissionRequested) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permission was denied. Please enable it in Settings > Apps > Jama Kharch > Permissions.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
