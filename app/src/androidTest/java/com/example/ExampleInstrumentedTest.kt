package com.example

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }

    @Test
    fun navigationOpensReportsAndPeriodDateControls() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithText("Laporan Keuangan").performClick()
        composeRule.onNodeWithTag("laporan_screen").assertExists()
        composeRule.onNodeWithText("Tanggal Mulai").assertExists()
        composeRule.onNodeWithText("Tanggal Akhir").assertExists()
    }

    @Test
    fun attendanceFormOpensWithDatePickerField() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithText("Absensi Karyawan").performClick()
        composeRule.onNodeWithTag("absensi_screen").assertExists()
        composeRule.onNodeWithTag("fab_record_attendance").performClick()
        composeRule.onNodeWithTag("attendance_date_input").assertExists()
    }

    @Test
    fun backupScreenExposesRealFileImportAndExportControls() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithText("Cadangan & Pulihkan").performClick()
        composeRule.onNodeWithTag("backup_restore_screen").assertExists()
        composeRule.onNodeWithText("Simpan Backup JSON").assertExists()
        composeRule.onNodeWithText("Pulihkan dari File Backup").assertExists()
        composeRule.onNodeWithTag("spreadsheet_export").assertExists()
        composeRule.onNodeWithTag("spreadsheet_import").assertExists()
    }
}
