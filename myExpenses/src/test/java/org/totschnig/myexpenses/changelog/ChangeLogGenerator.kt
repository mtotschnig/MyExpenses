package org.totschnig.myexpenses.changelog

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.ui.ContextHelper.wrap
import org.totschnig.myexpenses.viewmodel.data.VersionInfo
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.util.Locale
import kotlin.io.path.exists

@RunWith(RobolectricTestRunner::class)
@Ignore("This is only run on demand in order to generate ChangeLog!")
class ChangeLogGenerator {

    val context: Application
        get() = ApplicationProvider.getApplicationContext()

    private val versionInfo: VersionInfo
        get() = VersionInfo(context.resources.getStringArray(R.array.versions).first())

    @Test
    fun generateChangeLogPlay() {
        val allLanguages = arrayOf(
            "en-US",
            "ar",
            "bg",
            "ca",
            "cs-CZ",
            "da-DK",
            "de-DE",
            "el-GR",
            "es-ES",
            "eu-ES",
            "fr-FR",
            "hr",
            "hu-HU",
            "it-IT",
            "iw-IL",
            "ja-JP",
            "km-KH",
            "kn-IN",
            "ko-KR",
            "ms",
            "nl-NL",
            "pl-PL",
            "pt-BR",
            "pt-PT",
            "ro",
            "ru-RU",
            "si-LK",
            "ta-IN",
            "te-IN",
            "tr-TR",
            "vi",
            "zh-CN",
            "zh-TW"
        )
        print(
            buildString {
                allLanguages.forEach { language ->
                    appendLine("<$language>")
                    versionInfo.getChanges(wrap(context, Locale.forLanguageTag(language)))!!
                        .forEach {
                            appendLine("• $it")
                        }
                    appendLine("</$language>")
                }
            }
        )
    }

    @Test
    fun generateChangeLogFdroid() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val allLanguages = arrayOf(
            "ar",
            "bg-BG",
            "de-DE",
            "es-ES",
            "fr-FR",
            "he",
            "hu",
            "it-IT",
            "ja-JP",
            "ko",
            "ms",
            "pl-PL",
            "pt-PT",
            "ro",
            "ru-RU",
            "tr-TR",
            "zh-Hans",
            "en-US"
        )
        allLanguages.forEach { language ->
            val path = Paths.get(System.getProperty("user.dir")).parent.resolve(
                Path.of("metadata", language, "changelogs", "${versionInfo.code}.txt")
            )
            assertWithMessage("Changelog language $language, version ${versionInfo.code} exists")
                .that(path.exists()).isFalse()
            Files.newOutputStream(path).bufferedWriter().use { writer ->
                writer.write(
                    buildString {
                        versionInfo.getChanges(wrap(context, Locale.forLanguageTag(language)))!!
                            .forEach { appendLine("• $it") }
                        appendLine()
                        versionInfo.githubUrl(context)?.let {
                            appendLine(it)
                        }
                        versionInfo.mastodonUrl(context)?.let {
                            appendLine(it)
                        }
                    }
                )
            }
        }
    }

    @Test
    fun generateChangeLogYaml() {
        val versionDate = LocalDate.now().toString()
        val context = ApplicationProvider.getApplicationContext<Application>()
        val allLanguages = arrayOf(
            "bg",
            "cs",
            "de",
            "el",
            "es",
            "fr",
            "hr",
            "hu",
            "it",
            "iw",
            "nl",
            "pl",
            "pt",
            "ro",
            "tr",
            "vi",
            "en"
        )
        val yaml = Yaml(
            DumperOptions().apply {
                setIndent(2)
                setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
                setIndicatorIndent(2)
                indentWithIndicator = true
            }
        )

        val data = listOf(listOf(
            versionInfo.name,
            versionDate,
            buildMap {
                allLanguages.forEach { language ->
                    put(
                        language,
                        versionInfo.getChanges(wrap(context, Locale(language)))!!.joinToString(" ")
                    )
                }
            },
            buildMap {
                put("github2", versionInfo.githubLink(context)!!)
                put("mastodon", versionInfo.mastodonLink(context)!!)
            }
        ))
        print(yaml.dump(data))
    }
}