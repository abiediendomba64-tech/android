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
    var empDept by remember { mutableStateOf("Operasional") }
    var empPhone by remember { mutableStateOf("") }
    var empDailyRate by remember { mutableStateOf("150000") }
    var empSalary by remember { mutableStateOf("3500000") }

    // Attendance Form States
    var selectedEmpId by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
    var attStatus by remember { mutableStateOf("Hadir") }
    var attTimeIn by remember { mutableStateOf("08:00") }
    var attTimeOut by remember { mutableStateOf("17:00") }
    var attOvertime by remember { mutableStateOf("0") }
    var attNotes by remember { mutableStateOf("") }

    val accountNames = accounts.map { it.name }.ifEmpty { listOf("Bank BCA", "Kas Tunai") }
    var disburseAccount by remember { mutableStateOf(accountNames.first()) }

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
                Text("Apakah Anda yakin ingin menghapus data karyawan ${toDel.name} (${toDel.id} - ${toDel.position}) secara permanen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.hapusKaryawan(toDel.id)
                        employeeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Hapus Permanen")
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
