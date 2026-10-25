package com.hioclock

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

object ClockUpdater {
    fun update(ctx: Context, mgr: AppWidgetManager, ids: IntArray, digital: Boolean, dark: Boolean) {
        if (ids.isEmpty()) return
        val bmp = ClockRenderer.render(ctx, digital, dark)
        val rv = RemoteViews(ctx.packageName, R.layout.widget_image)
        rv.setImageViewBitmap(R.id.img, bmp)
        mgr.updateAppWidget(ids, rv)
    }
}

abstract class BaseClockWidget(private val digital: Boolean, private val dark: Boolean) : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ClockUpdater.update(context, mgr, ids, digital, dark)
        ClockService.start(context)
    }

    override fun onEnabled(context: Context) {
        ClockService.start(context)
    }
}

class AnalogBlackWidget : BaseClockWidget(digital = false, dark = true)
class AnalogWhiteWidget : BaseClockWidget(digital = false, dark = false)
class DigitalBlackWidget : BaseClockWidget(digital = true, dark = true)
class DigitalWhiteWidget : BaseClockWidget(digital = true, dark = false)
