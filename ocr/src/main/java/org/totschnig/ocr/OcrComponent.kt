/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.totschnig.ocr

import android.content.ContentResolver
import dagger.Component
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.preference.PrefHandler
import javax.inject.Singleton

@Singleton
@Component(
    modules = [OcrModule::class],
    dependencies = [ContentResolver::class, PrefHandler::class]
)
interface OcrComponent {
    fun ocrFeature(): OcrFeature
}

@Module
class OcrModule {
    @Provides
    internal fun bindOcrFeatureImpl(ocrFeatureImpl: OcrFeatureImpl): OcrFeature = ocrFeatureImpl
}
