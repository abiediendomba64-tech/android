package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
    fun appContextUsesExpectedPackageName() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aistudio.sistemkas.trkxvd", appContext.packageName)
    }

    @Test
    fun navigationOpensReportsAndPeriodDateControls() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithTag("drawer_item_laporan").performScrollTo().performClick()
        composeRule.onNodeWithTag("laporan_screen")
            .performScrollToNode(hasText("Tanggal Mulai"))
        composeRule.onNodeWithText("Tanggal Mulai").assertIsDisplayed()
        composeRule.onNodeWithText("Tanggal Akhir").assertIsDisplayed()
    }

    @Test
    fun attendanceFormOpensWithDatePickerField() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithTag("drawer_item_absensi").performScrollTo().performClick()
        composeRule.onNodeWithTag("absensi_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("fab_record_attendance").performClick()
        composeRule.onNodeWithTag("attendance_date_input").assertIsDisplayed()
    }

    @Test
    fun backupScreenExposesRealFileImportAndExportControls() {
        composeRule.onNodeWithTag("nav_drawer_toggle").performClick()
        composeRule.onNodeWithTag("drawer_item_backup").performScrollTo().performClick()
        composeRule.onNodeWithTag("backup_restore_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_restore_screen")
            .performScrollToNode(hasText("Simpan Backup JSON"))
        composeRule.onNodeWithText("Simpan Backup JSON").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_restore_screen")
            .performScrollToNode(hasText("Pulihkan dari File Backup"))
        composeRule.onNodeWithText("Pulihkan dari File Backup").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_restore_screen")
            .performScrollToNode(hasTestTag("spreadsheet_export"))
        composeRule.onNodeWithTag("spreadsheet_export").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_restore_screen")
            .performScrollToNode(hasTestTag("spreadsheet_import"))
        composeRule.onNodeWithTag("spreadsheet_import").assertIsDisplayed()
    }
}
