package com.app.promptai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

fun createFileProviderTempUri(context: Context): Uri? {
    val tempImagesDir = File(context.filesDir, "temp_images_internal") // Временная папка
    tempImagesDir.mkdirs()

    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val tempImageFile = File(tempImagesDir, "TEMP_JPEG_${timeStamp}.jpg") // Временный файл

    val authority = "${context.packageName}.provider"
    return try {
        FileProvider.getUriForFile(
            context,
            authority,
            tempImageFile
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

fun saveFileToInternalStorage(context: Context, uri: Uri): Uri? { // Изменено название для ясности
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {

            val mimeType: String? = context.contentResolver.getType(uri)
            val detectedExtension: String? = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)

            // 1. Правильное получение имени файла из Uri (для content://)
            // Попытаемся получить имя файла из _display_name
            val originalFileName = getFileNameFromUri(context, uri)

            // 2. Формируем безопасное и уникальное имя файла
            // Удаляем потенциальное расширение из оригинального имени, чтобы не дублировать
            val baseFileName = originalFileName?.substringBeforeLast('.') ?: "file"
            val uniqueId = UUID.randomUUID().toString()

            val finalFileName = if (detectedExtension != null) {
                "${baseFileName}_${uniqueId}.$detectedExtension"
            } else {
                // Если расширение не определено, добавляем UUID без расширения
                "${baseFileName}_${uniqueId}"
            }


            val destinationFile = File(context.filesDir, finalFileName) // Получаем путь к файлу во внутреннем хранилище

            // 3. Используем 'use' для обоих потоков для безопасного закрытия
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.use { input -> // Закрываем inputStream автоматически
                    input.copyTo(outputStream)
                }
            }

            Uri.fromFile(destinationFile) // Возвращаем URI сохраненного файла
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getMimeTypeFromFileUri(uri: Uri): String? {
    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    return if (extension != null) {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    } else {
        null
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

fun deleteTempFile(context: Context, uri: Uri) {
    try {
        // Предпочтительный способ удаления по URI - через ContentResolver
        val deletedRows = context.contentResolver.delete(uri, null, null)
        if (deletedRows > 0) {
            // Логирование: "Временный файл удален через ContentResolver: $uri"
        } else {
            val file = uriToFileHelper(context, uri)
            if (file?.exists() == true) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace() // Вывод стека ошибок в лог
    }
}

fun uriToFileHelper(context: Context, uri: Uri): File? {
    // Проверяем, что это URI нашего FileProvider
    val authority = "${context.packageName}.provider"
    if (uri.authority != authority) {
        // Это не наш FileProvider URI, мы не можем его преобразовать таким способом
        return null
    }

    try {
        // Парсим путь из URI FileProvider
        // Пример URI: content://your.app.id.provider/my_images/TEMP_JPEG_...jpg
        val path = uri.path ?: return null
        // Имя пути из file_paths.xml: "my_images" -> соответствует <external-files-path name="my_images" ...>
        // Удаляем имя пути из начала URI path, чтобы получить относительный путь внутри этой директории
        val pathInXml = "my_images/" // Должно совпадать с атрибутом 'name' в file_paths.xml + "/"
        if (!path.startsWith("/$pathInXml")) {
            // Путь не соответствует ожидаемой структуре из file_paths.xml
            return null
        }
        val relativePath = path.substringAfter("/$pathInXml") // Получаем "TEMP_JPEG_...jpg"

        // Получаем базовую директорию, соответствующую <external-files-path name="my_images" path="Pictures/YourAppImages/"/>
        val baseExternalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Собираем полный путь к файлу: baseExternalFilesDir + path_in_xml + relativePath
        // path_in_xml из file_paths.xml атрибут 'path': "Pictures/YourAppImages/"
        val fullFilePath = File(baseExternalFilesDir, "Pictures/YourAppImages/$relativePath") // Собрали полный путь

        // Убеждаемся, что файл существует по собранному пути
        if (!fullFilePath.exists() || !fullFilePath.isFile) {
            // Файл не найден или это не файл по собранному пути
            return null
        }

        return fullFilePath // Возвращаем объект File

    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}