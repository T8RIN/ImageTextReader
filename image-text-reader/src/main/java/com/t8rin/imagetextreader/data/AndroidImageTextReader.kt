/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2024 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetextreader.data

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import com.t8rin.imagetextreader.R
import com.t8rin.imagetextreader.domain.Constants
import com.t8rin.imagetextreader.domain.DownloadData
import com.t8rin.imagetextreader.domain.ImageTextReader
import com.t8rin.imagetextreader.domain.OCRLanguage
import com.t8rin.imagetextreader.domain.OcrEngineMode
import com.t8rin.imagetextreader.domain.RecognitionData
import com.t8rin.imagetextreader.domain.RecognitionType
import com.t8rin.imagetextreader.domain.SegmentationMode
import com.t8rin.imagetextreader.domain.TextRecognitionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.String.format
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject

internal class AndroidImageTextReader @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageTextReader<Bitmap> {

    private val defaultDispatcher = Dispatchers.Default
    private val ioDispatcher = Dispatchers.IO

    init {
        RecognitionType.entries.forEach {
            File(context.filesDir, "${it.displayName}/tessdata").mkdirs()
        }
    }

    override suspend fun getTextFromImage(
        type: RecognitionType,
        languageCode: String,
        segmentationMode: SegmentationMode,
        ocrEngineMode: OcrEngineMode,
        image: Bitmap?,
        onProgress: (Int) -> Unit
    ): TextRecognitionResult = withContext(defaultDispatcher) {

        if (image == null) return@withContext TextRecognitionResult.Success(RecognitionData("", 0))

        val needToDownload = getNeedToDownloadLanguages(type, languageCode)

        if (needToDownload.isNotEmpty()) {
            return@withContext TextRecognitionResult.NoData(needToDownload)
        }

        val path = getPathFromMode(type)

        return@withContext runCatching {
            val api = TessBaseAPI {
                if (isActive) onProgress(it.percent)
                else return@TessBaseAPI
            }.apply {
                val success = init(path, languageCode, ocrEngineMode.ordinal)
                if (!success) {
                    return@withContext TextRecognitionResult.NoData(
                        getNeedToDownloadLanguages(
                            type = type,
                            languageCode = languageCode
                        )
                    ).also {
                        it.data.forEach { data ->
                            File(
                                "${getPathFromMode(type)}/tessdata",
                                format(Constants.LANGUAGE_CODE, data.languageCode)
                            ).delete()
                        }
                    }
                }
                pageSegMode = segmentationMode.ordinal
                setImage(image.copy(Bitmap.Config.ARGB_8888, false))
            }

            api.getHOCRText(0)

            val text = api.utF8Text

            val accuracy = api.meanConfidence()

            TextRecognitionResult.Success(
                RecognitionData(
                    text = text,
                    accuracy = if (text.isEmpty()) 0 else accuracy
                )
            )
        }.let {
            if (it.isSuccess) {
                it.getOrNull()!!
            } else {
                languageCode.split("+").forEach { code ->
                    File(
                        path,
                        format(Constants.LANGUAGE_CODE, code)
                    ).delete()
                }

                TextRecognitionResult.Error(it.exceptionOrNull()!!)
            }
        }
    }

    private fun getNeedToDownloadLanguages(
        type: RecognitionType,
        languageCode: String
    ): List<DownloadData> {
        val needToDownload = mutableListOf<DownloadData>()
        languageCode.split("+").forEach { code ->
            if (!isLanguageDataExists(type, code)) {
                needToDownload.add(
                    DownloadData(
                        type = type,
                        languageCode = code,
                        name = getDisplayName(code, false),
                        localizedName = getDisplayName(code, true)
                    )
                )
            }
        }
        return needToDownload
    }

    override fun isLanguageDataExists(
        type: RecognitionType,
        languageCode: String
    ): Boolean = File(
        "${getPathFromMode(type)}/tessdata",
        format(Constants.LANGUAGE_CODE, languageCode)
    ).exists()

    override suspend fun getLanguages(
        type: RecognitionType
    ): List<OCRLanguage> = withContext(ioDispatcher) {

        val codes = context.resources.getStringArray(R.array.key_ocr_engine_language_value)

        return@withContext codes.mapNotNull { code ->
            val name = getDisplayName(code, false)
            val localizedName = getDisplayName(code, true)
            if (name.isEmpty() || localizedName.isEmpty()) return@mapNotNull null

            OCRLanguage(
                name = name,
                code = code,
                downloaded = RecognitionType.entries.filter {
                    isLanguageDataExists(it, code)
                },
                localizedName = localizedName
            )
        }.toList()
    }

    override fun getLanguageForCode(
        code: String
    ): OCRLanguage = OCRLanguage(
        name = getDisplayName(code, false),
        code = code,
        downloaded = RecognitionType.entries.filter {
            isLanguageDataExists(it, code)
        },
        localizedName = getDisplayName(code, true)
    )

    override suspend fun deleteLanguage(
        language: OCRLanguage,
        types: List<RecognitionType>
    ) = withContext(ioDispatcher) {
        types.forEach { type ->
            File(
                "${getPathFromMode(type)}/tessdata",
                format(Constants.LANGUAGE_CODE, language.code)
            ).delete()
        }
    }

    override suspend fun downloadTrainingData(
        type: RecognitionType,
        languageCode: String,
        onProgress: (Float, Long) -> Unit
    ): Boolean {
        val needToDownloadLanguages = getNeedToDownloadLanguages(type, languageCode)

        return if (needToDownloadLanguages.isNotEmpty()) {
            downloadTrainingDataImpl(type, needToDownloadLanguages, onProgress)
        } else false
    }

    private suspend fun downloadTrainingDataImpl(
        type: RecognitionType,
        needToDownloadLanguages: List<DownloadData>,
        onProgress: (Float, Long) -> Unit
    ): Boolean = needToDownloadLanguages.map {
        downloadTrainingDataForCode(type, it.languageCode, onProgress)
    }.all { it }

    private suspend fun downloadTrainingDataForCode(
        type: RecognitionType,
        lang: String,
        onProgress: (Float, Long) -> Unit
    ): Boolean = withContext(defaultDispatcher) {
        var location: String
        var downloadURL = when (type) {
            RecognitionType.Best -> format(Constants.TESSERACT_DATA_DOWNLOAD_URL_BEST, lang)
            RecognitionType.Standard -> format(Constants.TESSERACT_DATA_DOWNLOAD_URL_STANDARD, lang)
            RecognitionType.Fast -> format(Constants.TESSERACT_DATA_DOWNLOAD_URL_FAST, lang)
        }
        var url: URL
        var base: URL
        var next: URL
        var conn: HttpURLConnection
        return@withContext runCatching {
            while (true) {
                url = URL(downloadURL)
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false

                when (conn.responseCode) {
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP -> {
                        location = conn.getHeaderField("Location")
                        base = URL(downloadURL)
                        next = URL(base, location)
                        downloadURL = next.toExternalForm()
                        continue
                    }
                }
                break
            }
            conn.connect()

            val totalContentSize = conn.contentLength.toLong()
            onProgress(0f, totalContentSize)

            val input: InputStream = BufferedInputStream(url.openStream())
            val output: OutputStream = FileOutputStream(
                File(
                    "${getPathFromMode(type)}/tessdata",
                    format(Constants.LANGUAGE_CODE, lang)
                ).apply {
                    createNewFile()
                }
            )
            val data = ByteArray(1024 * 8)
            var count: Int
            var downloaded = 0
            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
                downloaded += count
                val percentage = downloaded * 100f / totalContentSize
                onProgress(percentage, totalContentSize)
            }

            output.flush()
            output.close()
            input.close()
        }.isSuccess
    }

    private fun getPathFromMode(
        type: RecognitionType
    ): String = File(context.filesDir, type.displayName).absolutePath

    private fun getDisplayName(
        lang: String?,
        useDefaultLocale: Boolean
    ): String {
        if (lang.isNullOrEmpty()) {
            return ""
        }

        val locale = Locale.forLanguageTag(
            if (lang.contains("chi_sim")) "zh-CN"
            else if (lang.contains("chi_tra")) "zh-TW"
            else lang
        )
        return if (useDefaultLocale) {
            locale.getDisplayName(Locale.getDefault()).replaceFirstChar { it.uppercase(locale) }
        } else locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }
}