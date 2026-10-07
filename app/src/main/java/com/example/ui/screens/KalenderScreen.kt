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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KasViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatDateIndo
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryNavy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyCashflowSummary(
    val date: String,
    val totalMasuk: Double,
    val totalKeluar: Double,
    val net: Double,
    val countMasuk: Int,
    val countKeluar: Int,
    val status: String // "Positif", "Minus", "Nol"
)

@Composable
fun KalenderScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.activeTransactions.collectAsStateWithLifecycle()

    val dailySummaries = remember(transactions) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Last 30 days
        val list = mutableListOf<DailyCashflowSummary>()
        for (i in 0 until 30) {
            val d = dateFormat.format(cal.time)
            val dayTx = transactions.filter { it.date == d && it.status == "Selesai" }

            val masuk = dayTx.filter { it.type == "MASUK" }.sumOf { it.amount }
            val keluar = dayTx.filter { it.type == "KELUAR" }.sumOf { it.amount }
            val net = masuk - keluar
            val cMasuk = dayTx.count { it.type == "MASUK" }
            val cKeluar = dayTx.count { it.type == "KELUAR" }

            val status = when {
                net > 0 -> "Positif"
                net < 0 -> "Minus"
                else -> "Nol"
            }

            if (dayTx.isNotEmpty() || i < 7) {
                list.add(
                    DailyCashflowSummary(
                        date = d,
                        totalMasuk = masuk,
                        totalKeluar = keluar,
                        net = net,
                        countMasuk = cMasuk,
                        countKeluar = cKeluar,
                        status = status
                    )
                )
            }
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("kalender_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "KALENDER ARUS KAS",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Rekapitulasi penerimaan, pengeluaran & net per tanggal",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        if (dailySummaries.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.CalendarMonth,
                    title = "Belum Ada Data Kalender",
                    message = "Data akan muncul otomatis seiring transaksi dicatat."
                )
            }
        } else {
            items(dailySummaries) { day ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatDateIndo(day.date),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Masuk: ${formatRupiah(day.totalMasuk)} (${day.countMasuk} tx) • Keluar: ${formatRupiah(day.totalKeluar)} (${day.countKeluar} tx)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatRupiah(day.net),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (day.net >= 0) IncomeGreen else ExpenseRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val (badgeBg, badgeText) = when (day.status) {
                                "Positif" -> Pair(Color(0xFFDCFCE7), IncomeGreen)
                                "Minus" -> Pair(Color(0xFFFEE2E2), ExpenseRed)
                                else -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = day.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeText
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
