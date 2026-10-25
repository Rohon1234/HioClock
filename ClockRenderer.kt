package com.hioclock

import android.content.Context
import android.graphics.*
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object ClockRenderer {
    private const val S = 320
    private const val ORANGE = 0xFFFF9500.toInt()
    private const val INSET = 14f
    // Corner radius as a fraction of widget size, matched to the HiOS weather widget.
    private const val CORNER = 0.155f

    fun render(ctx: Context, digital: Boolean, dark: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val now = Calendar.getInstance()
        val fg = if (dark) Color.WHITE else 0xFF1C1C1E.toInt()
        drawBackground(c, dark)
        if (digital) drawDigital(ctx, c, now, fg) else drawAnalog(c, now, fg)
        return bmp
    }

    private fun withAlpha(color: Int, a: Float): Int =
        Color.argb((a * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private fun drawBackground(c: Canvas, dark: Boolean) {
        val rect = RectF(2f, 2f, S - 2f, S - 2f)
        val r = S * CORNER
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dark) 0xB3000000.toInt() else 0x99FFFFFF.toInt()
        }
        c.drawRoundRect(rect, r, r, fill)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = if (dark) 0x40FFFFFF else 0x99FFFFFF.toInt()
        }
        c.drawRoundRect(rect, r, r, stroke)
    }

    // radius of a rounded-square (superellipse) along a direction theta (clockwise from top)
    private fun edgeRadius(theta: Double): Float {
        val R = S / 2f - INSET
        val cc = abs(cos(theta)).pow(4.0)
        val ss = abs(sin(theta)).pow(4.0)
        return (R / (cc + ss).pow(0.25)).toFloat()
    }

    private fun drawTick(c: Canvas, p: Paint, theta: Double, len: Float) {
        val cx = S / 2f
        val cy = S / 2f
        val r = edgeRadius(theta)
        val dx = sin(theta).toFloat()
        val dy = -cos(theta).toFloat()
        c.drawLine(cx + dx * r, cy + dy * r, cx + dx * (r - len), cy + dy * (r - len), p)
    }


    private const val TICKS = 60

    private fun cornerR() = S * CORNER - INSET
    private fun halfStraight() = (S / 2f - INSET) - cornerR()
    private fun perimeterLength(): Float = 8f * halfStraight() + 2f * Math.PI.toFloat() * cornerR()

    // Point on the rounded-square perimeter, `dist` from top-centre going clockwise.
    // Returns x, y and the inward unit normal (nx, ny).
    private fun perimeterPoint(dist: Float): FloatArray {
        val h = S / 2f - INSET
        val rc = cornerR()
        val a = h - rc
        val arc = (Math.PI / 2).toFloat() * rc
        val segs = floatArrayOf(a, arc, 2 * a, arc, 2 * a, arc, 2 * a, arc, a)
        var d = dist % perimeterLength()
        var k = 0
        while (k < segs.size - 1 && d > segs[k]) { d -= segs[k]; k++ }
        val phi = d / rc
        val sn = sin(phi.toDouble()).toFloat()
        val cs = cos(phi.toDouble()).toFloat()
        val r: FloatArray = when (k) {
            0 -> floatArrayOf(d, -h, 0f, 1f)
            1 -> floatArrayOf(a + rc * sn, -a - rc * cs, -sn, cs)
            2 -> floatArrayOf(h, -a + d, -1f, 0f)
            3 -> floatArrayOf(a + rc * cs, a + rc * sn, -cs, -sn)
            4 -> floatArrayOf(a - d, h, 0f, -1f)
            5 -> floatArrayOf(-a - rc * sn, a + rc * cs, sn, -cs)
            6 -> floatArrayOf(-h, a - d, 1f, 0f)
            7 -> floatArrayOf(-a - rc * cs, -a - rc * sn, cs, sn)
            else -> floatArrayOf(-a + d, -h, 0f, 1f)
        }
        return floatArrayOf(S / 2f + r[0], S / 2f + r[1], r[2], r[3])
    }

    private fun drawEdgeTick(c: Canvas, p: Paint, pt: FloatArray, len: Float) {
        c.drawLine(pt[0], pt[1], pt[0] + pt[2] * len, pt[1] + pt[3] * len, p)
    }

    private fun drawDigital(ctx: Context, c: Canvas, now: Calendar, fg: Int) {
        val sec = now.get(Calendar.SECOND)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        val n = TICKS
        val hi = Math.round(sec * n / 60.0).toInt() % n
        val trail = n / 3
        val total = perimeterLength()
        for (i in 0 until n) {
            val d = (hi - i + n) % n
            val pt = perimeterPoint(i * total / n)
            val alpha = if (d == 0) 1f else if (d < trail) (1f - (d / trail.toFloat()) * 0.75f) * 0.9f else 0.25f * 0.9f
            p.color = withAlpha(fg, alpha)
            p.strokeWidth = 4f
            drawEdgeTick(c, p, pt, 16f)
        }

        val cx = S / 2f
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(fg, 0.7f)
            textSize = S * 0.072f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val date = SimpleDateFormat("EEE, MMM yy", Locale.getDefault()).format(now.time)
        c.drawText(date, cx, S * 0.27f, datePaint)

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fg
            textSize = S * 0.34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        val pattern = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
        val time = SimpleDateFormat(pattern, Locale.getDefault()).format(now.time)
        val y = S / 2f - (timePaint.ascent() + timePaint.descent()) / 2f
        c.drawText(time, cx, y, timePaint)

        val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(fg, 0.7f)
            textSize = S * 0.09f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        c.drawText(zoneOffsetText(now), cx, S * 0.77f, offPaint)
    }

    // Hour difference between ZONE_ID and the phone's own time zone, like the iOS Clock widget.
    private const val ZONE_ID = "Asia/Dhaka"

    private fun zoneOffsetText(now: Calendar): String {
        val t = now.timeInMillis
        val diffMin = (TimeZone.getTimeZone(ZONE_ID).getOffset(t) - TimeZone.getDefault().getOffset(t)) / 60000
        if (diffMin == 0) return "0"
        val sign = if (diffMin > 0) "+" else "-"
        val a = abs(diffMin)
        return if (a % 60 == 0) "$sign${a / 60}" else "$sign${a / 60}:${(a % 60).toString().padStart(2, '0')}"
    }

    private fun drawAnalog(c: Canvas, now: Calendar, fg: Int) {
        val cx = S / 2f
        val cy = S / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

        for (i in 0 until 60) {
            val theta = Math.toRadians(i * 6.0)
            if (i % 5 == 0) {
                if (i % 15 == 0) continue // 12,3,6,9 have numbers
                p.color = fg
                p.strokeWidth = 6f
                drawTick(c, p, theta, 28f)
            } else {
                p.color = withAlpha(fg, 0.55f)
                p.strokeWidth = 2f
                drawTick(c, p, theta, 9f)
            }
        }

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fg
            textSize = S * 0.14f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        val off = S * 0.26f
        val dy = -(tp.ascent() + tp.descent()) / 2f
        c.drawText("12", cx, cy - off + dy, tp)
        c.drawText("3", cx + off, cy + dy, tp)
        c.drawText("6", cx, cy + off + dy, tp)
        c.drawText("9", cx - off, cy + dy, tp)

        val h = now.get(Calendar.HOUR)
        val m = now.get(Calendar.MINUTE)
        val s = now.get(Calendar.SECOND)
        val hourAngle = (h + m / 60f) * 30f
        val minAngle = (m + s / 60f) * 6f
        val secAngle = s * 6f

        fun hand(angle: Float, len: Float, tail: Float, width: Float, color: Int) {
            p.color = color
            p.strokeWidth = width
            c.save()
            c.rotate(angle, cx, cy)
            c.drawLine(cx, cy + tail, cx, cy - len, p)
            c.restore()
        }
        hand(hourAngle, S * 0.24f, S * 0.05f, 9f, fg)
        hand(minAngle, S * 0.37f, S * 0.04f, 7f, fg)
        hand(secAngle, S * 0.40f, S * 0.08f, 3f, ORANGE)

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = ORANGE
        }
        c.drawCircle(cx, cy, 7f, ring)
    }
}
