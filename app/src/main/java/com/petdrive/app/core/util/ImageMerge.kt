package com.petdrive.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import java.io.ByteArrayOutputStream

private const val MAX_PAGE_WIDTH = 1600
private const val MAX_MERGED_HEIGHT = 12_000

// Decodes a scanned page, downscaled so its width is at most MAX_PAGE_WIDTH -- keeps up
// to ~10 pages comfortably in memory and the merged upload well under the 20 MB limit.
fun decodePage(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= MAX_PAGE_WIDTH) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
    if (decoded.width <= MAX_PAGE_WIDTH) return decoded
    val scaled = Bitmap.createScaledBitmap(decoded, MAX_PAGE_WIDTH, decoded.height * MAX_PAGE_WIDTH / decoded.width, true)
    if (scaled !== decoded) decoded.recycle()
    return scaled
}

fun rotate90(bitmap: Bitmap): Bitmap {
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Stacks the pages vertically (each scaled to the widest page's width) into one JPEG, so a
// multi-page scan is one document and one AI call. The total height is capped, scaling
// everything down together if a long scan would exceed it.
fun mergePagesToJpeg(pages: List<Bitmap>): ByteArray {
    val width = pages.maxOf { it.width }
    val heights = pages.map { it.height * width / it.width }
    val totalHeight = heights.sum()
    val shrink = if (totalHeight > MAX_MERGED_HEIGHT) MAX_MERGED_HEIGHT.toFloat() / totalHeight else 1f
    val outWidth = (width * shrink).toInt().coerceAtLeast(1)
    val outHeight = (totalHeight * shrink).toInt().coerceAtLeast(1)

    val merged = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(merged)
    canvas.drawColor(android.graphics.Color.WHITE)
    var top = 0f
    pages.forEachIndexed { index, page ->
        val h = heights[index] * shrink
        val dest = android.graphics.RectF(0f, top, outWidth.toFloat(), top + h)
        canvas.drawBitmap(page, null, dest, null)
        top += h
    }
    return ByteArrayOutputStream().use { out ->
        merged.compress(Bitmap.CompressFormat.JPEG, 85, out)
        merged.recycle()
        out.toByteArray()
    }
}
