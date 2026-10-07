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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KasViewModel
import com.example.ui.components.FormDropdown
import com.example.ui.components.formatRupiah
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy

@Composable
fun AkunKategoriScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var showAddAccountDialog by remember { mutableStateOf(false) }

    var accountNameInput by remember { mutableStateOf("") }
    var accountTypeInput by remember { mutableStateOf("Bank") }
    var initialBalanceInput by remember { mutableStateOf("0") }
    var colorHexInput by remember { mutableStateOf("#2563EB") }

    val companyName by viewModel.companyName.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("akun_kategori_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MASTER AKUN & KATEGORI",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Kelola rekening kas/bank dan master dropdown sistem",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Section: Daftar Akun
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Akun Kas / Bank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddAccountDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Akun", fontSize = 12.sp)
                }
            }
        }

        items(accounts) { acc ->
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorParsed = try {
                        Color(android.graphics.Color.parseColor(acc.colorHex))
                    } catch (e: Exception) {
                        PrimaryBlue
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colorParsed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = colorParsed,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = acc.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${acc.type} • Saldo Awal: ${formatRupiah(acc.initialBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.hapusAkun(acc.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Section: Master Kategori Lists
        item {
            Text(
                text = "Kategori Masuk Terdaftar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.masterKategoriMasuk.forEach { kat ->
                        Text(text = "• $kat", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Text(
                text = "Kategori Keluar Terdaftar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.masterKategoriKeluar.forEach { kat ->
                        Text(text = "• $kat", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Tambah Akun Kas / Bank", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = accountNameInput,
                        onValueChange = { accountNameInput = it },
                        label = { Text("Nama Akun (misal: Bank Mandiri)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormDropdown(
                        label = "Jenis Akun",
                        selectedValue = accountTypeInput,
                        options = listOf("Kas", "Bank", "E-Wallet", "Lainnya"),
                        onValueChange = { accountTypeInput = it }
                    )

                    OutlinedTextField(
                        value = initialBalanceInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) initialBalanceInput = it },
                        label = { Text("Saldo Awal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormDropdown(
                        label = "Warna Akun",
                        selectedValue = colorHexInput,
                        options = listOf("#16A34A", "#2563EB", "#0284C7", "#D97706", "#7C3AED", "#DC2626"),
                        onValueChange = { colorHexInput = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initBal = initialBalanceInput.toDoubleOrNull() ?: 0.0
                        if (accountNameInput.isNotBlank()) {
                            viewModel.tambahAkun(accountNameInput, accountTypeInput, initBal, colorHexInput)
                            accountNameInput = ""
                            initialBalanceInput = "0"
                            showAddAccountDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
