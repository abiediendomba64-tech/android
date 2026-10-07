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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.ReceivableEntity
import com.example.ui.KasViewModel
import com.example.ui.components.DetailRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.formatDateIndo
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TransferAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PiutangScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allRecords by viewModel.receivables.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val accountNames = accounts.map { it.name }

    var activeTab by remember { mutableStateOf("Piutang") } // "Piutang" or "Hutang"

    val piutangList = allRecords.filter { it.type == "PIUTANG" }
    val hutangList = allRecords.filter { it.type == "HUTANG" }
    val displayList = if (activeTab == "Piutang") piutangList else hutangList

    val totalAmount = displayList.sumOf { it.totalAmount }
    val totalPaid = displayList.sumOf { it.paidAmount }
    val totalRemaining = displayList.sumOf { it.remainingAmount }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedForPayment by remember { mutableStateOf<ReceivableEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ReceivableEntity?>(null) }

    // Add form states
    var partyName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var dueDateInput by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf(accountNames.firstOrNull().orEmpty()) }
    var notesInput by remember { mutableStateOf("") }

    // Pay form states
    var payAmountInput by remember { mutableStateOf("") }
    var payAccount by remember { mutableStateOf(accountNames.firstOrNull().orEmpty()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = if (activeTab == "Piutang") Color(0xFF6B21A8) else ExpenseRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_piutang_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Data")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("piutang_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (activeTab == "Piutang") Color(0xFF6B21A8) else Color(0xFF991B1B))
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
                                text = if (activeTab == "Piutang") "BUKU PIUTANG USAHA (UANG MASUK)" else "BUKU HUTANG DAGANG (UANG KELUAR)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (activeTab == "Piutang") "Tagihan ke pelanggan / pihak ketiga yang belum lunas" else "Kewajiban pembayaran ke supplier / vendor yang harus dibayar",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        IconButton(
                            onClick = {
                                val waText = """
                                    *REKAPITULASI ${if (activeTab == "Piutang") "PIUTANG USAHA" else "HUTANG DAGANG"}*
                                    Total Pagu: ${formatRupiah(totalAmount)}
                                    Total Terbayar: ${formatRupiah(totalPaid)}
                                    Sisa Tagihan: ${formatRupiah(totalRemaining)}
                                    ====================================
                                    *Daftar ${if (activeTab == "Piutang") "Pelanggan" else "Supplier"}:*
                                    ${displayList.joinToString("\n") { 
                                        "• *${it.customerName}*: Tagihan ${formatRupiah(it.totalAmount)} | Sisa: ${formatRupiah(it.remainingAmount)} [Jatuh Tempo: ${it.dueDate}] [${if (it.isSettled) "LUNAS" else "BELUM LUNAS"}]" 
                                    }}
                                    ====================================
                                    _Sistem Kas Terintegrasi_
                                """.trimIndent()
                                viewModel.kirimPesanWhatsApp(context, waText)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF25D366))
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Kirim ke WA", tint = Color.White)
                        }
                    }
                }
            }

            // Tab Selector: Piutang vs Hutang
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = activeTab == "Piutang",
                        onClick = { activeTab = "Piutang" },
                        label = { Text("Piutang Usaha (${piutangList.size})", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = activeTab == "Hutang",
                        onClick = { activeTab = "Hutang" },
                        label = { Text("Hutang Dagang (${hutangList.size})", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Financial Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailRow("Total Nominal ${if (activeTab == "Piutang") "Piutang" else "Hutang"}", formatRupiah(totalAmount))
                        DetailRow("Sudah Dibayar / Dilunasi", formatRupiah(totalPaid))
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sisa yang Belum Lunas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatRupiah(totalRemaining),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeTab == "Piutang") IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = "Daftar ${if (activeTab == "Piutang") "Piutang Pelanggan" else "Hutang Supplier"} (${displayList.size} Data)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (displayList.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.CreditCard,
                        title = "Belum Ada Catatan $activeTab",
                        message = "Tekan tombol + di pojok kanan bawah untuk mencatat data $activeTab baru."
                    )
                }
            } else {
                items(displayList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_card_${item.id}"),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ID: ${item.id} • ${item.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (item.isSettled) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (item.isSettled) "LUNAS" else "BELUM LUNAS",
                                            color = if (item.isSettled) IncomeGreen else TransferAmber,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Real delete button
                                    IconButton(
                                        onClick = { itemToDelete = item },
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

                            DetailRow("Tanggal Transaksi", formatDateIndo(item.date))
                            DetailRow("Jatuh Tempo", formatDateIndo(item.dueDate))
                            DetailRow("Akun Terhubung", item.targetAccount)
                            DetailRow("Total Nominal", formatRupiah(item.totalAmount))
                            DetailRow("Sudah Dibayar", formatRupiah(item.paidAmount))
                            DetailRow("Sisa Tagihan", formatRupiah(item.remainingAmount))

                            if (item.notes.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${item.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!item.isSettled) {
                                    Button(
                                        onClick = {
                                            selectedForPayment = item
                                            payAmountInput = item.remainingAmount.toLong().toString()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = if (activeTab == "Piutang") "Terima Pelunasan" else "Bayar Hutang", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val waText = if (activeTab == "Piutang") {
                                            """
                                                *PENGINGAT TAGIHAN PIUTANG*
                                                Kepada: ${item.customerName}
                                                ------------------------------------
                                                Tagihan: ${item.description}
                                                Total Tagihan : ${formatRupiah(item.totalAmount)}
                                                Sudah Dibayar : ${formatRupiah(item.paidAmount)}
                                                *Sisa Tagihan : ${formatRupiah(item.remainingAmount)}*
                                                Jatuh Tempo   : ${item.dueDate}
                                                ------------------------------------
                                                Mohon konfirmasi pembayaran ke rekening ${item.targetAccount}. Terima kasih.
                                            """.trimIndent()
                                        } else {
                                            """
                                                *KONFIRMASI HUTANG PEMBELIAN*
                                                Kepada Supplier: ${item.customerName}
                                                ------------------------------------
                                                Keterangan: ${item.description}
                                                Total Kewajiban: ${formatRupiah(item.totalAmount)}
                                                Telah Dibayar  : ${formatRupiah(item.paidAmount)}
                                                *Sisa Hutang   : ${formatRupiah(item.remainingAmount)}*
                                                ------------------------------------
                                                Pembayaran akan diproses via ${item.targetAccount}. Terima kasih.
                                            """.trimIndent()
                                        }
                                        viewModel.kirimPesanWhatsApp(context, waText)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Ke WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // Modal Tambah Piutang / Hutang
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Tambah Catatan $activeTab", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text(if (activeTab == "Piutang") "Nama Pelanggan / Debitur" else "Nama Supplier / Kreditur") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Tagihan / Barang") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountInput = it },
                        label = { Text("Nominal Total (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dueDateInput,
                        onValueChange = { dueDateInput = it },
                        label = { Text("Tanggal Jatuh Tempo (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormDropdown(
                        label = if (activeTab == "Piutang") "Akun Kas Penerimaan" else "Akun Kas Pembayaran",
                        selectedValue = selectedAccount,
                        options = accountNames,
                        onValueChange = { selectedAccount = it }
                    )

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Catatan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        val validDueDate = dueDateInput.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
                        if (partyName.isNotBlank() && amt > 0 && selectedAccount in accountNames && validDueDate) {
                            if (activeTab == "Piutang") {
                                viewModel.tambahPiutang(
                                    customerName = partyName,
                                    description = description,
                                    totalAmount = amt,
                                    dueDate = dueDateInput,
                                    targetAccount = selectedAccount,
                                    notes = notesInput
                                )
                            } else {
                                viewModel.tambahHutang(
                                    supplierName = partyName,
                                    description = description,
                                    totalAmount = amt,
                                    dueDate = dueDateInput,
                                    sourceAccount = selectedAccount,
                                    notes = notesInput
                                )
                            }
                            showAddDialog = false
                            partyName = ""
                            description = ""
                            amountInput = ""
                            notesInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan $activeTab")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Bayar / Lunasi
    if (selectedForPayment != null) {
        val target = selectedForPayment!!
        val isPiutang = target.type == "PIUTANG"
        AlertDialog(
            onDismissRequest = { selectedForPayment = null },
            title = { Text(text = if (isPiutang) "Terima Pembayaran Piutang" else "Bayar Tagihan Hutang", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Pihak: ${target.customerName}", fontWeight = FontWeight.SemiBold)
                    Text(text = "Sisa yang harus diselesaikan: ${formatRupiah(target.remainingAmount)}", fontSize = 13.sp)

                    HorizontalDivider()

                    OutlinedTextField(
                        value = payAmountInput,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) payAmountInput = it },
                        label = { Text("Nominal Pembayaran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormDropdown(
                        label = if (isPiutang) "Kas/Bank Penerima (Kas Masuk)" else "Kas/Bank Sumber (Kas Keluar)",
                        selectedValue = payAccount,
                        options = accountNames,
                        onValueChange = { payAccount = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && amt <= target.remainingAmount && payAccount in accountNames) {
                            if (isPiutang) {
                                viewModel.bayarPiutang(target.id, amt, payAccount)
                            } else {
                                viewModel.bayarHutang(target.id, amt, payAccount)
                            }
                            selectedForPayment = null
                            payAmountInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Proses Transaksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedForPayment = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Konfirmasi Hapus Asli (Bukan Fake)
    if (itemToDelete != null) {
        val toDel = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed) },
            title = { Text("Konfirmasi Hapus Data") },
            text = {
                Text("Apakah Anda yakin ingin menghapus data ${toDel.type} (${toDel.customerName} - ${formatRupiah(toDel.totalAmount)}) secara permanen dari basis data?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hapusPiutang(toDel.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
