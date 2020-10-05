package org.totschnig.ocr

import android.graphics.Rect
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Text(val textBlocks: List<TextBlock>): Parcelable

@Parcelize
data class TextBlock(val lines: List<Line>): Parcelable

@Parcelize
data class Line(val text: String, val boundingBox: Rect?, val elements: List<Element>): Parcelable

@Parcelize
data class Element(val text: String, val boundingBox: Rect?): Parcelable