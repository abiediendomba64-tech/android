package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryNavy
import kotlinx.coroutines.launch

@Composable
fun TransaksiAllScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Semua") }
    var selectedAccount by remember { mutableStateOf("Semua") }
    var selectedStatus by remember { mutableStateOf("Semua") }

    val filteredList = transactions.filter { tx ->
        val matchSearch = searchQuery.isBlank() ||
                tx.name.contains(searchQuery, ignoreCase = true) ||
                tx.description.contains(searchQuery, ignoreCase = true) ||
                tx.id.contains(searchQuery, ignoreCase = true) ||
                tx.receiptNo.contains(searchQuery, ignoreCase = true) ||
                tx.pic.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true)

        val matchType = when (selectedType) {
            "Semua" -> true
            "Kas Masuk" -> tx.type == "MASUK"
            "Kas Keluar" -> tx.type == "KELUAR"
            "Transfer" -> tx.type == "TRANSFER"
            else -> true
        }

        val matchAccount = selectedAccount == "Semua" ||
                tx.account.equals(selectedAccount, ignoreCase = true) ||
                tx.toAccount?.equals(selectedAccount, ignoreCase = true) == true

        val matchStatus = selectedStatus == "Semua" || tx.status.equals(selectedStatus, ignoreCase = true)

        matchSearch && matchType && matchAccount && matchStatus
    }

    val totalMasuk = filteredList.filter { it.type == "MASUK" && it.status == "Selesai" }.sumOf { it.amount }
    val totalKeluar = filteredList.filter { it.type == "KELUAR" && it.status == "Selesai" }.sumOf { it.amount }
    val net = totalMasuk - totalKeluar

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transaksi_all_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BUKU BESAR SEMUA AKUN",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val csv = viewModel.exportCsv()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                        type = "text/csv"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Ekspor CSV Transaksi"))
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Ekspor CSV",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Masuk", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                text = formatRupiah(totalMasuk),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                        Column {
                            Text(text = "Total Keluar", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                text = formatRupiah(totalKeluar),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                        }
                        Column {
                            Text(text = "Net Arus", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                text = formatRupiah(net),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari transaksi, nomor bukti, PIC, kategori...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Bersihkan")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("all_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Type Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "Kas Masuk", "Kas Keluar", "Transfer").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Account Filter Chips
        item {
            val accFilterOptions = listOf("Semua") + accounts.map { it.name }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accFilterOptions.forEach { acc ->
                    FilterChip(
                        selected = selectedAccount == acc,
                        onClick = { selectedAccount = acc },
                        label = { Text(acc, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Result count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ditemukan: ${filteredList.size} Transaksi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredList.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Tidak Ada Transaksi Sesuai Filter",
                    message = "Coba ubah kata kunci pencarian atau bersihkan filter tipe & akun."
                )
            }
        } else {
            items(filteredList) { tx ->
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
