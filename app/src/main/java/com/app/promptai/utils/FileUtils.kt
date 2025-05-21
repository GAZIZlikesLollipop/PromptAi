package com.app.promptai.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.ImageDecoder

fun createFileProviderTempUri(context: Context): Uri? {
    val externalFilesDir = context.filesDir
    val tempImagesDir = File(externalFilesDir, "temp_images_internal") // Временная папка
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

fun deleteTempFile(context: Context, uri: Uri) {
    try {
        // Предпочтительный способ удаления по URI - через ContentResolver
        val deletedRows = context.contentResolver.delete(uri, null, null)
        if (deletedRows > 0) {
            // Логирование: "Временный файл удален через ContentResolver: $uri"
        } else {
            // Логирование: "Не удалось удалить временный файл через ContentResolver: $uri"
            // Если ContentResolver не сработал (например, URI не полностью поддерживается delete),
            // можно попробовать удалить как File, если известен путь через uriToFileHelper.
            // Это менее универсально для всех URI, но может сработать для FileProvider URIs, которые мы сами создали.
            val file = uriToFileHelper(context, uri)
            if (file?.exists() == true) {
                file.delete()
                // Логирование: "Временный файл удален через File.delete(): ${file.absolutePath}"
            } else {
                // Логирование: "Не удалось удалить временный файл ни одним способом: $uri"
            }
        }
    } catch (e: Exception) {
        // Логирование ошибки: "Ошибка при удалении временного файла $uri", e
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