package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProjectPlanEntity
import com.example.data.model.TransactionEntity
import com.example.ui.KasViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.IsoDatePickerField
import com.example.ui.components.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KalenderScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.projectPlans.collectAsStateWithLifecycle()
    val projects by viewModel.activeProjects.collectAsStateWithLifecycle()
    val transactions by viewModel.ledgerTransactions.collectAsStateWithLifecycle()
    var showAddPlan by remember { mutableStateOf(false) }
    var planDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var selectedProject by remember { mutableStateOf("") }
    var planTitle by remember { mutableStateOf("") }
    var planCategory by remember { mutableStateOf(viewModel.masterProjectPlanCategories.first()) }
    var estimateText by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    val settled = transactions.filter { it.status == "Selesai" && it.type != "TRANSFER" }
    val actualIn = settled.filter { it.type == "MASUK" }.sumOf { it.amount }
    val actualOut = settled.filter { it.type == "KELUAR" }.sumOf { it.amount }
    val dailyActuals = settled.groupBy { it.date }.toSortedMap(compareByDescending { it })

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPlan = true }) {
                androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Tambah rencana kalender")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Text("KALENDER RENCANA & ARUS KAS AKTUAL", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        Text("Pemasukan aktual terselesaikan: ${formatRupiah(actualIn)}", fontWeight = FontWeight.SemiBold)
                        Text("Pengeluaran aktual terselesaikan: ${formatRupiah(actualOut)}", fontWeight = FontWeight.SemiBold)
                        Text("Net aktual: ${formatRupiah(actualIn - actualOut)}", fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        Text("Agenda dan estimasi di bawah ini tersimpan sebagai rencana. Agenda tidak menambah saldo, omzet, atau arus kas sampai transaksi nyata dicatat.", fontSize = 11.sp)
                    }
                }
            }
            item {
                Text("RENCANA / JADWAL TERSIMPAN (${plans.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            if (plans.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.CalendarMonth,
                        title = "Belum Ada Rencana",
                        message = "Tekan tombol + untuk menjadwalkan pembebasan tanah, cut & fill, pembayaran, pembangunan unit, pemasaran, atau agenda PT."
                    )
                }
            } else {
                items(plans.sortedWith(compareBy<ProjectPlanEntity> { it.planDate }.thenBy { it.createdAt }), key = { it.id }) { plan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(plan.title, fontWeight = FontWeight.Bold)
                                    Text(plan.planDate + " • " + plan.project.ifBlank { "PT / Umum" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(plan.status, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(plan.category + " • Estimasi " + formatRupiah(plan.estimatedAmount), fontSize = 12.sp)
                            if (plan.details.isNotBlank()) Text(plan.details, fontSize = 12.sp)
                            Text("Rencana saja — tidak masuk ke buku besar.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (plan.status == "Direncanakan") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.ubahStatusRencana(plan.id, "Selesai") },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Tandai Selesai", fontSize = 11.sp) }
                                    TextButton(onClick = { viewModel.ubahStatusRencana(plan.id, "Batal") }) { Text("Batalkan") }
                                }
                            } else if (plan.status != "Direncanakan") {
                                Text("Status rencana diperbarui; bila terjadi pembayaran/penerimaan, catat transaksi aktual terpisah.", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            item {
                Text("TRANSAKSI AKTUAL TERBARU", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            if (dailyActuals.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.CalendarMonth,
                        title = "Belum Ada Arus Kas Aktual",
                        message = "Transaksi yang sudah dicatat dan berstatus Selesai akan muncul di bagian ini."
                    )
                }
            } else {
                dailyActuals.entries.take(10).forEach { (date, entries) ->
                    item(key = "actual-date-$date") {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(date, fontWeight = FontWeight.Bold)
                                val incoming = entries.filter { it.type == "MASUK" }.sumOf { it.amount }
                                val outgoing = entries.filter { it.type == "KELUAR" }.sumOf { it.amount }
                                Text("Masuk ${formatRupiah(incoming)} • Keluar ${formatRupiah(outgoing)} • Net ${formatRupiah(incoming - outgoing)}", fontSize = 11.sp)
                                entries.sortedBy { it.time }.take(4).forEach { tx: TransactionEntity ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(tx.name, fontSize = 12.sp)
                                            Text(tx.project.ifBlank { tx.fundBucket } + " • " + tx.account, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text((if (tx.type == "MASUK") "+" else "-") + formatRupiah(tx.amount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }

    if (showAddPlan) {
        AlertDialog(
            onDismissRequest = { showAddPlan = false },
            title = { Text("Tambah Rencana Kalender", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    IsoDatePickerField(
                        value = planDate,
                        label = "Tanggal rencana",
                        onDateSelected = { planDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FormDropdown(
                        label = "Proyek",
                        selectedValue = selectedProject.ifBlank { "PT / Umum" },
                        options = listOf("PT / Umum") + projects.map { it.name },
                        onValueChange = { selectedProject = if (it == "PT / Umum") "" else it }
                    )
                    OutlinedTextField(
                        value = planTitle,
                        onValueChange = { planTitle = it },
                        label = { Text("Agenda / pekerjaan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    FormDropdown(
                        label = "Kategori rencana",
                        selectedValue = planCategory,
                        options = viewModel.masterProjectPlanCategories,
                        onValueChange = { planCategory = it }
                    )
                    OutlinedTextField(
                        value = estimateText,
                        onValueChange = { if (it.all(Char::isDigit)) estimateText = it },
                        label = { Text("Estimasi biaya (Rp, isi 0 bila tidak ada)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("PIC / detail tindak lanjut") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = planTitle.isNotBlank() && (estimateText.toDoubleOrNull() ?: 0.0) >= 0.0,
                    onClick = {
                        viewModel.tambahRencana(
                            project = selectedProject,
                            date = planDate,
                            title = planTitle,
                            category = planCategory,
                            estimatedAmount = estimateText.toDoubleOrNull() ?: 0.0,
                            details = details
                        )
                        planTitle = ""
                        estimateText = ""
                        details = ""
                        showAddPlan = false
                    }
                ) { Text("Simpan Rencana") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlan = false }) { Text("Batal") }
            }
        )
    }
}
