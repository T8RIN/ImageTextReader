<h1 align="center">

ImageTextReader

</h1>


<p align="center">
  <img alt="API" src="https://img.shields.io/badge/Api%2021+-50f270?logo=android&logoColor=black&style=for-the-badge"/></a>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-a503fc?logo=kotlin&logoColor=white&style=for-the-badge"/></a>
  <a href="https://hits.sh/github.com/t8rin/imagetextreader/"><img alt="Hits" src="https://hits.sh/github.com/t8rin/imagetextreader.svg?style=for-the-badge&label=Views&extraCount=10&color=54856b"/></a>
  <img src="https://img.shields.io/github/v/release/t8rin/imagetextreader?style=for-the-badge"/>
</p>

<div align="center">
            
📸 ImageTextReader is a Library for performing OCR in fast and convenient way

</div>

## Features

- 120+ languages
- 3 Type of data: Fast, Standard, Best
- Segmentation Mode Selection
- Multiple languages at the same time

## Usage

### 1. Add dependencies

#### Kotlin (kts)
```kotlin
repositories {
  maven { setUrl("https://jitpack.io") } // Add jitpack
}
dependencies {
  implementation("com.github.T8RIN:ImageTextReader:LATEST_VERSION") // Replace "LATEST_VERSION" with preferrend version tag
}
```

#### Groovy
```groovy
repositories {
  maven { url 'https://jitpack.io' } // Add jitpack
}
dependencies {
  implementation 'com.github.T8RIN:ImageTextReader:LATEST_VERSION' // Replace "LATEST_VERSION" with preferrend version tag
}
```

### 2. Get `ImageTextReader` instance

```kotlin
// Use injection through dagger
@Inject
lateinit var imageTextReader: ImageTextReader<Bitmap>

// Or inject in ViewModel
@HiltViewModel
class ExampleViewModel @Inject constructor(
  private val imageTextReader: ImageTextReader<Bitmap>
): ViewModel()

// Or obtain new instance passing application context
val imageTextReader: ImageTextReader<Bitmap> = ImageTextReader(context)

/* When you have ImageTextReader instance use it as shown below */

// First get available languages list
val recognitionType: RecognitionType = RecognitionType.Standard // Also available Best and Fast models
val languages: List<OCRLanguage> = imageTextReader.getLanguages(recognitionType)

// Or get OCRLanguage by code, for example `en`
val language: OCRLanguage = imageTextReader.getLanguageForCode("en")

// Select needed languages
val languageCode = selectedLanguages.joinToString("+") { it.code } // selectedLanguages is your needed OCRLanguage instances

// Or with single language
val languageCode = language.code

// Set some parameters
val segmentationMode: SegmentationMode = SegmentationMode.PSM_AUTO_OSD
val ocrEngineMode: OcrEngineMode = OcrEngineMode.DEFAULT

imageTextReader.getTextFromImage(
    type = recognitionType,
    languageCode = languageCode,
    segmentationMode = segmentationMode,
    image = bitmap,
    ocrEngineMode = ocrEngineMode,
    onProgress = { progress ->
       // Get recognition progress in percents
    }
).also { result ->
    when (result) {
        is TextRecognitionResult.Error -> {
            val throeable: Throwable = result.throwable
        }

        is TextRecognitionResult.NoData -> {
            val downloadData: List<DownloadData> = result.data
        }

        is TextRecognitionResult.Success -> {
            val text: String = result.data
        }
    }
}

// When you have downloadData you can download them as shown below
val isDownloadSuccessfully: Boolean = imageTextReader.downloadTrainingData(
    type = recognitionType,
    languageCode = downloadData.joinToString(separator = "+") { it.languageCode },
    onProgress = { percentage, totalContentSize ->
        // Get current download progress and total size of model
    }
)

// Also you can check if some language model exists for selected recognition type
fun isLanguageDataExists(
    type: RecognitionType,
    languageCode: String
): Boolean

// Or delete model that stored in memory
suspend fun deleteLanguage(
    language: OCRLanguage,
    types: List<RecognitionType>
)

```

## Find this repository useful? :heart:
Support it by joining __[stargazers](https://github.com/T8RIN/ImageTextReader/stargazers)__ for this repository. :star: <br>
And __[follow](https://github.com/T8RIN)__ me for my next creations! 🤩

## License
```xml
Designed and developed by 2024 T8RIN

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
