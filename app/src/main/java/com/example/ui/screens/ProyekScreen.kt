package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Domain
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
import androidx.compose.runtime.collectAsState
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
import com.example.ui.KasViewModel
import com.example.data.model.HousingUnitEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.IsoDatePickerField
import com.example.ui.components.formatRupiah

@Composable
fun ProyekScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.activeProjects.collectAsStateWithLifecycle()
    val transactions by viewModel.ledgerTransactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val housingUnits by viewModel.housingUnits.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(viewModel.masterProjectCategories.first()) }
    var businessModel by remember { mutableStateOf("Tidak berlaku") }
    var location by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var targetDate by remember { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())) }
    var budgetText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showAddUnitDialog by remember { mutableStateOf(false) }
    var unitProjectName by remember { mutableStateOf("") }
    var unitCode by remember { mutableStateOf("") }
    var unitBlock by remember { mutableStateOf("") }
    var unitPosition by remember { mutableStateOf("") }
    var unitLandArea by remember { mutableStateOf("") }
    var unitBuildingArea by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var unitBuyer by remember { mutableStateOf("") }
    var unitNotes by remember { mutableStateOf("") }
    var unitToMarkSold by remember { mutableStateOf<HousingUnitEntity?>(null) }
    var soldBuyerName by remember { mutableStateOf("") }

    val settled = transactions.filter { it.status == "Selesai" }
    val projectTransactions = settled.filter { it.project.isNotBlank() }
    val totalProjectIn = projectTransactions.filter { it.type == "MASUK" }.sumOf { it.amount }
    val totalProjectOut = projectTransactions.filter { it.type == "KELUAR" }.sumOf { it.amount }
    val fundSummary = viewModel.masterFundBuckets.map { bucket ->
        val rows = settled.filter { it.fundBucket.equals(bucket, ignoreCase = true) && it.type != "TRANSFER" }
        Triple(bucket, rows.filter { it.type == "MASUK" }.sumOf { it.amount }, rows.filter { it.type == "KELUAR" }.sumOf { it.amount })
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Tambah proyek")
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
                        Text("DASHBOARD PROYEK & USAHA", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text("Proyek aktif: ${projects.size}  •  Pagu master: ${formatRupiah(projects.sumOf { it.budgetAmount })}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Pemasukan aktual proyek", fontSize = 11.sp)
                                Text(formatRupiah(totalProjectIn), fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Pengeluaran aktual proyek", fontSize = 11.sp)
                                Text(formatRupiah(totalProjectOut), fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Angka hanya dari transaksi berstatus Selesai; rencana kalender dan pagu bukan uang masuk/keluar.", fontSize = 11.sp)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PEMISAHAN KELOMPOK DANA", fontWeight = FontWeight.Bold)
                        fundSummary.forEach { (bucket, inflow, outflow) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(bucket, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Masuk ${formatRupiah(inflow)}", fontSize = 11.sp)
                                }
                                Column {
                                    Text("Keluar", fontSize = 11.sp)
                                    Text(formatRupiah(outflow), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                }
                            }
                        }
                        HorizontalDivider()
                        Text("Kelompok dana adalah label pembukuan. Saldo kas/bank tetap berasal dari akun nyata; transfer antar akun netral.", fontSize = 11.sp)
                    }
                }
            }
            if (projects.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Domain,
                        title = "Belum Ada Master Proyek",
                        message = "Tambahkan proyek pertama: pembebasan tanah, cut & fill, perumahan subsidi/komersial, perdagangan, atau operasional PT."
                    )
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    val rows = projectTransactions.filter { it.project.equals(project.name, ignoreCase = true) }
                    val inflow = rows.filter { it.type == "MASUK" }.sumOf { it.amount }
                    val outflow = rows.filter { it.type == "KELUAR" }.sumOf { it.amount }
                    val remaining = project.budgetAmount - outflow
                    val budgetRows = budgets.filter { it.project.equals(project.name, ignoreCase = true) }
                    val projectUnits = housingUnits.filter { it.project.equals(project.name, ignoreCase = true) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(project.category + if (project.businessModel != "Tidak berlaku") " • " + project.businessModel else "", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(project.status, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if (project.location.isNotBlank()) Text("Lokasi: ${project.location}", fontSize = 12.sp)
                            Text("Jadwal: ${project.startDate} s/d ${project.targetEndDate}", fontSize = 11.sp)
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Pagu proyek", fontSize = 11.sp)
                                    Text(formatRupiah(project.budgetAmount), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Keluar aktual", fontSize = 11.sp)
                                    Text(formatRupiah(outflow), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Sisa pagu", fontSize = 11.sp)
                                    Text(formatRupiah(remaining), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Masuk: ${formatRupiah(inflow)}", fontSize = 12.sp)
                                Text("Net aktual: ${formatRupiah(inflow - outflow)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Pos anggaran terikat proyek: ${budgetRows.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (project.category == "Perumahan") {
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text("UNIT / KAVLING SITE PLAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Segmen ${project.businessModel} • ${projectUnits.size} unit terdaftar", fontSize = 11.sp)
                                    }
                                    Button(onClick = {
                                        unitProjectName = project.name
                                        unitCode = ""
                                        unitBlock = ""
                                        unitPosition = ""
                                        unitLandArea = ""
                                        unitBuildingArea = ""
                                        unitPrice = ""
                                        unitBuyer = ""
                                        unitNotes = ""
                                        showAddUnitDialog = true
                                    }) { Text("Tambah Unit", fontSize = 11.sp) }
                                }
                                if (projectUnits.isEmpty()) {
                                    Text("Belum ada daftar unit. Tambahkan kode kavling dan posisi blok sesuai site plan.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    projectUnits.forEach { unit ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Unit ${unit.unitCode}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(unit.status, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                }
                                                Text("Blok ${unit.block.ifBlank { "-" }} • Posisi ${unit.sitePosition.ifBlank { "-" }}", fontSize = 11.sp)
                                                Text("Tanah ${unit.landAreaM2} m² • Bangunan ${unit.buildingAreaM2} m² • Harga ${formatRupiah(unit.salePrice)}", fontSize = 11.sp)
                                                if (unit.buyerName.isNotBlank()) Text("Pembeli: ${unit.buyerName}", fontSize = 11.sp)
                                                if (unit.notes.isNotBlank()) Text(unit.notes, fontSize = 11.sp)
                                                if (unit.status == "Tersedia" || unit.status == "Booking") {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        if (unit.status == "Tersedia") {
                                                            TextButton(onClick = { viewModel.ubahStatusUnit(unit.id, "Booking", unit.buyerName) }) { Text("Booking") }
                                                        }
                                                        TextButton(onClick = {
                                                            unitToMarkSold = unit
                                                            soldBuyerName = unit.buyerName
                                                        }) { Text("Tandai Terjual") }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Text("Status unit tidak menambah pendapatan. Catat pembayaran pembeli sebagai transaksi Kas Masuk dan tautkan ke proyek ${project.name}.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (project.notes.isNotBlank()) Text(project.notes, fontSize = 12.sp)
                            if (rows.isEmpty()) Text("Belum ada transaksi aktual yang ditautkan ke proyek ini.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else {
                                HorizontalDivider()
                                Text("TRANSAKSI AKTUAL TERBARU", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                rows.sortedWith(compareByDescending<com.example.data.model.TransactionEntity> { it.date }.thenByDescending { it.time }).take(4).forEach { tx ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(tx.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text("${tx.date} • ${tx.account} • ${tx.fundBucket}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text((if (tx.type == "MASUK") "+" else "-") + formatRupiah(tx.amount), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Master Proyek", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Nama proyek unik") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    FormDropdown(
                        label = "Jenis proyek",
                        selectedValue = category,
                        options = viewModel.masterProjectCategories,
                        onValueChange = {
                            category = it
                            businessModel = if (it == "Perumahan") "Subsidi" else "Tidak berlaku"
                        }
                    )
                    if (category == "Perumahan") {
                        FormDropdown(
                            label = "Segmen perumahan",
                            selectedValue = businessModel,
                            options = listOf("Subsidi", "Komersial"),
                            onValueChange = { businessModel = it }
                        )
                    }
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lokasi / kawasan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    IsoDatePickerField(
                        value = startDate,
                        label = "Tanggal mulai proyek",
                        onDateSelected = { startDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    IsoDatePickerField(
                        value = targetDate,
                        label = "Target selesai",
                        onDateSelected = { targetDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { if (it.all(Char::isDigit)) budgetText = it },
                        label = { Text("Pagu proyek (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Ruang lingkup / catatan") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Text("Nama master proyek dipakai untuk menghubungkan transaksi, absensi, anggaran, catatan, dan kalender.", fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = projectName.isNotBlank() && (budgetText.toDoubleOrNull() ?: 0.0) >= 0.0 && targetDate >= startDate,
                    onClick = {
                        val budget = budgetText.toDoubleOrNull() ?: 0.0
                        viewModel.tambahProyek(projectName, category, businessModel, location, startDate, targetDate, budget, notes)
                        projectName = ""
                        location = ""
                        budgetText = ""
                        notes = ""
                        showAddDialog = false
                    }
                ) { Text("Simpan Proyek") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showAddUnitDialog) {
        val project = projects.firstOrNull { it.name.equals(unitProjectName, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showAddUnitDialog = false },
            title = { Text("Tambah Unit / Kavling", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Proyek: $unitProjectName • Segmen ${project?.businessModel ?: "-"}", fontSize = 12.sp)
                    OutlinedTextField(
                        value = unitCode,
                        onValueChange = { unitCode = it },
                        label = { Text("Kode unit/kavling (unik)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unitBlock,
                            onValueChange = { unitBlock = it },
                            label = { Text("Blok") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitPosition,
                            onValueChange = { unitPosition = it },
                            label = { Text("Posisi/site plan") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = unitLandArea,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) unitLandArea = it },
                        label = { Text("Luas tanah m²") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitBuildingArea,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) unitBuildingArea = it },
                        label = { Text("Luas bangunan m² (0 bila tanah kavling)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitPrice,
                        onValueChange = { if (it.all(Char::isDigit)) unitPrice = it },
                        label = { Text("Harga jual unit (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitBuyer,
                        onValueChange = { unitBuyer = it },
                        label = { Text("Nama calon pembeli (opsional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = unitNotes,
                        onValueChange = { unitNotes = it },
                        label = { Text("Catatan posisi / detail site plan") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Text("Kode, blok, dan posisi adalah data inventaris site plan; bukan peta gambar/CAD.", fontSize = 10.sp)
                }
            },
            confirmButton = {
                val land = unitLandArea.toDoubleOrNull() ?: 0.0
                val building = unitBuildingArea.toDoubleOrNull() ?: 0.0
                val price = unitPrice.toDoubleOrNull() ?: 0.0
                Button(
                    enabled = project?.category == "Perumahan" && unitCode.isNotBlank() && land > 0.0 && building >= 0.0 && price > 0.0,
                    onClick = {
                        viewModel.tambahUnitPerumahan(
                            projectName = unitProjectName,
                            unitCode = unitCode,
                            block = unitBlock,
                            sitePosition = unitPosition,
                            landAreaM2 = land,
                            buildingAreaM2 = building,
                            salePrice = price,
                            buyerName = unitBuyer,
                            notes = unitNotes
                        )
                        showAddUnitDialog = false
                    }
                ) { Text("Simpan Unit") }
            },
            dismissButton = {
                TextButton(onClick = { showAddUnitDialog = false }) { Text("Batal") }
            }
        )
    }

    if (unitToMarkSold != null) {
        val unit = unitToMarkSold!!
        AlertDialog(
            onDismissRequest = { unitToMarkSold = null },
            title = { Text("Tandai Unit Terjual") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unit ${unit.unitCode} • ${unit.project}")
                    OutlinedTextField(
                        value = soldBuyerName,
                        onValueChange = { soldBuyerName = it },
                        label = { Text("Nama pembeli wajib") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Tindakan ini hanya mengubah status inventaris. Uang muka/pelunasan tetap harus dicatat sebagai Kas Masuk dengan bukti transaksi.", fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = soldBuyerName.isNotBlank(),
                    onClick = {
                        viewModel.ubahStatusUnit(unit.id, "Terjual", soldBuyerName)
                        unitToMarkSold = null
                    }
                ) { Text("Konfirmasi Terjual") }
            },
            dismissButton = {
                TextButton(onClick = { unitToMarkSold = null }) { Text("Batal") }
            }
        )
    }

}
