package com.jamakharch.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object MappingBackup {

    const val FILE_NAME = "merchant_mappings.json"
    private const val FORMAT_VERSION = 1

    fun autoFile(context: Context): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    suspend fun writeAuto(context: Context) {
        try {
            autoFile(context).writeText(buildJson(context))
        } catch (_: Exception) {
        }
    }

    suspend fun exportTo(context: Context, uri: Uri): Int {
        val json = buildJson(context)
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Could not open destination for writing")
        return countMappings(context)
    }

    suspend fun importFrom(context: Context, uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not open source for reading")

        val root = JSONObject(text)
        val arr = root.optJSONArray("mappings") ?: return 0

        val db = AppDatabase.getInstance(context)
        val overrideDao = db.merchantOverrideDao()
        val expenseDao = db.expenseDao()

        var count = 0
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val merchant = obj.optString("merchant").lowercase().trim()
            val category = obj.optString("category").trim()
            if (merchant.isBlank() || category.isBlank()) continue
            overrideDao.setOverride(
                MerchantOverrideEntity(merchant = merchant, category = category)
            )
            expenseDao.updateCategoryByMerchant(merchant, category)
            count++
        }
        writeAuto(context)
        return count
    }

    private suspend fun buildJson(context: Context): String {
        val list = AppDatabase.getInstance(context)
            .merchantOverrideDao()
            .getAllOverrides()
            .first()

        val arr = JSONArray()
        list.forEach { o ->
            val obj = JSONObject()
            obj.put("merchant", o.merchant)
            obj.put("category", o.category)
            arr.put(obj)
        }
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("mappings", arr)
        return root.toString(2)
    }

    private suspend fun countMappings(context: Context): Int {
        return AppDatabase.getInstance(context)
            .merchantOverrideDao()
            .getAllOverrides()
            .first()
            .size
    }
}
