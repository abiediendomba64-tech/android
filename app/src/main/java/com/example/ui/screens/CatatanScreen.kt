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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KasViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FormDropdown
import com.example.ui.components.IsoDatePickerField
import com.example.ui.components.formatDateIndo
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatatanScreen(
    viewModel: KasViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val projects by viewModel.activeProjects.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }
    var picInput by remember { mutableStateOf(viewModel.masterPic.first()) }
    var priorityInput by remember { mutableStateOf("Sedang") }
    var statusInput by remember { mutableStateOf("Open") }
    var dateInput by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var selectedProject by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Catatan")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("catatan_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF475569))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "BUKU CATATAN KEUANGAN",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Agenda rekonsiliasi, to-do bendahara, & instruksi kas",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.EditNote,
                        title = "Belum Ada Catatan Keuangan",
                        message = "Tekan tombol + di pojok kanan bawah untuk menambahkan catatan baru."
                    )
                }
            } else {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_card_${note.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val (pColor, pBg) = when (note.priority) {
                                        "Tinggi" -> Pair(ExpenseRed, Color(0xFFFEE2E2))
                                        "Sedang" -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
                                        else -> Pair(Color(0xFF64748B), Color(0xFFF1F5F9))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(pBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = note.priority, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pColor)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (note.status == "Done") Color(0xFFDCFCE7) else Color(0xFFE0E7FF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = note.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (note.status == "Done") IncomeGreen else PrimaryBlue
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.hapusCatatan(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PIC: ${note.pic}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = formatDateIndo(note.date) + if (note.project.isNotBlank()) " • " + note.project else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
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

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tambah Catatan Keuangan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IsoDatePickerField(
                        value = dateInput,
                        label = "Tanggal agenda/catatan",
                        onDateSelected = { dateInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FormDropdown(
                        label = "Proyek",
                        selectedValue = selectedProject.ifBlank { "PT / Umum" },
                        options = listOf("PT / Umum") + projects.map { it.name },
                        onValueChange = { selectedProject = if (it == "PT / Umum") "" else it }
                    )
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Judul Catatan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contentInput,
                        onValueChange = { contentInput = it },
                        label = { Text("Isi Catatan / Tindak Lanjut") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    FormDropdown(
                        label = "PIC",
                        selectedValue = picInput,
                        options = viewModel.masterPic,
                        onValueChange = { picInput = it }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormDropdown(
                            label = "Prioritas",
                            selectedValue = priorityInput,
                            options = listOf("Rendah", "Sedang", "Tinggi"),
                            onValueChange = { priorityInput = it },
                            modifier = Modifier.weight(1f)
                        )

                        FormDropdown(
                            label = "Status",
                            selectedValue = statusInput,
                            options = listOf("Open", "Done", "Follow Up"),
                            onValueChange = { statusInput = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            viewModel.tambahCatatan(titleInput, contentInput, picInput, priorityInput, statusInput, dateInput, selectedProject)
                            titleInput = ""
                            contentInput = ""
                            dateInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            selectedProject = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
