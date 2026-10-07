package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.components.FormDropdown
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TransferAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RekonsiliasiArusKasScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accountsWithBalance.collectAsStateWithLifecycle()
    val budgetRealizations by viewModel.budgetRealizations.collectAsStateWithLifecycle()
    val bankRecons by viewModel.bankReconciliations.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("Arus Kas") } // "Arus Kas", "Rekonsiliasi Bank", "Rincian Alokasi"

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { dateFormat.format(Date()) }
    val startOfMonth = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        dateFormat.format(cal.time)
    }

    var startDate by remember { mutableStateOf(startOfMonth) }
    var endDate by remember { mutableStateOf(today) }

    // Bank Recon Form States
    val bankAccountNames = accounts.filter { it.account.type == "Bank" || it.account.type == "Kas" }.map { it.account.name }
        
    var reconAccountName by remember { mutableStateOf(bankAccountNames.first()) }
    var reconPeriod by remember { mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())) }
    var statementBalanceInput by remember { mutableStateOf("") }
    var reconNotes by remember { mutableStateOf("") }
    var targetWaPhone by remember { mutableStateOf("") }

    val cashFlow = viewModel.hitungArusKas(startDate, endDate)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("rekonsiliasi_arus_kas_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header with WhatsApp Sender
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
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
                            text = "ARUS KAS & REKONSILIASI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Laporan Arus Kas, Rekonsiliasi Bank, & Rincian Alokasi",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    IconButton(
                        onClick = {
                            val waText = when (activeTab) {
                                "Arus Kas" -> """
                                    *LAPORAN ARUS KAS LENGKAP*
                                    Periode: $startDate s/d $endDate
                                    ====================================
                                    📊 *1. ARUS KAS OPERASIONAL*
                                    • Penerimaan: ${formatRupiah(cashFlow.operatingInflow)}
                                    • Pengeluaran: (${formatRupiah(cashFlow.operatingOutflow)})
                                    • Net Operasional: ${formatRupiah(cashFlow.netOperatingFlow)}
                                    
                                    🏗️ *2. ARUS KAS INVESTASI / PROYEK*
                                    • Pengeluaran Proyek: (${formatRupiah(cashFlow.investingOutflow)})
                                    • Net Investasi: ${formatRupiah(cashFlow.netInvestingFlow)}
                                    
                                    💵 *3. ARUS KAS PENDANAAN*
                                    • Modal Masuk: ${formatRupiah(cashFlow.financingInflow)}
                                    • Net Pendanaan: ${formatRupiah(cashFlow.netFinancingFlow)}
                                    
                                    📈 *RINGKASAN TOTAL ARUS KAS*
                                    • Saldo Awal: ${formatRupiah(cashFlow.openingBalance)}
                                    • Net Perubahan Kas: ${formatRupiah(cashFlow.netCashChange)}
                                    • Saldo Akhir Kas: ${formatRupiah(cashFlow.closingBalance)}
                                    ====================================
                                    _Sistem Kas Terintegrasi_
                                """.trimIndent()

                                "Rekonsiliasi Bank" -> {
                                    val currentAcc = accounts.firstOrNull { it.account.name == reconAccountName }
                                    val currentBookBal = currentAcc?.currentBalance ?: 0.0
                                    val stAmt = statementBalanceInput.toDoubleOrNull() ?: currentBookBal
                                    val diff = stAmt - currentBookBal
                                    """
                                        *BERITA ACARA REKONSILIASI BANK & KAS*
                                        Akun: $reconAccountName
                                        Periode: $reconPeriod
                                        ====================================
                                        • Saldo Buku Kas (Sistem) : ${formatRupiah(currentBookBal)}
                                        • Saldo Rekening Koran    : ${formatRupiah(stAmt)}
                                        • Selisih Rekonsiliasi    : ${formatRupiah(diff)}
                                        • Status                 : ${if (Math.abs(diff) < 1.0) "✅ COCOK (MATCH)" else "⚠️ SELISIH (UNMATCHED)"}
                                        • Catatan                : ${reconNotes.ifBlank { "Tidak ada selisih" }}
                                        ====================================
                                        _Diverifikasi oleh Auditor Keuangan_
                                    """.trimIndent()
                                }

                                else -> """
                                    *RINCIAN ALOKASI & REALISASI ANGGARAN*
                                    Periode: $reconPeriod
                                    ====================================
                                    ${budgetRealizations.joinToString("\n\n") { b ->
                                        "📌 *${b.budget.category}*\n• Pagu: ${formatRupiah(b.budget.budgetAmount)}\n• Realisasi: ${formatRupiah(b.realization)} (${String.format(Locale.getDefault(), "%.1f", b.percentUsed)}%)\n• Sisa: ${formatRupiah(b.remaining)} [${if (b.isOver) "OVER BUDGET" else "AMAN"}]"
                                    }}
                                    ====================================
                                    _Sistem Kas Terintegrasi_
                                """.trimIndent()
                            }
                            viewModel.kirimPesanWhatsApp(context, waText, targetWaPhone)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Sent to WA", tint = Color.White)
                    }
                }
            }
        }

        // Tab Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Arus Kas", "Rekonsiliasi Bank", "Rincian Alokasi").forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        when (activeTab) {
            "Arus Kas" -> {
                // Period Filters
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = startDate,
                                    onValueChange = { startDate = it },
                                    label = { Text("Tgl Mulai") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endDate,
                                    onValueChange = { endDate = it },
                                    label = { Text("Tgl Akhir") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        startDate = startOfMonth
                                        endDate = today
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Bulan Ini", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.DAY_OF_YEAR, 1)
                                        startDate = dateFormat.format(cal.time)
                                        endDate = today
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Tahun Ini", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Cash Flow Statement Cards
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "1. Arus Kas dari Aktivitas Operasional",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DetailRow("Penerimaan Kas Operasional", formatRupiah(cashFlow.operatingInflow))
                            DetailRow("Pengeluaran Kas Operasional", "(${formatRupiah(cashFlow.operatingOutflow)})")
                            HorizontalDivider()
                            DetailRow("Net Arus Kas Operasional", formatRupiah(cashFlow.netOperatingFlow))

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "2. Arus Kas dari Aktivitas Investasi & Proyek",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DetailRow("Pengeluaran Proyek / Unit", "(${formatRupiah(cashFlow.investingOutflow)})")
                            HorizontalDivider()
                            DetailRow("Net Arus Kas Investasi", formatRupiah(cashFlow.netInvestingFlow))

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "3. Arus Kas dari Aktivitas Pendanaan / Modal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            DetailRow("Setoran Modal / Pembiayaan", formatRupiah(cashFlow.financingInflow))
                            HorizontalDivider()
                            DetailRow("Net Arus Kas Pendanaan", formatRupiah(cashFlow.netFinancingFlow))

                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DetailRow("Saldo Awal Kas & Bank", formatRupiah(cashFlow.openingBalance))
                                    DetailRow("Net Perubahan Kas", formatRupiah(cashFlow.netCashChange))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "Saldo Akhir Kas & Bank", fontWeight = FontWeight.Bold)
                                        Text(text = formatRupiah(cashFlow.closingBalance), fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Rekonsiliasi Bank" -> {
                // Bank Reconciliation Form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Form Rekonsiliasi Rekening Bank / Kas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            FormDropdown(
                                label = "Pilih Akun Kas / Bank",
                                selectedValue = reconAccountName,
                                options = bankAccountNames,
                                onValueChange = { reconAccountName = it }
                            )

                            val currentBookBal = accounts.firstOrNull { it.account.name == reconAccountName }?.currentBalance ?: 0.0

                            DetailRow("Saldo Buku Kas (Sistem)", formatRupiah(currentBookBal))

                            OutlinedTextField(
                                value = reconPeriod,
                                onValueChange = { reconPeriod = it },
                                label = { Text("Periode Rekonsiliasi (YYYY-MM)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = statementBalanceInput,
                                onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '-' }) statementBalanceInput = it },
                                label = { Text("Saldo Rekening Koran / Kas Fisik (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            val statementBal = statementBalanceInput.toDoubleOrNull() ?: currentBookBal
                            val diff = statementBal - currentBookBal
                            val isMatched = Math.abs(diff) < 1.0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Selisih: ${formatRupiah(diff)}", fontWeight = FontWeight.Bold, color = if (isMatched) IncomeGreen else ExpenseRed)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isMatched) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = if (isMatched) "STATUS: COCOK (MATCH)" else "STATUS: SELISIH", fontWeight = FontWeight.Bold, color = if (isMatched) IncomeGreen else ExpenseRed, fontSize = 11.sp)
                                }
                            }

                            OutlinedTextField(
                                value = reconNotes,
                                onValueChange = { reconNotes = it },
                                label = { Text("Catatan Penyesuaian Rekonsiliasi") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val stAmt = statementBalanceInput.toDoubleOrNull()
                                    if (stAmt != null) {
                                        viewModel.simpanRekonsiliasiBank(reconAccountName, reconPeriod, stAmt, reconNotes)
                                        statementBalanceInput = ""
                                        reconNotes = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Berita Acara Rekonsiliasi")
                            }
                        }
                    }
                }

                // History of Reconciliations
                item {
                    Text(text = "Riwayat Rekonsiliasi Bank (${bankRecons.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(bankRecons) { recon ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${recon.accountName} (${recon.period})", fontWeight = FontWeight.Bold)
                                Text(text = recon.status, color = if (recon.status == "Cocok") IncomeGreen else ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                            DetailRow("Saldo Buku", formatRupiah(recon.bookBalance))
                            DetailRow("Saldo Koran", formatRupiah(recon.statementBalance))
                            DetailRow("Selisih", formatRupiah(recon.difference))
                            if (recon.notes.isNotBlank()) {
                                Text(text = "Catatan: ${recon.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            "Rincian Alokasi" -> {
                // Breakdown of Budget Division & Allocation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Pembagian & Rincian Alokasi Anggaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            val totalBudgetAllocated = budgetRealizations.sumOf { it.budget.budgetAmount }
                            val totalSpent = budgetRealizations.sumOf { it.realization }

                            DetailRow("Total Pagu Alokasi", formatRupiah(totalBudgetAllocated))
                            DetailRow("Total Realisasi", formatRupiah(totalSpent))
                            DetailRow("Sisa Anggaran", formatRupiah(totalBudgetAllocated - totalSpent))

                            HorizontalDivider()

                            budgetRealizations.forEach { b ->
                                val percentOfTotal = if (totalBudgetAllocated > 0) (b.budget.budgetAmount / totalBudgetAllocated) * 100.0 else 0.0
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = b.budget.category, fontWeight = FontWeight.Bold)
                                        Text(text = "${String.format(Locale.getDefault(), "%.1f", percentOfTotal)}% porsi total", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    LinearProgressIndicator(
                                        progress = { (b.percentUsed / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = if (b.isOver) ExpenseRed else IncomeGreen
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "Alokasi: ${formatRupiah(b.budget.budgetAmount)}", fontSize = 11.sp)
                                        Text(text = "Realisasi: ${formatRupiah(b.realization)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (b.isOver) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                                        Text(text = "Sisa: ${formatRupiah(b.remaining)}", fontSize = 11.sp, color = if (b.remaining < 0) ExpenseRed else IncomeGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
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
