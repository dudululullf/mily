package com.audioplayer.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.text.Collator
import java.util.*

object FileUtils {
    private val collator = Collator.getInstance(Locale.CHINA)
    
    fun isMp3File(file: File): Boolean {
        return file.isFile && file.extension.equals("mp3", ignoreCase = true)
    }

    fun getFolderName(folderPath: String): String {
        return File(folderPath).name
    }

    fun getMp3FilesInFolder(folderPath: String): List<File> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            return emptyList()
        }
        
        return folder.listFiles()?.filter { isMp3File(it) }?.sortedWith(naturalOrderComparator) ?: emptyList()
    }

    private val naturalOrderComparator = Comparator<File> {
        file1, file2 ->
        naturalCompare(file1.name, file2.name)
    }

    private fun naturalCompare(str1: String, str2: String): Int {
        val regex = "\\d+".toRegex()
        val parts1 = regex.split(str1)
        val parts2 = regex.split(str2)
        val numbers1 = regex.findAll(str1).map { it.value.toLong() }.toList()
        val numbers2 = regex.findAll(str2).map { it.value.toLong() }.toList()

        for (i in 0 until minOf(parts1.size, parts2.size)) {
            val partCompare = collator.compare(parts1[i], parts2[i])
            if (partCompare != 0) {
                return partCompare
            }
            if (i < numbers1.size && i < numbers2.size) {
                val numCompare = numbers1[i].compareTo(numbers2[i])
                if (numCompare != 0) {
                    return numCompare
                }
            }
        }
        return str1.length.compareTo(str2.length)
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun getDataColumn(context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?): String? {
        val column = MediaStore.Files.FileColumns.DATA
        val projection = arrayOf(column)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use {
            cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }
}
