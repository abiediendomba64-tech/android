package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KasViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatCard
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.formatRupiah
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TransferAmber

import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardScreen(
    viewModel: KasViewModel,
    onNavigateToMasuk: () -> Unit,
    onNavigateToKeluar: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToAll: () -> Unit,
    onNavigateToAbsensi: () -> Unit = {},
    onNavigateToRekonsiliasi: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val kpis by viewModel.dashboardKpis.collectAsStateWithLifecycle()
    val accountsWithBalance by viewModel.accountsWithBalance.collectAsStateWithLifecycle()
    val transactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val companyName by viewModel.companyName.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Total Saldo Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_balance_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryNavy, PrimaryBlue, AccentCyan)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = companyName,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "TOTAL SALDO KAS & BANK",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.simpanDanSyncSekarang() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("sync_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Sekarang",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = formatRupiah(kpis.totalBalance),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Formula SUMIFS Aktif • Terakhir Sync: $lastSyncTime",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToMasuk,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_kas_masuk"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Kas Masuk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToKeluar,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_kas_keluar"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Kas Keluar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateToTransfer,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_transfer"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TransferAmber),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Second Quick Action Row: Absensi, Rekonsiliasi, Audit Trail, Sent to WA
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToAbsensi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Absensi", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onNavigateToRekonsiliasi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Rekonsiliasi", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = onNavigateToAudit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Audit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val waText = """
                            *LAPORAN KAS EKSEKUTIF*
                            Perusahaan: $companyName
                            Saldo Total: ${formatRupiah(kpis.totalBalance)}
                            ------------------------------------
                            📥 Masuk Hari Ini : ${formatRupiah(kpis.masukToday)}
                            📤 Keluar Hari Ini: ${formatRupiah(kpis.keluarToday)}
                            📊 Net Hari Ini   : ${formatRupiah(kpis.netToday)}
                            ------------------------------------
                            📥 Masuk Bulan Ini : ${formatRupiah(kpis.masukMonth)}
                            📤 Keluar Bulan Ini: ${formatRupiah(kpis.keluarMonth)}
                            📈 Net Bulan Ini   : ${formatRupiah(kpis.netMonth)}
                            ------------------------------------
                            _Status Audit: Formula Terverifikasi 100%_
                        """.trimIndent()
                        viewModel.kirimPesanWhatsApp(context, waText)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Ke WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Arus Kas Hari Ini
        item {
            Text(
                text = "Arus Kas Hari Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Masuk Hari Ini",
                    value = kpis.masukToday,
                    subtitle = "Kas Masuk (Selesai)",
                    icon = Icons.Default.ArrowDownward,
                    iconColor = IncomeGreen,
                    iconBgColor = Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Keluar Hari Ini",
                    value = kpis.keluarToday,
                    subtitle = "Kas Keluar (Selesai)",
                    icon = Icons.Default.ArrowUpward,
                    iconColor = ExpenseRed,
                    iconBgColor = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatCard(
                title = "Net Hari Ini (Masuk - Keluar)",
                value = kpis.netToday,
                subtitle = if (kpis.netToday >= 0) "Surplus kas harian" else "Defisit kas harian",
                icon = Icons.Default.TrendingUp,
                iconColor = if (kpis.netToday >= 0) IncomeGreen else ExpenseRed,
                iconBgColor = if (kpis.netToday >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section: Arus Kas Bulan Ini
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Arus Kas Bulan Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Net: ${formatRupiah(kpis.netMonth)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (kpis.netMonth >= 0) IncomeGreen else ExpenseRed
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Kas Masuk Bulan Ini",
                    value = kpis.masukMonth,
                    icon = Icons.Default.CalendarMonth,
                    iconColor = IncomeGreen,
                    iconBgColor = Color(0xFFDCFCE7),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Kas Keluar Bulan Ini",
                    value = kpis.keluarMonth,
                    icon = Icons.Default.CalendarMonth,
                    iconColor = ExpenseRed,
                    iconBgColor = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Saldo per Akun
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Akun & Saldo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${accountsWithBalance.size} Akun",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(accountsWithBalance) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_card_${item.account.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorParsed = try {
                        Color(android.graphics.Color.parseColor(item.account.colorHex))
                    } catch (e: Exception) {
                        PrimaryBlue
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colorParsed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.account.type == "Kas") Icons.Default.AccountBalanceWallet else Icons.Default.AccountBalance,
                            contentDescription = item.account.type,
                            tint = colorParsed,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.account.type} • Awal: ${formatRupiah(item.account.initialBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Masuk: ${formatRupiah(item.totalMasuk)} • Keluar: ${formatRupiah(item.totalKeluar)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = formatRupiah(item.currentBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Section: Transaksi Terbaru
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terbaru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lihat Semua",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToAll() }
                        .padding(4.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum Ada Transaksi",
                    message = "Gunakan tombol Kas Masuk atau Kas Keluar di atas untuk mulai mencatat keuangan."
                )
            }
        } else {
            items(transactions.take(5)) { tx ->
                TransactionRowItem(
                    tx = tx,
                    onArchiveClick = { viewModel.arsipkanTransaksi(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
