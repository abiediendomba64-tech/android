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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.formatRupiah
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.TransferAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransferScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val transactions by viewModel.activeTransactions.collectAsStateWithLifecycle()
    val transferList = transactions.filter { it.type == "TRANSFER" }
    val totalTransfer = transferList.filter { it.status == "Selesai" }.sumOf { it.amount }

    val accountNames = accounts.map { it.name }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var date by remember { mutableStateOf(todayStr) }
    var fromAccount by remember { mutableStateOf(accountNames.firstOrNull().orEmpty()) }
    var toAccount by remember { mutableStateOf(accountNames.getOrNull(1).orEmpty()) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("Transfer saldo antar akun") }
    var pic by remember { mutableStateOf(viewModel.masterPic.first()) }
    var proofUrl by remember { mutableStateOf("") }
    var receiptNo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Selesai") }

    // Validation states
    var amountError by remember { mutableStateOf<String?>(null) }
    var accountMatchError by remember { mutableStateOf<String?>(null) }
    var formErrorBanner by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        formErrorBanner = null

        val amt = amountText.toDoubleOrNull()
        if (amountText.isBlank()) {
            amountError = "Nominal transfer wajib diisi"
            isValid = false
        } else if (amt == null || amt <= 0.0) {
            amountError = "Nominal transfer harus lebih besar dari Rp 0"
            isValid = false
        } else {
            amountError = null
        }

        if (fromAccount.isBlank() || fromAccount !in accountNames || toAccount.isBlank() || toAccount !in accountNames) {
            accountMatchError = "Pilih akun asal dan tujuan yang terdaftar"
            isValid = false
        } else if (fromAccount == toAccount) {
            accountMatchError = "Akun asal dan akun tujuan tidak boleh sama"
            isValid = false
        } else {
            accountMatchError = null
        }

        if (!isValid) {
            formErrorBanner = "Mohon periksa kembali input transfer saldo Anda."
        }

        return isValid
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transfer_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TransferAmber)
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
                            text = "TRANSFER ANTAR AKUN",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Pemindahan saldo kas / rekening bank",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text(
                        text = "Total: ${formatRupiah(totalTransfer)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Error Banner
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

        // Form
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transfer_form_card"),
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
                        text = "Form Transfer Saldo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                                        IsoDatePickerField(
                        value = date,
                        label = "Tanggal Transfer",
                        onDateSelected = { date = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dari Akun & Ke Akun
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormDropdown(
                            label = "Dari Akun (Sumber) *",
                            selectedValue = fromAccount,
                            options = accountNames,
                            onValueChange = {
                                fromAccount = it
                                if (accountMatchError != null) accountMatchError = null
                            },
                            isError = accountMatchError != null,
                            errorMessage = accountMatchError,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = TransferAmber,
                            modifier = Modifier.size(24.dp)
                        )

                        FormDropdown(
                            label = "Ke Akun (Tujuan) *",
                            selectedValue = toAccount,
                            options = accountNames,
                            onValueChange = {
                                toAccount = it
                                if (accountMatchError != null) accountMatchError = null
                            },
                            isError = accountMatchError != null,
                            errorMessage = accountMatchError,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Nominal
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                amountText = input
                                if (amountError != null) amountError = null
                            }
                        },
                        label = { Text("Nominal Transfer (Rp) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError != null,
                        supportingText = if (amountError != null) {
                            { Text(amountError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tr_amount_input"),
                        leadingIcon = {
                            Text(
                                "Rp",
                                fontWeight = FontWeight.Bold,
                                color = if (amountError != null) ExpenseRed else TransferAmber,
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
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Pratinjau Nominal: ${formatRupiah(parsedAmt)}",
                                color = TransferAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Keterangan
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan Transfer") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // PIC & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FormDropdown(
                            label = "PIC Transfer",
                            selectedValue = pic,
                            options = viewModel.masterPic,
                            onValueChange = { pic = it },
                            modifier = Modifier.weight(1f)
                        )

                        FormDropdown(
                            label = "Status",
                            selectedValue = status,
                            options = viewModel.masterStatus,
                            onValueChange = { status = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // No. Bukti & Bukti URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = receiptNo,
                            onValueChange = { receiptNo = it },
                            label = { Text("No. Bukti / Referensi") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )

                        OutlinedTextField(
                            value = proofUrl,
                            onValueChange = { proofUrl = it },
                            label = { Text("Bukti Foto/URL") },
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (validate()) {
                                val amount = amountText.toDouble()
                                viewModel.simpanTransfer(
                                    date = date,
                                    fromAccount = fromAccount,
                                    toAccount = toAccount,
                                    amount = amount,
                                    description = description,
                                    pic = pic,
                                    proofUrl = proofUrl,
                                    receiptNo = receiptNo,
                                    note = note,
                                    status = status,
                                    fundBucket = fundBucket
                                ) {
                                    amountText = ""
                                    receiptNo = ""
                                    proofUrl = ""
                                    formErrorBanner = null
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("tr_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TransferAmber),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "SIMPAN TRANSFER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Ledger History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Transfer Antar Akun (${transferList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (transferList.isEmpty()) {
            item {
                EmptyStateView(
                    title = "Belum Ada Riwayat Transfer",
                    message = "Gunakan form di atas untuk mencatat pemindahan saldo antar kas/bank."
                )
            }
        } else {
            items(transferList) { tx ->
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
