package com.hioclock

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "HiOS Clock\n\nHome screen এ long-press করে Widgets থেকে চারটা clock widget যোগ করো:\nAnalog Black, Analog White, Digital Black, Digital White"
            textSize = 16f
        })
        root.addView(Button(this).apply {
            text = "Start clock service"
            setOnClickListener { ClockService.start(this@MainActivity) }
        })
        setContentView(root)
        ClockService.start(this)
    }
}
