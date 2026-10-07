package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.PendingPurple
import com.example.ui.theme.PendingPurpleLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TransferAmber
import com.example.ui.theme.TransferAmberLight
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

fun formatRupiah(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0", symbols)
    val formattedNumber = formatter.format(Math.abs(amount))
    return if (amount < 0) "-Rp $formattedNumber" else "Rp $formattedNumber"
}

fun formatDateIndo(dateString: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = parser.parse(dateString) ?: return dateString
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        formatter.format(date)
    } catch (e: Exception) {
        dateString
    }
}

fun getStatusColors(status: String): Pair<Color, Color> {
    return when (status.lowercase(Locale.ROOT)) {
        "selesai" -> Pair(IncomeGreen, IncomeGreenLight)
        "draft" -> Pair(Color(0xFF64748B), Color(0xFFF1F5F9))
        "pending" -> Pair(PendingPurple, PendingPurpleLight)
        "batal", "dihapus" -> Pair(ExpenseRed, ExpenseRedLight)
        else -> Pair(PrimaryBlue, Color(0xFFE0F2FE))
    }
}

fun getTypeColors(type: String): Pair<Color, Color> {
    return when (type.uppercase(Locale.ROOT)) {
        "MASUK" -> Pair(IncomeGreen, IncomeGreenLight)
        "KELUAR" -> Pair(ExpenseRed, ExpenseRedLight)
        "TRANSFER" -> Pair(TransferAmber, TransferAmberLight)
        else -> Pair(PrimaryBlue, Color(0xFFE0F2FE))
    }
}
