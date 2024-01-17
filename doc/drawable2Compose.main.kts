@file:Repository("https://jitpack.io")
@file:Repository("https://maven.google.com")
@file:Repository("https://jetbrains.bintray.com/trove4j")

@file:DependsOn("com.github.DevSrSouza:svg-to-compose:-SNAPSHOT")
//@file:DependsOn("com.android.tools:sdk-common:27.2.0-alpha16")
//@file:DependsOn("com.android.tools:common:27.2.0-alpha16")
//@file:DependsOn("com.squareup:kotlinpoet:1.7.2")
//@file:DependsOn("org.ogce:xpp3:1.1.6")

import br.com.devsrsouza.svg2compose.Svg2Compose
import br.com.devsrsouza.svg2compose.VectorType
import java.io.File

val assetsDir = File("assets")
val srcDir = File("myExpenses/src/main/java")

Svg2Compose.parse(
    applicationIconPackage = "assets",
    accessorName = "JetnewsAssets",
    outputSourceDirectory = srcDir,
    vectorsDirectory = assetsDir,
    type = VectorType.DRAWABLE,
    allAssetsPropertyName = "AllAssets"
)