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

import dagger.Component
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.FeatureScope

@FeatureScope
@Component(
        modules = [OcrModule::class],
        dependencies = [AppComponent::class]
)
interface OcrComponent {
    fun inject(scanPreviewFragment: ScanPreviewFragment)
    fun inject(scanPreviewViewModel: ScanPreviewViewModel)
}

@Module
class OcrModule {
    @Provides
    internal fun bindOcrFeatureImpl(ocrHandlerImpl: OcrHandlerImpl): OcrHandler = ocrHandlerImpl
}
