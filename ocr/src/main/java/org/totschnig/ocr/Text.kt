package org.totschnig.ocr

import android.graphics.Rect
import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Text(val textBlocks: List<TextBlock>): Parcelable

@Parcelize
@Keep
data class TextBlock(val lines: List<Line>): Parcelable

@Parcelize
@Keep
data class Line(val text: String, val boundingBox: Rect?, val elements: List<Element>): Parcelable

@Parcelize
@Keep
data class Element(val text: String, val boundingBox: Rect?): Parcelable