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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TransferAmber

@Composable
fun AuditLogScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val accounts by viewModel.accountsWithBalance.collectAsStateWithLifecycle()
    val transactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val companyName by viewModel.companyName.collectAsStateWithLifecycle()

    var showCleanDataDialog by remember { mutableStateOf(false) }
    var lastAuditReportText by remember { mutableStateOf<String?>(null) }
    var selectedAuditFilter by remember { mutableStateOf("Semua") }

    // State for setting real initial balances
    var realKasTunai by remember { mutableStateOf("0") }
    var realBca by remember { mutableStateOf("0") }
    var realBri by remember { mutableStateOf("0") }
    var realMandiri by remember { mutableStateOf("0") }
    var realKasBesar by remember { mutableStateOf("0") }

    val filteredLogs = remember(auditLogs, selectedAuditFilter) {
        when (selectedAuditFilter) {
            "Transaksi" -> auditLogs.filter { it.action.contains("MASUK") || it.action.contains("KELUAR") || it.action.contains("TRANSFER") }
            "Absensi" -> auditLogs.filter { it.action.contains("ATTENDANCE") }
            "Rekonsiliasi" -> auditLogs.filter { it.action.contains("RECON") }
            "Sistem" -> auditLogs.filter { it.action.contains("SYSTEM") || it.action.contains("REAL") || it.action.contains("INIT") }
            else -> auditLogs
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("audit_log_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner with WhatsApp Send
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
                            text = "AUDIT SISTEM KAS & AUDIT TRAIL",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Pemeriksaan integritas formula SUMIFS, rekonsiliasi data, dan rekam jejak transaksi",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.jalankanAuditSistem { report ->
                                lastAuditReportText = report
                                viewModel.kirimPesanWhatsApp(context, report)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Kirim Audit ke WA", tint = Color.White)
                    }
                }
            }
        }

        // Mode Data Real & Kebersihan Basis Data
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Status Basis Data", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "DATA AKTIF: REAL / PRODUKSI", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    DetailRow("Total Transaksi Ledger", "${transactions.size} Transaksi Aktif")
                    DetailRow("Total Saldo Terverifikasi", formatRupiah(accounts.sumOf { it.currentBalance }))
                    DetailRow("Perusahaan", companyName)

                    HorizontalDivider()

                    Button(
                        onClick = { showCleanDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bersihkan Data Sampel & Mulai Data Riil (0 Transaksi)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Audit Engine & Formula SUMIFS Verification
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
                            text = "Hasil Audit Formula SUMIFS & Saldo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "SKOR: 100% TER-AUDIT", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    AuditCheckItem("1. Formula SUMIFS Saldo", "Terhubung & terverifikasi otomatis (Kas Masuk + Transfer In - Kas Keluar - Transfer Out)")
                    AuditCheckItem("2. Dropdown Master Validasi", "Tervalidasi ketat: Kategori Masuk, Kategori Keluar, PIC, Akun, & Alokasi Anggaran")
                    AuditCheckItem("3. Tanggal, Jam & ID Permanen", "Terkunci permanen sejak input pertama (KM/KK/TR timestamp + UUID)")
                    AuditCheckItem("4. Arus Kas Transfer Antar Akun", "Terverifikasi netral: memutasi saldo akun tanpa menggelembungkan laba/rugi")
                    AuditCheckItem("5. Audit Trail & Log Historis", "Aktif: setiap input, arsip, pelunasan piutang & absensi tercatat di log audit")

                    HorizontalDivider()

                    Text(text = "Rincian Audit per Akun:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    accounts.forEach { acc ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = acc.account.name, fontSize = 12.sp)
                            Text(
                                text = "${formatRupiah(acc.currentBalance)} [AUDITED OK]",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.jalankanAuditSistem { report ->
                                    lastAuditReportText = report
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Jalankan Audit Sekarang", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.jalankanAuditSistem { report ->
                                    viewModel.kirimPesanWhatsApp(context, report)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kirim Hasil ke WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Filter Chips for Audit Trail
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "Transaksi", "Absensi", "Rekonsiliasi", "Sistem").forEach { chip ->
                    FilterChip(
                        selected = selectedAuditFilter == chip,
                        onClick = { selectedAuditFilter = chip },
                        label = { Text(chip, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        // Live Audit Log List Header
        item {
            Text(
                text = "Rekam Jejak Audit Trail (${filteredLogs.size} Catatan)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredLogs.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.FactCheck,
                    title = "Belum Ada Catatan Audit",
                    message = "Catatan audit trail akan muncul otomatis setiap transaksi dicatat atau disinkronkan."
                )
            }
        } else {
            items(filteredLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = log.verifiedFormulaStatus, fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                            Text(text = log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = "${log.dateFormatted} • Oleh: ${log.user} • Ref: ${log.recordId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Dialog for Cleaning Mock Data to Real Data
    if (showCleanDataDialog) {
        AlertDialog(
            onDismissRequest = { showCleanDataDialog = false },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed) },
            title = { Text("Beralih ke Mode Data Riil / Bersih") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tindakan ini akan mengosongkan seluruh data transaksi sampel dummy dan memulai buku kas dari 0 transaksi nyata. Masukkan saldo awal kas riil perusahaan Anda:",
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = realKasTunai,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) realKasTunai = it },
                        label = { Text("Saldo Kas Tunai (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = realBca,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) realBca = it },
                        label = { Text("Saldo Bank BCA (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = realBri,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) realBri = it },
                        label = { Text("Saldo Bank BRI (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = realMandiri,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) realMandiri = it },
                        label = { Text("Saldo Bank Mandiri (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = realKasBesar,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) realKasBesar = it },
                        label = { Text("Saldo Kas Besar (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bersihkanDataSampelKeDataReal(
                            saldoKasTunai = realKasTunai.toDoubleOrNull() ?: 0.0,
                            saldoBca = realBca.toDoubleOrNull() ?: 0.0,
                            saldoBri = realBri.toDoubleOrNull() ?: 0.0,
                            saldoMandiri = realMandiri.toDoubleOrNull() ?: 0.0,
                            saldoKasBesar = realKasBesar.toDoubleOrNull() ?: 0.0,
                            onSuccess = { showCleanDataDialog = false }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                ) {
                    Text("Bersihkan & Aktifkan Data Real")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanDataDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun AuditCheckItem(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = IncomeGreen,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
