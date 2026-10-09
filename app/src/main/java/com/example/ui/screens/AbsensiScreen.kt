package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.EmployeeEntity
import com.example.ui.KasViewModel
import com.example.ui.components.DetailRow
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.IsoDatePickerField
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
fun AbsensiScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val filteredAttendances by viewModel.filteredAttendances.collectAsStateWithLifecycle()
    val summary by viewModel.attendanceSummary.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val projects by viewModel.activeProjects.collectAsStateWithLifecycle()

    val filterMode by viewModel.attendanceFilterMode.collectAsStateWithLifecycle()
    val selectedDate by viewModel.attendanceSelectedDate.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.attendanceSelectedMonth.collectAsStateWithLifecycle()
    val selectedYear by viewModel.attendanceSelectedYear.collectAsStateWithLifecycle()
    val startDate by viewModel.attendanceStartDate.collectAsStateWithLifecycle()
    val endDate by viewModel.attendanceEndDate.collectAsStateWithLifecycle()

    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showRecordAttendanceDialog by remember { mutableStateOf(false) }
    var showDisburseSalaryDialog by remember { mutableStateOf(false) }
    var showSelectPayslipEmployeeDialog by remember { mutableStateOf(false) }
    var selectedEmployeeForPayslip by remember { mutableStateOf<EmployeeEntity?>(null) }
    var employeeToDelete by remember { mutableStateOf<EmployeeEntity?>(null) }
    var targetWaPhone by remember { mutableStateOf("") }

    // Employee Form States
    var empName by remember { mutableStateOf("") }
    var empPosition by remember { mutableStateOf("") }
    var empDept by remember { mutableStateOf("") }
    var empPhone by remember { mutableStateOf("") }
    var empDailyRate by remember { mutableStateOf("") }
    var empSalary by remember { mutableStateOf("") }
    var empDefaultProject by remember { mutableStateOf("") }

    // Attendance Form States
    var selectedEmpId by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
    var attDateInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var attStatus by remember { mutableStateOf("Hadir") }
    var attTimeIn by remember { mutableStateOf("08:00") }
    var attTimeOut by remember { mutableStateOf("17:00") }
    var attOvertime by remember { mutableStateOf("0") }
    var attNotes by remember { mutableStateOf("") }
    var attProject by remember { mutableStateOf("") }

    val accountNames = accounts.map { it.name }
    var disburseAccount by remember { mutableStateOf(accountNames.firstOrNull().orEmpty()) }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showAddEmployeeDialog = true },
                    containerColor = PrimaryNavy,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp).testTag("fab_add_employee")
                ) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = "Tambah Karyawan", modifier = Modifier.size(20.dp))
                }

                FloatingActionButton(
                    onClick = {
                        if (employees.isNotEmpty()) {
                            selectedEmpId = employees.first().id
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            attDateInput = when (filterMode) {
                                "Bulanan" -> if (today.startsWith(selectedMonth)) today else "${selectedMonth}-01"
                                "Tahunan" -> if (today.startsWith(selectedYear)) today else "${selectedYear}-01-01"
                                "Periodik" -> today.takeIf { it >= startDate && it <= endDate } ?: endDate
                                else -> selectedDate
                            }
                            attProject = employees.first().defaultProject
                            showRecordAttendanceDialog = true
                        }
                    },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_record_attendance")
                ) {
                    Icon(imageVector = Icons.Default.HowToReg, contentDescription = "Catat Absensi")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("absensi_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ABSENSI & PAYROLL KARYAWAN",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Rekapitulasi Harian, Bulanan, Tahunan, & Per Priodik",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val periodLabel = when (filterMode) {
                                        "Harian" -> selectedDate
                                        "Bulanan" -> selectedMonth
                                        "Tahunan" -> selectedYear
                                        else -> "$startDate s/d $endDate"
                                    }

                                    val waText = """
                                        *REKAPITULASI ABSENSI KARYAWAN*
                                        Periode: $filterMode ($periodLabel)
                                        ====================================
                                        ✅ Total Hadir : ${summary.totalHadir}
                                        ⚠️ Total Izin  : ${summary.totalIzin}
                                        🏥 Total Sakit : ${summary.totalSakit}
                                        ❌ Total Alpa  : ${summary.totalAlpa}
                                        🏖️ Total Cuti  : ${summary.totalCuti}
                                        ⏰ Total Lembur: ${summary.totalOvertimeHours} jam
                                        💰 Total Tunjangan/Gaji: ${formatRupiah(summary.totalAllowance)}
                                        ====================================
                                        *Rincian per Karyawan:*
                                        ${employees.joinToString("\n") { emp ->
                                            val empAtts = filteredAttendances.filter { it.employeeId == emp.id }
                                            val hadirCount = empAtts.count { it.status == "Hadir" }
                                            val izinCount = empAtts.count { it.status == "Izin" }
                                            val sakitCount = empAtts.count { it.status == "Sakit" }
                                            val alpaCount = empAtts.count { it.status == "Alpa" }
                                            val totalNominal = empAtts.sumOf { it.dailyAllowance }
                                            "• *${emp.name}* (${emp.department}): $hadirCount Hadir, $izinCount Izin, $sakitCount Sakit, $alpaCount Alpa | Tunjangan: ${formatRupiah(totalNominal)}"
                                        }}
                                        ====================================
                                        _Dibuat otomatis oleh Sistem Kas Terintegrasi_
                                    """.trimIndent()
                                    viewModel.kirimPesanWhatsApp(context, waText, targetWaPhone)
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF25D366))
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Kirim ke WA", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Periodic Filter Chips (Harian, Bulanan, Tahunan, Periodik)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Harian", "Bulanan", "Tahunan", "Periodik").forEach { mode ->
                        FilterChip(
                            selected = filterMode == mode,
                            onClick = { viewModel.attendanceFilterMode.value = mode },
                            label = { Text(mode, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // Period Selector inputs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (filterMode) {
                            "Harian" -> {
                                IsoDatePickerField(
                                    value = selectedDate,
                                    label = "Pilih Tanggal Absensi",
                                    onDateSelected = { viewModel.attendanceSelectedDate.value = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                                    OutlinedButton(
                                        onClick = { viewModel.attendanceSelectedDate.value = todayStr },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Hari Ini", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.absensiCepatSemuaHadir(selectedDate) },
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                                    ) {
                                        Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Absen Cepat Semua Hadir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "Bulanan" -> {
                                IsoDatePickerField(
                                    value = selectedMonth + "-01",
                                    label = "Pilih Bulan",
                                    onDateSelected = { viewModel.attendanceSelectedMonth.value = it.take(7) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "Tahunan" -> {
                                IsoDatePickerField(
                                    value = selectedYear + "-01-01",
                                    label = "Pilih Tahun",
                                    onDateSelected = { viewModel.attendanceSelectedYear.value = it.take(4) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            "Periodik" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IsoDatePickerField(
                                        value = startDate,
                                        label = "Tanggal Mulai",
                                        onDateSelected = { viewModel.attendanceStartDate.value = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IsoDatePickerField(
                                        value = endDate,
                                        label = "Tanggal Akhir",
                                        onDateSelected = { viewModel.attendanceEndDate.value = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // KPI Summary Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Rekapitulasi Kehadiran ($filterMode)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${employees.size} Karyawan Terdaftar",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Hadir: ${summary.totalHadir}", color = IncomeGreen, fontWeight = FontWeight.Bold)
                            Text(text = "Izin: ${summary.totalIzin}", color = TransferAmber, fontWeight = FontWeight.Bold)
                            Text(text = "Sakit: ${summary.totalSakit}", color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                            Text(text = "Alpa: ${summary.totalAlpa}", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Total Lembur: ${summary.totalOvertimeHours} Jam", fontSize = 12.sp)
                            Text(
                                text = "Total Tunjangan/Gaji: ${formatRupiah(summary.totalAllowance)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }

                        // Actions Row: Cairkan Gaji & Cetak Slip Gaji
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showDisburseSalaryDialog = true },
                                enabled = !(filterMode == "Harian" && employees.any { it.monthlySalary > 0.0 }),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cairkan Gaji", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showSelectPayslipEmployeeDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B21A8)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Slip Gaji", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (filterMode == "Harian" && employees.any { it.monthlySalary > 0.0 }) {
                            Text(
                                "Pilih Bulanan atau Periodik untuk mencairkan payroll. Filter Harian akan ikut menghitung gaji pokok bulanan sehingga berisiko membayar gaji penuh berulang.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Breakdown Per Karyawan (When in Bulanan, Tahunan, or Periodik)
            if (employees.isNotEmpty()) {
                item {
                    Text(
                        text = "Rekapitulasi Karyawan & Slip Gaji (${employees.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(employees) { emp ->
                    val empAtts = filteredAttendances.filter { it.employeeId == emp.id }
                    val hadirCount = empAtts.count { it.status == "Hadir" }
                    val izinCount = empAtts.count { it.status == "Izin" }
                    val sakitCount = empAtts.count { it.status == "Sakit" }
                    val alpaCount = empAtts.count { it.status == "Alpa" }
                    val totalTunjangan = empAtts.sumOf { it.dailyAllowance }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = "${emp.position} • Divisi ${emp.department} • Gaji: ${formatRupiah(emp.monthlySalary)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }

                                IconButton(
                                    onClick = { employeeToDelete = emp },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "$hadirCount Hadir • $izinCount Izin • $sakitCount Sakit • $alpaCount Alpa", fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = formatRupiah(totalTunjangan), fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { selectedEmployeeForPayslip = emp },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Slip Gaji", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Attendances List
            item {
                Text(
                    text = "Daftar Absensi (${filteredAttendances.size} data)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredAttendances.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.HowToReg,
                        title = "Belum Ada Catatan Absensi",
                        message = "Gunakan tombol centang di bawah atau tombol 'Absen Cepat Semua Hadir'."
                    )
                }
            } else {
                items(filteredAttendances) { att ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = att.employeeName, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${formatDateIndo(att.date)} • ${att.department} • Masuk: ${att.timeIn} - Keluar: ${att.timeOut}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (att.notes.isNotBlank()) {
                                    Text(text = "Ket: ${att.notes}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val (statusBg, statusText) = when (att.status) {
                                    "Hadir" -> Pair(Color(0xFFDCFCE7), IncomeGreen)
                                    "Izin" -> Pair(Color(0xFFFEF3C7), TransferAmber)
                                    "Sakit" -> Pair(Color(0xFFF3E8FF), Color(0xFF6B21A8))
                                    else -> Pair(Color(0xFFFEE2E2), ExpenseRed)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = att.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusText)
                                }

                                IconButton(
                                    onClick = { viewModel.hapusAbsensi(att.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Modal Add Employee
    if (showAddEmployeeDialog) {
        AlertDialog(
            onDismissRequest = { showAddEmployeeDialog = false },
            title = { Text("Tambah Data Karyawan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empPosition,
                        onValueChange = { empPosition = it },
                        label = { Text("Jabatan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empDept,
                        onValueChange = { empDept = it },
                        label = { Text("Divisi / Bagian") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empPhone,
                        onValueChange = { empPhone = it },
                        label = { Text("No. WhatsApp (628...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empDailyRate,
                        onValueChange = { empDailyRate = it },
                        label = { Text("Uang Harian / Tunjangan Kehadiran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = empSalary,
                        onValueChange = { empSalary = it },
                        label = { Text("Gaji Pokok Bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormDropdown(
                        label = "Alokasi Gaji ke Proyek",
                        selectedValue = empDefaultProject.ifBlank { "PT / Umum" },
                        options = listOf("PT / Umum") + projects.map { it.name },
                        onValueChange = { empDefaultProject = if (it == "PT / Umum") "" else it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = empDailyRate.toDoubleOrNull()
                        val sal = empSalary.toDoubleOrNull()
                        if (empName.isNotBlank() && rate != null && sal != null && rate >= 0.0 && sal >= 0.0) {
                            viewModel.tambahKaryawan(empName, empPosition, empDept, empPhone, rate, sal, empDefaultProject)
                            showAddEmployeeDialog = false
                            empName = ""
                            empPosition = ""
                            empDefaultProject = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan Karyawan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmployeeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Record Attendance
    if (showRecordAttendanceDialog) {
        val currentEmp = employees.firstOrNull { it.id == selectedEmpId }
        val employeeNames = employees.map { "${it.name} (${it.department})" }
        var selectedEmpDropdown by remember {
            mutableStateOf(currentEmp?.let { "${it.name} (${it.department})" } ?: employeeNames.firstOrNull() ?: "")
        }

        AlertDialog(
            onDismissRequest = { showRecordAttendanceDialog = false },
            title = { Text("Catat Kehadiran Karyawan", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IsoDatePickerField(
                        value = attDateInput,
                        label = "Tanggal Absensi *",
                        onDateSelected = { attDateInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("attendance_date_input")
                    )
                    FormDropdown(
                        label = "Pilih Karyawan",
                        selectedValue = selectedEmpDropdown,
                        options = employeeNames,
                        onValueChange = { selected ->
                            selectedEmpDropdown = selected
                            val found = employees.firstOrNull { "${it.name} (${it.department})" == selected }
                            if (found != null) {
                                selectedEmpId = found.id
                                attProject = found.defaultProject
                            }
                        }
                    )

                    FormDropdown(
                        label = "Proyek pekerjaan",
                        selectedValue = attProject.ifBlank { "PT / Umum" },
                        options = listOf("PT / Umum") + projects.map { it.name },
                        onValueChange = { attProject = if (it == "PT / Umum") "" else it }
                    )
                    FormDropdown(
                        label = "Status Kehadiran",
                        selectedValue = attStatus,
                        options = listOf("Hadir", "Izin", "Sakit", "Alpa", "Cuti"),
                        onValueChange = { attStatus = it }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = attTimeIn,
                            onValueChange = { attTimeIn = it },
                            label = { Text("Jam Masuk") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = attTimeOut,
                            onValueChange = { attTimeOut = it },
                            label = { Text("Jam Keluar") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = attOvertime,
                        onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) attOvertime = it },
                        label = { Text("Jam Lembur (Jam)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = attNotes,
                        onValueChange = { attNotes = it },
                        label = { Text("Keterangan Tambahan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emp = employees.firstOrNull { it.id == selectedEmpId } ?: return@Button
                        val ovt = attOvertime.toDoubleOrNull() ?: 0.0
                        val allowance = if (attStatus == "Hadir") emp.dailyRate else 0.0
                        when (filterMode) {
                            "Harian" -> viewModel.attendanceSelectedDate.value = attDateInput
                            "Bulanan" -> viewModel.attendanceSelectedMonth.value = attDateInput.take(7)
                            "Tahunan" -> viewModel.attendanceSelectedYear.value = attDateInput.take(4)
                            "Periodik" -> {
                                if (attDateInput < startDate) viewModel.attendanceStartDate.value = attDateInput
                                if (attDateInput > endDate) viewModel.attendanceEndDate.value = attDateInput
                            }
                        }
                        viewModel.catatAbsensi(
                            employeeId = emp.id,
                            employeeName = emp.name,
                            department = emp.department,
                            date = attDateInput,
                            timeIn = attTimeIn,
                            timeOut = attTimeOut,
                            status = attStatus,
                            overtime = ovt,
                            allowance = allowance,
                            notes = attNotes,
                            project = attProject
                        )
                        showRecordAttendanceDialog = false
                        attNotes = ""
                        attTimeIn = "08:00"
                        attTimeOut = "17:00"
                        attOvertime = "0"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Simpan Absensi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordAttendanceDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Disburse Salary to Kas Keluar
    if (showDisburseSalaryDialog) {
        val totalDisburse = viewModel.hitungTotalPayroll(employees, filteredAttendances)
        val periodText = when (filterMode) {
            "Harian" -> selectedDate
            "Bulanan" -> selectedMonth
            "Tahunan" -> selectedYear
            else -> "$startDate-$endDate"
        }

        AlertDialog(
            onDismissRequest = { showDisburseSalaryDialog = false },
            title = { Text("Cairkan Gaji ke Kas Keluar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pencairan dana tunjangan kehadiran & gaji karyawan berdasarkan rekapitulasi absensi $filterMode ($periodText).",
                        fontSize = 13.sp
                    )
                    DetailRow("Total Nominal Gaji/Tunjangan", formatRupiah(totalDisburse))
                    DetailRow("Total Kehadiran", "${summary.totalHadir} Hari Kerja")

                    HorizontalDivider()

                    if (accountNames.isEmpty()) {
                        Text(
                            "Belum ada akun aktif. Tambahkan akun kas/bank terlebih dahulu sebelum mencairkan payroll.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    } else {
                        FormDropdown(
                            label = "Pilih Akun Sumber Pembayaran",
                            selectedValue = disburseAccount,
                            options = accountNames,
                            onValueChange = { disburseAccount = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = totalDisburse > 0 && disburseAccount.isNotBlank() && accountNames.isNotEmpty(),
                    onClick = {
                        if (totalDisburse > 0 && disburseAccount.isNotBlank()) {
                            viewModel.cairkanGajiAbsensiKeKasKeluar(
                                period = periodText,
                                totalGaji = totalDisburse,
                                accountName = disburseAccount,
                                onSuccess = { showDisburseSalaryDialog = false }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Cairkan Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisburseSalaryDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Modal Pilih Karyawan untuk Slip Gaji
    if (showSelectPayslipEmployeeDialog) {
        AlertDialog(
            onDismissRequest = { showSelectPayslipEmployeeDialog = false },
            title = { Text("Pilih Karyawan untuk Slip Gaji", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (employees.isEmpty()) {
                        Text("Belum ada data karyawan terdaftar.")
                    } else {
                        employees.forEach { emp ->
                            Card(
                                onClick = {
                                    selectedEmployeeForPayslip = emp
                                    showSelectPayslipEmployeeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "${emp.position} • ${emp.department}", fontSize = 11.sp)
                                    }
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSelectPayslipEmployeeDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Modal Tampilan Slip Gaji Resmi
    if (selectedEmployeeForPayslip != null) {
        val emp = selectedEmployeeForPayslip!!
        val empAtts = filteredAttendances.filter { it.employeeId == emp.id }
        val payroll = viewModel.hitungPayrollKaryawan(emp, empAtts)
        val hadirDays = empAtts.count { it.status == "Hadir" }
        val alpaDays = empAtts.count { it.status == "Alpa" }
        val lemburHours = empAtts.sumOf { it.overtimeHours }
        val uangKehadiran = payroll.attendanceAllowance
        val uangLembur = payroll.overtimePay
        val gajiPokok = payroll.baseSalary
        val totalKotor = payroll.gross
        val potonganAlpa = payroll.absenceDeduction
        val totalPotongan = payroll.totalDeduction
        val takeHomePay = payroll.net

        val periodLabel = when (filterMode) {
            "Harian" -> selectedDate
            "Bulanan" -> selectedMonth
            "Tahunan" -> selectedYear
            else -> "$startDate s/d $endDate"
        }

        val payslipText = """
            =========================================
                    SLIP GAJI KARYAWAN RESMI         
                   Sistem Kas         
            Periode: $periodLabel                    
            =========================================
            ID Karyawan : ${emp.id}
            Nama        : ${emp.name}
            Jabatan     : ${emp.position}
            Departemen  : ${emp.department}
            No. WhatsApp: ${emp.phone}
            -----------------------------------------
            PENGHASILAN (PENERIMAAN):
            1. Gaji Pokok             : ${formatRupiah(gajiPokok)}
            2. Uang Kehadiran ($hadirDays Hari): ${formatRupiah(uangKehadiran)}
            3. Uang Lembur ($lemburHours Jam)  : ${formatRupiah(uangLembur)}
            -----------------------------------------
            Total Penghasilan Kotor   : ${formatRupiah(totalKotor)}

            POTONGAN:
            1. Potongan Alpa ($alpaDays Hari)  : (${formatRupiah(potonganAlpa)})
            -----------------------------------------
            Total Potongan            : (${formatRupiah(totalPotongan)})
            -----------------------------------------
            *GAJI BERSIH (TAKE HOME PAY)*: ${formatRupiah(takeHomePay)}
            =========================================
            Status: PERHITUNGAN ABSENSI — BUKAN BUKTI PEMBAYARAN
            Pengesahan: Bagian Keuangan / Bendahara
            =========================================
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { selectedEmployeeForPayslip = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SLIP GAJI KARYAWAN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PERHITUNGAN", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow("Karyawan", "${emp.name} (${emp.id})")
                    DetailRow("Jabatan & Divisi", "${emp.position} • ${emp.department}")
                    DetailRow("Periode", periodLabel)
                    DetailRow("No. WhatsApp", emp.phone)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Rincian Penerimaan:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryNavy)
                    DetailRow("• Gaji Pokok", formatRupiah(gajiPokok))
                    DetailRow("• Tunjangan Kehadiran ($hadirDays Hari)", formatRupiah(uangKehadiran))
                    DetailRow("• Uang Lembur ($lemburHours Jam)", formatRupiah(uangLembur))
                    DetailRow("Total Penerimaan Kotor", formatRupiah(totalKotor))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Potongan:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ExpenseRed)
                    DetailRow("• Potongan Alpa ($alpaDays Hari)", "(${formatRupiah(potonganAlpa)})")
                    DetailRow("Total Potongan", "(${formatRupiah(totalPotongan)})")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Gaji Bersih (THP)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(formatRupiah(takeHomePay), fontWeight = FontWeight.ExtraBold, color = IncomeGreen, fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.kirimPesanWhatsApp(context, payslipText, emp.phone)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ke WA", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, payslipText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Cetak / Bagikan Slip Gaji"))
                        }
                    ) {
                        Text("Cetak / Bagikan", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEmployeeForPayslip = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Modal Konfirmasi Hapus Karyawan Asli
    if (employeeToDelete != null) {
        val toDel = employeeToDelete!!
        AlertDialog(
            onDismissRequest = { employeeToDelete = null },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ExpenseRed) },
            title = { Text("Konfirmasi Hapus Data Karyawan") },
            text = {
                Text("Apakah Anda yakin ingin menghapus data karyawan ${toDel.name} (${toDel.id} - ${toDel.position}) Histori absensi akan dipertahankan; bila masih berelasi, karyawan akan dinonaktifkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hapusKaryawan(toDel.id)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Lanjutkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { employeeToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
