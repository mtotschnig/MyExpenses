package org.totschnig.changelog

import net.sf.saxon.s9api.ExtensionFunction
import net.sf.saxon.s9api.ItemType
import net.sf.saxon.s9api.OccurrenceIndicator
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.SaxonApiException
import net.sf.saxon.s9api.SequenceType
import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.s9api.XsltCompiler
import net.sf.saxon.s9api.XsltExecutable
import net.sf.saxon.s9api.XsltTransformer
import java.io.File
import java.lang.UnsupportedOperationException
import java.util.*
import javax.xml.transform.stream.StreamSource
import kotlin.system.exitProcess

@Suppress("unused")
class Main {
    companion object {
        private var displayNameForLanguage: ExtensionFunction = object : ExtensionFunction {
            override fun getName() =
                QName("http://myexpenses.mobi/", "displayNameForLanguage")

            override fun getResultType() = SequenceType.makeSequenceType(
                ItemType.STRING, OccurrenceIndicator.ONE
            )

            override fun getArgumentTypes() = arrayOf(
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                ),
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                )
            )

            @Throws(SaxonApiException::class)
            override fun call(arguments: Array<XdmValue>): XdmValue {
                val targetLanguage = (arguments[0].itemAt(0) as XdmAtomicValue).stringValue
                val displayLanguage = (arguments[1].itemAt(0) as XdmAtomicValue).stringValue
                return XdmAtomicValue(Locale(targetLanguage).getDisplayLanguage(Locale(displayLanguage)))
            }
        }

        private var displayNameForCountry: ExtensionFunction = object : ExtensionFunction {
            override fun getName() =
                QName("http://myexpenses.mobi/", "displayNameForCountry")

            override fun getResultType() = SequenceType.makeSequenceType(
                ItemType.STRING, OccurrenceIndicator.ONE
            )

            override fun getArgumentTypes() = arrayOf(
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                ),
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                )
            )

            @Throws(SaxonApiException::class)
            override fun call(arguments: Array<XdmValue>): XdmValue {
                val targetCountry = (arguments[0].itemAt(0) as XdmAtomicValue).stringValue
                val displayLanguage = (arguments[1].itemAt(0) as XdmAtomicValue).stringValue
                return XdmAtomicValue(when(targetCountry) {
                    "de" -> Locale.GERMANY
                    else -> throw UnsupportedOperationException()
                }.getDisplayCountry(Locale(displayLanguage)))
            }
        }

        private var fileExists: ExtensionFunction = object : ExtensionFunction {
            override fun getName() = QName("http://myexpenses.mobi/", "fileExists")

            override fun getResultType() = SequenceType.makeSequenceType(
                ItemType.BOOLEAN, OccurrenceIndicator.ONE
            )

            override fun getArgumentTypes() = arrayOf(
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                )
            )

            @Throws(SaxonApiException::class)
            override fun call(arguments: Array<XdmValue>): XdmValue {
                val path = (arguments[0].itemAt(0) as XdmAtomicValue).stringValue
                return XdmAtomicValue(File(path).exists())
            }
        }

        private var displayNameForScript: ExtensionFunction = object : ExtensionFunction {
            override fun getName() =
                QName("http://myexpenses.mobi/", "displayNameForScript")

            override fun getResultType() = SequenceType.makeSequenceType(
                ItemType.STRING, OccurrenceIndicator.ONE
            )

            override fun getArgumentTypes() = arrayOf(
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                ),
                SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                )
            )

            @Throws(SaxonApiException::class)
            override fun call(arguments: Array<XdmValue>): XdmValue {
                val script = (arguments[0].itemAt(0) as XdmAtomicValue).stringValue
                val displayLanguage = (arguments[1].itemAt(0) as XdmAtomicValue).stringValue
                return XdmAtomicValue(getDisplayNameForScript(Locale(displayLanguage), script))
            }
        }

        private fun getDisplayNameForScript(locale: Locale, script: String): String =
            when(script) {
                "Han" -> Locale.CHINESE.getDisplayLanguage(locale)
                else -> Locale.Builder().setScript(script).build().getDisplayScript(locale)
            }

        @JvmStatic
        fun main(args: Array<String>) {
            Locale.setDefault(Locale("en"))
            val parameters = mutableMapOf("version" to args[1])
            when(args.getOrNull(0)) {
                "fdroid" -> {
                    if (args.size != 3) usage()
                    parameters["versionCode"] = args[2]
                    runTransform("doc/whatsnew2fdroid.xsl", parameters, "changelog.xml")
                }
                "gplay" -> {
                    if (args.size != 2) usage()
                    runTransform("doc/whatsnew2gplay2.xsl", parameters)
                }
                "yaml" -> {
                    if (args.size != 4) usage()
                    parameters["versionDate"] = args[2]
                    parameters["appendDot"] = args[3]
                    runTransform("doc/whatsnew2yaml.xsl", parameters)
                }
                else -> {
                    usage()
                }
            }
        }

        private fun runTransform(
            styleSheet: String,
            parameters: Map<String, String>,
            outFile: String? = null
        ) {
            val processor = Processor(false)
            processor.registerExtensionFunction(displayNameForLanguage)
            processor.registerExtensionFunction(fileExists)
            processor.registerExtensionFunction(displayNameForScript)
            processor.registerExtensionFunction(displayNameForCountry)
            val out: Serializer = outFile?.let { processor.newSerializer(File(it)) } ?: processor.newSerializer(System.out)
            val compiler: XsltCompiler = processor.newXsltCompiler()
            val stylesheet: XsltExecutable =
                compiler.compile(StreamSource(File(styleSheet)))
            val transformer: XsltTransformer = stylesheet.load()
            transformer.initialTemplate = QName("main")
            parameters.forEach { (name, value) ->
                transformer.setParameter(QName(name), XdmAtomicValue(value))
            }
            transformer.destination = out
            transformer.transform()
        }

        private fun usage() {
            System.err.println("Usage:\nChangelog fdroid <version> <versionCode>\nChangelog gplay <version>\nChangelog yaml <version> <versionDate> <appendDot>")
            exitProcess(0)
        }
    }
}