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
        var function: ExtensionFunction = object : ExtensionFunction {
            override fun getName(): QName {
                return QName("http://myexpenses.mobi/", "displayNameForLanguage")
            }

            override fun getResultType(): SequenceType {
                return SequenceType.makeSequenceType(
                    ItemType.STRING, OccurrenceIndicator.ONE
                )
            }

            override fun getArgumentTypes(): Array<SequenceType> {
                return arrayOf(
                    SequenceType.makeSequenceType(
                        ItemType.STRING, OccurrenceIndicator.ONE
                    ),
                    SequenceType.makeSequenceType(
                        ItemType.STRING, OccurrenceIndicator.ONE
                    )
                )
            }

            @Throws(SaxonApiException::class)
            override fun call(arguments: Array<XdmValue>): XdmValue {
                val targetLanguage = (arguments[0].itemAt(0) as XdmAtomicValue).stringValue
                val displayLanguage = (arguments[1].itemAt(0) as XdmAtomicValue).stringValue
                return XdmAtomicValue(Locale(targetLanguage).getDisplayLanguage(Locale(displayLanguage)))
            }
        }
        @JvmStatic
        fun main(args: Array<String>) {
            Locale.setDefault(Locale("en"))
            when(args.getOrNull(0)) {
                "fdroid" -> {
                    if (args.size != 3) usage()
                    runTransform("doc/whatsnew2gplay2.xsl", args[1], "changelog.xml", args[2])
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
            processor.registerExtensionFunction(function)
            val out: Serializer = outFile?.let { processor.newSerializer(File(it)) } ?: processor.newSerializer(System.out)
            val compiler: XsltCompiler = processor.newXsltCompiler()
            val stylesheet: XsltExecutable =
                compiler.compile(StreamSource(File(styleSheet)))
            val transformer: XsltTransformer = stylesheet.load()
            transformer.initialTemplate = QName("main")
            transformer.setParameter(QName("version"), XdmAtomicValue(version))
            versionCode?.let {
                transformer.setParameter(QName("version"), XdmAtomicValue(it))
            }
            transformer.destination = out
            transformer.transform()
        }

        private fun usage() {
            System.err.println("Usage:\nChangelog fdroid <version> <versionCode>\nChangelog gplay\nChangelog yaml")
            exitProcess(0)
        }
    }
}