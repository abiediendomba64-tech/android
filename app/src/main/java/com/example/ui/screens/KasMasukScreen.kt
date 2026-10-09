package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.IsoDatePickerField
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KasMasukScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.activeAccounts.collectAsStateWithLifecycle()
    val projects by viewModel.activeProjects.collectAsStateWithLifecycle()
    val transactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val kasMasukList = transactions.filter { it.type == "MASUK" }
    val totalMasuk = kasMasukList.filter { it.status == "Selesai" }.sumOf { it.amount }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var date by remember { mutableStateOf(todayStr) }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()?.name.orEmpty()) }
    var transactionName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(viewModel.masterKategoriMasuk.first()) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var allocation by remember { mutableStateOf(viewModel.masterAlokasi.first()) }
    var pic by remember { mutableStateOf(viewModel.masterPic.first()) }
    var proofUrl by remember { mutableStateOf("") }
    var receiptNo by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("") }
    var fundBucket by remember { mutableStateOf("PT") }
    var note by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Selesai") }

    // Validation error states
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var formErrorBanner by remember { mutableStateOf<String?>(null) }

    val accountNames = accounts.map { it.name }

    LaunchedEffect(accountNames) {
        if (selectedAccount !in accountNames) selectedAccount = accountNames.firstOrNull().orEmpty()
    }

    fun validateForm(): Boolean {
        var isValid = true
        formErrorBanner = null

        // Validate Amount
        val parsedAmount = amountText.toDoubleOrNull()
        if (amountText.isBlank()) {
            amountError = "Nominal wajib diisi"
            isValid = false
        } else if (parsedAmount == null || parsedAmount <= 0.0) {
            amountError = "Nominal harus lebih besar dari Rp 0"
            isValid = false
        } else {
            amountError = null
        }

        // Validate Category
        if (selectedCategory.isBlank() || selectedCategory !in viewModel.masterKategoriMasuk) {
            categoryError = "Pilih kategori pemasukan yang valid"
            isValid = false
        } else {
            categoryError = null
        }

        // Validate Transaction Name
        if (transactionName.trim().isBlank()) {
            nameError = "Nama transaksi tidak boleh kosong"
            isValid = false
        } else {
            nameError = null
        }

        // Validate Account
        if (selectedAccount.isBlank() || selectedAccount !in accountNames) {
            accountError = "Akun penerima wajib dipilih"
            isValid = false
        } else {
            accountError = null
        }

        // Validate Date
        if (!date.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
            dateError = "Format tanggal harus YYYY-MM-DD"
            isValid = false
        } else {
            dateError = null
        }

        if (!isValid) {
            formErrorBanner = "Mohon lengkapi seluruh field dengan format yang benar."
        }

        return isValid
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("kas_masuk_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IncomeGreen)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FORM KAS MASUK",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Penerimaan uang kas, penjualan, & piutang",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = "Total: ${formatRupiah(totalMasuk)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Error Banner if validation fails
        if (formErrorBanner != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formErrorBanner!!,
                            color = ExpenseRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // The Entry Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kas_masuk_form_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Input Transaksi Kas Masuk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Date & Account
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                                                IsoDatePickerField(
                            value = date,
                            label = "Tanggal *",
                            onDateSelected = { date = it },
                            modifier = Modifier.weight(1f).testTag("km_date_input")
                        )

                        FormDropdown(
                            label = "Akun Masuk *",
                            selectedValue = selectedAccount,
                            options = accountNames,
                            onValueChange = {
                                selectedAccount = it
                                if (accountError != null) accountError = null
                            },
                            isError = accountError != null,
                            errorMessage = accountError,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("km_account_dropdown")
                        )
                    }

                                        // Proyek dan kelompok sumber dana untuk pemisahan laporan.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FormDropdown(
                            label = "Proyek / Alokasi",
                            selectedValue = project.ifBlank { "Umum / PT" },
                            options = listOf("Umum / PT") + projects.map { it.name },
                            onValueChange = { project = if (it == "Umum / PT") "" else it },
                            modifier = Modifier.weight(1f)
                        )
                        FormDropdown(
                            label = "Kelompok Dana",
                            selectedValue = fundBucket,
                            options = viewModel.masterFundBuckets,
                            onValueChange = { fundBucket = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
// Nama Transaksi & Kategori
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = transactionName,
                            onValueChange = {
                                transactionName = it
                                if (nameError != null) nameError = null
                            },
                            label = { Text("Nama Transaksi *") },
                            isError = nameError != null,
                            supportingText = if (nameError != null) {
                                { Text(nameError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("km_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        FormDropdown(
                            label = "Kategori Masuk *",
                            selectedValue = selectedCategory,
                            options = viewModel.masterKategoriMasuk,
                            onValueChange = {
                                selectedCategory = it
                                if (categoryError != null) categoryError = null
                            },
                            isError = categoryError != null,
                            errorMessage = categoryError,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("km_category_dropdown")
                        )
                    }

                    // Nominal with Real-Time Validation and Live Rupiah Preview
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                amountText = input
                                if (amountError != null) amountError = null
                            }
                        },
                        label = { Text("Nominal Penerimaan (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError != null,
                        supportingText = if (amountError != null) {
                            { Text(amountError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("km_amount_input"),
                        leadingIcon = {
                            Text(
                                "Rp",
                                fontWeight = FontWeight.Bold,
                                color = if (amountError != null) ExpenseRed else IncomeGreen,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Live Formatted Rupiah Preview Chip
                    val parsedAmt = amountText.toDoubleOrNull() ?: 0.0
                    if (parsedAmt > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Pratinjau Nominal: ${formatRupiah(parsedAmt)}",
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Keterangan
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Penerimaan (Opsional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("km_desc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Alokasi & PIC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FormDropdown(
                            label = "Alokasi Anggaran",
                            selectedValue = allocation,
                            options = viewModel.masterAlokasi,
                            onValueChange = { allocation = it },
                            modifier = Modifier.weight(1f)
                        )

                        FormDropdown(
                            label = "PIC",
                            selectedValue = pic,
                            options = viewModel.masterPic,
                            onValueChange = { pic = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // No. Bukti & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = receiptNo,
                            onValueChange = { receiptNo = it },
                            label = { Text("No. Bukti / Kwitansi") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        FormDropdown(
                            label = "Status",
                            selectedValue = status,
                            options = viewModel.masterStatus,
                            onValueChange = { status = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Bukti URL
                    OutlinedTextField(
                        value = proofUrl,
                        onValueChange = { proofUrl = it },
                        label = { Text("Bukti Foto / Drive Link URL") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Submit Button with Validation Gate
                    Button(
                        onClick = {
                            if (validateForm()) {
                                val amount = amountText.toDouble()
                                viewModel.simpanKasMasuk(
                                    date = date,
                                    account = selectedAccount,
                                    name = transactionName.trim(),
                                    category = selectedCategory,
                                    description = description.trim(),
                                    amount = amount,
                                    allocation = allocation,
                                    pic = pic,
                                    proofUrl = proofUrl.trim(),
                                    receiptNo = receiptNo.trim(),
                                    project = project.trim(),
                                    note = note.trim(),
                                    status = status,
                                    fundBucket = fundBucket
                                ) {
                                    amountText = ""
                                    description = ""
                                    receiptNo = ""
                                    proofUrl = ""
                                    formErrorBanner = null
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("km_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "SIMPAN KAS MASUK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Ledger Table Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buku Kas Masuk (${kasMasukList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Selesai: ${formatRupiah(totalMasuk)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }
        }

        if (kasMasukList.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum Ada Catatan Kas Masuk",
                    message = "Isi formulir di atas lalu tekan Simpan Kas Masuk untuk menambahkan entri."
                )
            }
        } else {
            items(kasMasukList) { tx ->
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
