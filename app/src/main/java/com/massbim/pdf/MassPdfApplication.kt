package com.massbim.pdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MassPdfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
