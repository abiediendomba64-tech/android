package com.example

import androidx.compose.ui.test.assertIsDisplayed
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
        composeRule.onNodeWithTag("laporan_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Tanggal Mulai").assertIsDisplayed()
        composeRule.onNodeWithText("Tanggal Akhir").assertIsDisplayed()
    }

    @Test
    fun attendanceFormOpensWithDatePickerField() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithText("Absensi Karyawan").performClick()
        composeRule.onNodeWithTag("absensi_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_record_attendance").performClick()
        composeRule.onNodeWithTag("attendance_date_input").assertIsDisplayed()
    }

    @Test
    fun backupScreenExposesRealFileImportAndExportControls() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithText("Cadangan & Pulihkan").performClick()
        composeRule.onNodeWithTag("backup_restore_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan Backup JSON").assertIsDisplayed()
        composeRule.onNodeWithText("Pulihkan dari File Backup").assertIsDisplayed()
        composeRule.onNodeWithTag("spreadsheet_export").assertIsDisplayed()
        composeRule.onNodeWithTag("spreadsheet_import").assertIsDisplayed()
    }
}
