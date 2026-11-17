package com.massbim.pdf.data

data class RecentPdf(
    val uri: String,
    val title: String,
    val lastPage: Int = 0,
    val pinned: Boolean = false,
)
