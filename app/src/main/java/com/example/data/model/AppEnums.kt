package com.example.data.model

enum class TransactionType(val label: String, val prefix: String) {
    MASUK("Kas Masuk", "KM"),
    KELUAR("Kas Keluar", "KK"),
    TRANSFER("Transfer", "TR")
}

enum class TransactionStatus(val label: String) {
    SELESAI("Selesai"),
    DRAFT("Draft"),
    PENDING("Pending"),
    BATAL("Batal"),
    DIHAPUS("Dihapus")
}

enum class NotePriority(val label: String) {
    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi")
}

enum class NoteStatus(val label: String) {
    OPEN("Open"),
    DONE("Done"),
    FOLLOW_UP("Follow Up")
}

enum class ReceivableStatus(val label: String) {
    BELUM_JATUH_TEMPO("Belum Jatuh Tempo"),
    JATUH_TEMPO("Jatuh Tempo"),
    LUNAS("Lunas")
}
