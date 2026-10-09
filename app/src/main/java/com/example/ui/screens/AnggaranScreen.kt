package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KasViewModel
import com.example.ui.components.DetailRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.IsoDatePickerField
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnggaranScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val budgetRealizations by viewModel.budgetRealizations.collectAsStateWithLifecycle()
    val activeTransactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val projects by viewModel.activeProjects.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val currentPeriod = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    var periodInput by remember { mutableStateOf(currentPeriod) }
    var selectedCategory by remember { mutableStateOf(viewModel.masterAlokasi.first()) }
    var selectedProject by remember { mutableStateOf("") }
    var selectedFundBucket by remember { mutableStateOf("PT") }
    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var targetWaPhone by remember { mutableStateOf("") }

    // Expanded states for showing transaction breakdown per category
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Anggaran")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("anggaran_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card with WhatsApp Broadcast
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ALOKASI & PEMBAGIAN ANGGARAN",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Plafon alokasi vs realisasi pengeluaran kas periode $currentPeriod",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        IconButton(
                            onClick = {
                                val totalBudget = budgetRealizations.sumOf { it.budget.budgetAmount }
                                val totalSpent = budgetRealizations.sumOf { it.realization }
                                val waText = """
                                    *RINCIAN ALOKASI & PEMBAGIAN ANGGARAN*
                                    Periode: $currentPeriod
                                    ====================================
                                    💰 *TOTAL PAGU ANGGARAN* : ${formatRupiah(totalBudget)}
                                    💸 *TOTAL REALISASI*     : ${formatRupiah(totalSpent)}
                                    💵 *SISA ANGGARAN*       : ${formatRupiah(totalBudget - totalSpent)} (${String.format(Locale.getDefault(), "%.1f", if (totalBudget > 0) (totalSpent / totalBudget) * 100.0 else 0.0)}% terpakai)
                                    ====================================
                                    *Rincian Alokasi per Pos Biaya:*
                                    ${budgetRealizations.joinToString("\n\n") { b ->
                                        val status = if (b.isOver) "⚠️ OVER BUDGET" else "✅ AMAN"
                                        "📌 *${b.budget.category}*\n• Pagu: ${formatRupiah(b.budget.budgetAmount)}\n• Realisasi: ${formatRupiah(b.realization)} (${String.format(Locale.getDefault(), "%.1f", b.percentUsed)}%)\n• Sisa: ${formatRupiah(b.remaining)} [$status]"
                                    }}
                                    ====================================
                                    _Sistem Kas Terintegrasi - Laporan Otomatis_
                                """.trimIndent()
                                viewModel.kirimPesanWhatsApp(context, waText, targetWaPhone)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF25D366))
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Kirim ke WA", tint = Color.White)
                        }
                    }
                }
            }

            // Pembagian Porsi Anggaran (Budget Distribution Pie / Progress Overview)
            val totalBudgetPool = budgetRealizations.sumOf { it.budget.budgetAmount }
            val totalSpentPool = budgetRealizations.sumOf { it.realization }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pembagian Porsi Anggaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total: ${formatRupiah(totalBudgetPool)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryBlue,
                                fontSize = 13.sp
                            )
                        }

                        DetailRow("Total Realisasi Terpakai", formatRupiah(totalSpentPool))
                        DetailRow("Sisa Pagu Keseluruhan", formatRupiah(totalBudgetPool - totalSpentPool))

                        HorizontalDivider()

                        Text(text = "Distribusi Porsi per Pos Kategori:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        budgetRealizations.forEach { item ->
                            val portionPercent = if (totalBudgetPool > 0) (item.budget.budgetAmount / totalBudgetPool) * 100.0 else 0.0
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = item.budget.category, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "${formatRupiah(item.budget.budgetAmount)} (${String.format(Locale.getDefault(), "%.1f", portionPercent)}%)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (portionPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = PrimaryBlue,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = "Rincian Anggaran & Realisasi (${budgetRealizations.size} Pos)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (budgetRealizations.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.PieChart,
                        title = "Belum Ada Target Anggaran",
                        message = "Tekan tombol + di pojok kanan bawah untuk menambahkan alokasi anggaran."
                    )
                }
            } else {
                items(budgetRealizations) { item ->
                    val isExpanded = expandedCategoryId == item.budget.id
                    val categoryExpenses = activeTransactions.filter {
                        it.type == "KELUAR" && it.status == "Selesai" &&
                                (it.date.startsWith(item.budget.period) || item.budget.period.equals("All", ignoreCase = true)) &&
                                (it.category.equals(item.budget.category, ignoreCase = true) || it.allocation.equals(item.budget.category, ignoreCase = true))
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_card_${item.budget.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.budget.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Periode: ${item.budget.period} • ${item.budget.notes.ifBlank { "Pos Biaya" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusColor = if (item.isOver) ExpenseRed else IncomeGreen
                                    val statusBg = if (item.isOver) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                    val statusLabel = if (item.isOver) "OVER BUDGET" else "OK"

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(statusBg)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.hapusAnggaran(item.budget.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Progress Bar
                            val progressFloat = (item.percentUsed / 100.0).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (item.isOver) ExpenseRed else IncomeGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Numbers Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Realisasi: ${formatRupiah(item.realization)} (${String.format(Locale.getDefault(), "%.1f", item.percentUsed)}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pagu: ${formatRupiah(item.budget.budgetAmount)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Sisa Anggaran: ${formatRupiah(item.remaining)}",
                                    fontSize = 12.sp,
                                    color = if (item.remaining < 0) ExpenseRed else IncomeGreen,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Button to toggle transactions breakdown
                                    TextButton(
                                        onClick = {
                                            expandedCategoryId = if (isExpanded) null else item.budget.id
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(text = "Rincian (${categoryExpenses.size})", fontSize = 11.sp)
                                    }

                                    // Button to send this specific budget to WhatsApp
                                    IconButton(
                                        onClick = {
                                            val categoryWaText = """
                                                *RINCIAN ANGGARAN: ${item.budget.category.uppercase()}*
                                                Periode: ${item.budget.period}
                                                ------------------------------------
                                                • Pagu Anggaran : ${formatRupiah(item.budget.budgetAmount)}
                                                • Realisasi     : ${formatRupiah(item.realization)} (${String.format(Locale.getDefault(), "%.1f", item.percentUsed)}%)
                                                • Sisa Anggaran : ${formatRupiah(item.remaining)}
                                                • Status        : ${if (item.isOver) "OVER BUDGET" else "AMAN"}
                                                ------------------------------------
                                                *Daftar Pengeluaran Terkait:*
                                                ${categoryExpenses.take(5).joinToString("\n") { 
                                                    "• ${it.date}: ${it.name} - ${formatRupiah(it.amount)}" 
                                                }.ifBlank { "(Belum ada transaksi realisasi)" }}
                                                ------------------------------------
                                                _Sistem Kas Terintegrasi_
                                            """.trimIndent()
                                            viewModel.kirimPesanWhatsApp(context, categoryWaText, targetWaPhone)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Kirim Pos ke WA",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Expandable Breakdown of Realized Transactions
                            if (isExpanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Rincian Transaksi Pengeluaran Alokasi Ini:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )

                                if (categoryExpenses.isEmpty()) {
                                    Text(
                                        text = "Belum ada transaksi kas keluar yang tercatat untuk alokasi ini.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                } else {
                                    categoryExpenses.forEach { tx ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = tx.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                                Text(text = "${tx.date} • ${tx.account}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Text(
                                                text = formatRupiah(tx.amount),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Dialog Tambah Anggaran
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Tambah Alokasi Anggaran", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormDropdown(
                        label = "Cakupan Periode",
                        selectedValue = if (periodInput.equals("All", ignoreCase = true)) "Semua Periode" else "Bulanan",
                        options = listOf("Bulanan", "Semua Periode"),
                        onValueChange = { periodInput = if (it == "Semua Periode") "All" else currentPeriod }
                    )

                    if (!periodInput.equals("All", ignoreCase = true)) {
                        IsoDatePickerField(
                            value = periodInput + "-01",
                            label = "Bulan Anggaran",
                            onDateSelected = { periodInput = it.take(7) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    FormDropdown(
                        label = "Pos Alokasi Anggaran",
                        selectedValue = selectedCategory,
                        options = viewModel.masterAlokasi,
                        onValueChange = { selectedCategory = it }
                    )

                    FormDropdown(
                        label = "Proyek",
                        selectedValue = selectedProject.ifBlank { "PT / Umum" },
                        options = listOf("PT / Umum") + projects.map { it.name },
                        onValueChange = { selectedProject = if (it == "PT / Umum") "" else it }
                    )

                    FormDropdown(
                        label = "Kelompok Dana",
                        selectedValue = selectedFundBucket,
                        options = viewModel.masterFundBuckets,
                        onValueChange = { selectedFundBucket = it }
                    )

                    FormDropdown(
                        label = "Pos Alokasi Anggaran",
                        selectedValue = selectedCategory,
                        options = viewModel.masterAlokasi,
                        onValueChange = { selectedCategory = it }
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountInput = it },
                        label = { Text("Pagu Nominal Target (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Catatan / Keterangan Pos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.tambahAnggaran(periodInput, selectedCategory, amt, noteInput, selectedProject, selectedFundBucket)
                            showAddDialog = false
                            amountInput = ""
                            noteInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan Anggaran")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
