package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.KasViewModel
import com.example.ui.screens.AbsensiScreen
import com.example.ui.screens.AkunKategoriScreen
import com.example.ui.screens.AnggaranScreen
import com.example.ui.screens.ArsipScreen
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CatatanScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KalenderScreen
import com.example.ui.screens.KalkulatorScreen
import com.example.ui.screens.KasKeluarScreen
import com.example.ui.screens.KasMasukScreen
import com.example.ui.screens.LaporanScreen
import com.example.ui.screens.PiutangScreen
import com.example.ui.screens.RekonsiliasiArusKasScreen
import com.example.ui.screens.TransaksiAllScreen
import com.example.ui.screens.TransferScreen
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.HowToReg
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.TransferAmber
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    KAS_MASUK("Kas Masuk", Icons.Default.AddCircle),
    KAS_KELUAR("Kas Keluar", Icons.Default.RemoveCircle),
    TRANSFER("Transfer Antar Akun", Icons.Default.SwapHoriz),
    TRANSAKSI_ALL("Buku Besar / Semua", Icons.Default.ListAlt),
    ABSENSI("Absensi Karyawan", Icons.Default.HowToReg),
    REKONSILIASI("Rekonsiliasi & Arus Kas", Icons.Default.AccountBalance),
    LAPORAN("Laporan Keuangan", Icons.Default.Assessment),
    ANGGARAN("Anggaran & Realisasi", Icons.Default.PieChart),
    PIUTANG("Kelola Piutang", Icons.Default.CreditCard),
    KALENDER("Kalender Arus Kas", Icons.Default.CalendarMonth),
    KALKULATOR("Kalkulator Kas", Icons.Default.Calculate),
    CATATAN("Buku Catatan", Icons.Default.EditNote),
    AUDIT_LOG("Audit Trail & Sistem", Icons.Default.Policy),
    BACKUP("Cadangan & Pulihkan", Icons.Default.Security),
    ARSIP("Transaksi Dihapus", Icons.Default.Delete),
    AKUN_KATEGORI("Master Akun & Dropdown", Icons.Default.AccountBalance)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val kasViewModel: KasViewModel = viewModel()
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        kasViewModel.snackBarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle back button
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .verticalScroll(rememberScrollState()),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryNavy)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "SISTEM KAS",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Terintegrasi Google Sheets Formula",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                DrawerSectionLabel("OPERASIONAL KAS")
                DrawerItem(Screen.DASHBOARD, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.KAS_MASUK, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.KAS_KELUAR, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.TRANSFER, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.TRANSAKSI_ALL, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DrawerSectionLabel("KARYAWAN & ANALISA")
                DrawerItem(Screen.ABSENSI, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.REKONSILIASI, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.LAPORAN, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.ANGGARAN, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.PIUTANG, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.KALENDER, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.KALKULATOR, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.CATATAN, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DrawerSectionLabel("AUDIT & PENGATURAN")
                DrawerItem(Screen.AUDIT_LOG, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.BACKUP, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.ARSIP, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }
                DrawerItem(Screen.AKUN_KATEGORI, currentScreen) { currentScreen = it; scope.launch { drawerState.close() } }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentScreen.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sistem Kas Terintegrasi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("nav_drawer_toggle")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu Navigasi", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { kasViewModel.simpanDanSyncSekarang() },
                            modifier = Modifier.testTag("appbar_sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Simpan & Sync",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryNavy,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                // Bottom Bar for 4 daily essentials
                val isBottomNavScreen = currentScreen in listOf(
                    Screen.DASHBOARD, Screen.KAS_MASUK, Screen.KAS_KELUAR, Screen.TRANSAKSI_ALL
                )

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.DASHBOARD,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_bottom_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.KAS_MASUK,
                        onClick = { currentScreen = Screen.KAS_MASUK },
                        icon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Kas Masuk", tint = if (currentScreen == Screen.KAS_MASUK) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant) },
                        label = { Text("Masuk", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_bottom_masuk")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.KAS_KELUAR,
                        onClick = { currentScreen = Screen.KAS_KELUAR },
                        icon = { Icon(imageVector = Icons.Default.RemoveCircle, contentDescription = "Kas Keluar", tint = if (currentScreen == Screen.KAS_KELUAR) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant) },
                        label = { Text("Keluar", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_bottom_keluar")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.TRANSAKSI_ALL,
                        onClick = { currentScreen = Screen.TRANSAKSI_ALL },
                        icon = { Icon(imageVector = Icons.Default.ListAlt, contentDescription = "Buku Besar") },
                        label = { Text("Buku Besar", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_bottom_all")
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        viewModel = kasViewModel,
                        onNavigateToMasuk = { currentScreen = Screen.KAS_MASUK },
                        onNavigateToKeluar = { currentScreen = Screen.KAS_KELUAR },
                        onNavigateToTransfer = { currentScreen = Screen.TRANSFER },
                        onNavigateToAll = { currentScreen = Screen.TRANSAKSI_ALL },
                        onNavigateToAbsensi = { currentScreen = Screen.ABSENSI },
                        onNavigateToRekonsiliasi = { currentScreen = Screen.REKONSILIASI },
                        onNavigateToAudit = { currentScreen = Screen.AUDIT_LOG }
                    )
                    Screen.KAS_MASUK -> KasMasukScreen(viewModel = kasViewModel)
                    Screen.KAS_KELUAR -> KasKeluarScreen(viewModel = kasViewModel)
                    Screen.TRANSFER -> TransferScreen(viewModel = kasViewModel)
                    Screen.TRANSAKSI_ALL -> TransaksiAllScreen(viewModel = kasViewModel)
                    Screen.ABSENSI -> AbsensiScreen(viewModel = kasViewModel)
                    Screen.REKONSILIASI -> RekonsiliasiArusKasScreen(viewModel = kasViewModel)
                    Screen.LAPORAN -> LaporanScreen(viewModel = kasViewModel)
                    Screen.ANGGARAN -> AnggaranScreen(viewModel = kasViewModel)
                    Screen.PIUTANG -> PiutangScreen(viewModel = kasViewModel)
                    Screen.KALENDER -> KalenderScreen(viewModel = kasViewModel)
                    Screen.KALKULATOR -> KalkulatorScreen(viewModel = kasViewModel)
                    Screen.CATATAN -> CatatanScreen(viewModel = kasViewModel)
                    Screen.AUDIT_LOG -> AuditLogScreen(viewModel = kasViewModel)
                    Screen.BACKUP -> BackupRestoreScreen(viewModel = kasViewModel)
                    Screen.ARSIP -> ArsipScreen(viewModel = kasViewModel)
                    Screen.AKUN_KATEGORI -> AkunKategoriScreen(viewModel = kasViewModel)
                }
            }
        }
    }
}

@Composable
fun DrawerSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DrawerItem(screen: Screen, currentScreen: Screen, onSelect: (Screen) -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
        label = { Text(text = screen.title, fontSize = 13.sp, fontWeight = if (screen == currentScreen) FontWeight.Bold else FontWeight.Normal) },
        selected = screen == currentScreen,
        onClick = { onSelect(screen) },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag("drawer_item_${screen.name.lowercase()}"),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
            selectedIconColor = PrimaryBlue,
            selectedTextColor = PrimaryBlue
        )
    )
}
