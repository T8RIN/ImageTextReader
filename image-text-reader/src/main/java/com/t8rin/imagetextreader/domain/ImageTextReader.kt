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

package com.t8rin.imagetextreader.domain

interface ImageTextReader<Image> {

    suspend fun getTextFromImage(
        type: RecognitionType,
        languageCode: String,
        segmentationMode: SegmentationMode = SegmentationMode.PSM_AUTO_OSD,
        ocrEngineMode: OcrEngineMode = OcrEngineMode.DEFAULT,
        image: Image?,
        onProgress: (Int) -> Unit
    ): TextRecognitionResult

    suspend fun downloadTrainingData(
        type: RecognitionType,
        languageCode: String,
        onProgress: (Float, Long) -> Unit
    ): Boolean

    fun isLanguageDataExists(
        type: RecognitionType,
        languageCode: String
    ): Boolean

    suspend fun getLanguages(
        type: RecognitionType
    ): List<OCRLanguage>

    fun getLanguageForCode(
        code: String
    ): OCRLanguage

    suspend fun deleteLanguage(
        language: OCRLanguage,
        types: List<RecognitionType>
    )

}