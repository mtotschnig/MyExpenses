package org.totschnig.myexpenses.changelog

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.dialog.VersionDialogFragment.Companion.resolveMoreInfo
import org.totschnig.myexpenses.ui.ContextHelper.wrap
import org.totschnig.myexpenses.viewmodel.data.VersionInfo
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale


const val CURRENT_VERSION = "728;3.8.2"
const val VERSION_DATE = "2014-04-15"

val versionInfo = VersionInfo(CURRENT_VERSION)

@RunWith(RobolectricTestRunner::class)
@Ignore("This is only run on demand in order to generate ChangeLog!")
class ChangeLogGenerator {

    @Test
    fun generateChangeLogPlay() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val allLanguages = arrayOf(
            "ar",
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
        print(
            buildString {
                allLanguages.forEach { language ->
                    appendLine("<$language>")
                    versionInfo.getChanges(wrap(context, Locale(language)))!!.forEach {
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
            Files.newOutputStream(
                Paths.get(System.getProperty("user.dir")).parent.resolve(
                    Path.of("metadata", language, "changelogs", "${versionInfo.code}.txt")
                )
            ).bufferedWriter().use { writer ->
                writer.write(
                    buildString {
                        versionInfo.getChanges(wrap(context, Locale(language)))!!.forEach {
                            appendLine("• $it")
                        }
                        appendLine()
                        appendLine(context.githubUrl(versionInfo))
                    }
                )
            }
        }
    }

    private fun Context.githubUrl(versionInfo: VersionInfo) =
        "https://github.com/mtotschnig/MyExpenses/projects/" +
        getString(resolveMoreInfo("project_board_", versionInfo)!!)

    private fun Context.mastodonUrl(versionInfo: VersionInfo) =
        "https://mastodon.social/@myexpenses/" +
        getString(resolveMoreInfo("version_more_info_", versionInfo)!!)

    @Test
    fun generateChangeLogYaml() {
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
            VERSION_DATE,
            buildMap {
                allLanguages.forEach { language ->
                    put(
                        language,
                        versionInfo.getChanges(wrap(context, Locale(language)))!!.joinToString(" ")
                    )
                }
            },
            buildMap {
                put("github", context.githubUrl(versionInfo))
                put("mastodon", context.mastodonUrl(versionInfo))
            }
        ))
        print(yaml.dump(data))
    }
}