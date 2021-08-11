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
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmValue
import net.sf.saxon.s9api.XsltCompiler
import net.sf.saxon.s9api.XsltExecutable
import net.sf.saxon.s9api.XsltTransformer
import java.io.File
import java.util.*
import javax.xml.transform.TransformerException
import javax.xml.transform.stream.StreamSource
import kotlin.system.exitProcess

@Suppress("unused")
class Main {
    companion object {
        var displayNameForLanguage: ExtensionFunction = object : ExtensionFunction {
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

        var fileExists: ExtensionFunction = object : ExtensionFunction {
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

        @JvmStatic
        fun main(args: Array<String>) {
            Locale.setDefault(Locale("en"))
            when(args.getOrNull(0)) {
                "fdroid" -> {
                    if (args.size != 3) usage()
                    runTransform("doc/whatsnew2fdroid.xsl", args[1], "changelog.xml", args[2])
                }
                "gplay" -> {
                    if (args.size != 2) usage()
                    runTransform("doc/whatsnew2gplay2.xsl", args[1])
                }
                "yaml" -> {
                    if (args.size != 2) usage()
                    runTransform("doc/whatsnew2yaml.xsl", args[1])
                }
                else -> {
                    usage()
                }
            }
        }

        private fun runTransform(styleSheet: String, version: String, outFile: String? = null, versionCode: String? = null) {
            val processor = Processor(false)
            processor.registerExtensionFunction(displayNameForLanguage)
            processor.registerExtensionFunction(fileExists)
            val out: Serializer = outFile?.let { processor.newSerializer(File(it)) } ?: processor.newSerializer(System.out)
            val compiler: XsltCompiler = processor.newXsltCompiler()
            val stylesheet: XsltExecutable =
                compiler.compile(StreamSource(File(styleSheet)))
            val transformer: XsltTransformer = stylesheet.load()
            transformer.initialTemplate = QName("main")
            transformer.setParameter(QName("version"), XdmAtomicValue(version))
            versionCode?.let {
                transformer.setParameter(QName("versionCode"), XdmAtomicValue(it))
            }
            transformer.destination = out
            transformer.transform()
        }

        private fun usage() {
            System.err.println("Usage:\nChangelog fdroid <version> <versionCode>\nChangelog gplay <version>\nChangelog yaml <version>")
            exitProcess(0)
        }
    }
}