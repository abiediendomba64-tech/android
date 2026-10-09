package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KasViewModel
import com.example.ui.components.DetailRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryNavy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LaporanScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.ledgerTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val kpis by viewModel.dashboardKpis.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { dateFormat.format(Date()) }
    val startOfMonth = remember {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        dateFormat.format(cal.time)
    }

    var startDate by remember { mutableStateOf(startOfMonth) }
    var endDate by remember { mutableStateOf(today) }
    var selectedAccount by remember { mutableStateOf("Semua") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedStatus by remember { mutableStateOf("Selesai") }

    val accountFilterOptions = listOf("Semua") + accounts.map { it.name }
    val categoryFilterOptions = listOf("Semua") + viewModel.masterKategoriMasuk + viewModel.masterKategoriKeluar

    // Filtered reporting data
    val reportTransactions = transactions.filter { tx ->
        val inDateRange = tx.date >= startDate && tx.date <= endDate
        val matchAccount = selectedAccount == "Semua" || tx.account.equals(selectedAccount, ignoreCase = true)
        val matchCategory = selectedCategory == "Semua" || tx.category.equals(selectedCategory, ignoreCase = true)
        val matchStatus = selectedStatus == "Semua" || tx.status.equals(selectedStatus, ignoreCase = true)

        inDateRange && matchAccount && matchCategory && matchStatus
    }

    val totalMasuk = reportTransactions.filter { it.type == "MASUK" }.sumOf { it.amount }
    val totalKeluar = reportTransactions.filter { it.type == "KELUAR" }.sumOf { it.amount }
    val netLaporan = totalMasuk - totalKeluar

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("laporan_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Share Header
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
                    Column {
                        Text(
                            text = "LAPORAN KEUANGAN KAS",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Rekapitulasi berkala & analisa arus kas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val text = """
                                    *LAPORAN KEUANGAN SISTEM KAS*
                                    Periode: $startDate s/d $endDate
                                    Akun: $selectedAccount | Kategori: $selectedCategory
                                    ------------------------------------
                                    📥 Total Kas Masuk : ${formatRupiah(totalMasuk)}
                                    📤 Total Kas Keluar: ${formatRupiah(totalKeluar)}
                                    📊 Net Arus Kas   : ${formatRupiah(netLaporan)}
                                    📑 Total Transaksi: ${reportTransactions.size}
                                    💰 Saldo Kas Saat Ini: ${formatRupiah(kpis.totalBalance)}
                                    ------------------------------------
                                    _Sistem Kas_
                                """.trimIndent()
                                viewModel.kirimPesanWhatsApp(context, text)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF25D366))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Kirim ke WA",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                val text = """
                                    LAPORAN KEUANGAN SISTEM KAS
                                    Periode: $startDate s/d $endDate
                                    Akun: $selectedAccount | Kategori: $selectedCategory
                                    
                                    Total Kas Masuk : ${formatRupiah(totalMasuk)}
                                    Total Kas Keluar: ${formatRupiah(totalKeluar)}
                                    Net Arus Kas   : ${formatRupiah(netLaporan)}
                                    Total Transaksi: ${reportTransactions.size}
                                    Saldo Kas Saat Ini: ${formatRupiah(kpis.totalBalance)}
                                """.trimIndent()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Laporan"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Bagikan",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Filter Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Parameter Laporan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Tgl Mulai") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("Tgl Akhir") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FormDropdown(
                            label = "Filter Akun",
                            selectedValue = selectedAccount,
                            options = accountFilterOptions,
                            onValueChange = { selectedAccount = it },
                            modifier = Modifier.weight(1f)
                        )

                        FormDropdown(
                            label = "Filter Kategori",
                            selectedValue = selectedCategory,
                            options = categoryFilterOptions.distinct(),
                            onValueChange = { selectedCategory = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // KPI Summary Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ringkasan Hasil Periode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    DetailRow("Total Kas Masuk", formatRupiah(totalMasuk))
                    DetailRow("Total Kas Keluar", formatRupiah(totalKeluar))
                    HorizontalDivider()
                    DetailRow("Net Arus Kas (Surplus/Defisit)", formatRupiah(netLaporan))
                    DetailRow("Jumlah Transaksi", "${reportTransactions.size} transaksi")
                    DetailRow("Total Saldo Kas Saat Ini", formatRupiah(kpis.totalBalance))
                }
            }
        }

        // Section: Daftar Transaksi Terkait
        item {
            Text(
                text = "Rincian Transaksi (${reportTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (reportTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Assessment,
                    title = "Tidak Ada Data Transaksi",
                    message = "Tidak ada transaksi dalam rentang tanggal atau filter yang dipilih."
                )
            }
        } else {
            items(reportTransactions) { tx ->
                TransactionRowItem(tx = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
