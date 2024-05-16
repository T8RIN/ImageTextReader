package com.t8rin.imagetextreader

import android.content.Context
import android.graphics.Bitmap
import com.t8rin.imagetextreader.data.AndroidImageTextReader
import com.t8rin.imagetextreader.domain.ImageTextReader

fun ImageTextReader(
    applicationContext: Context
): ImageTextReader<Bitmap> = AndroidImageTextReader(
    context = applicationContext.applicationContext
)