# build-logic

Convention plugins for the modules of the new UI, so that a module's `build.gradle` names only what
is special about it: namespace, its dependencies, its own tasks. The modules of upstream keep their
own build files.

| Plugin | Applies | Sets |
|---|---|---|
| `next.android.library` | `com.android.library` | `compileSdk`, `minSdk` from the `ext` block of the root `build.gradle`, Java 17 |
| `next.android.feature` | `com.android.dynamic-feature` | as above, plus `:myExpenses`, desugaring and the dimension `textRecognition` = `extern` |
| `next.android.compose` | `org.jetbrains.kotlin.plugin.compose` | `buildFeatures.compose`, Compose BOM, tooling for previews; after one of the two above |
| `next.jvm` | `org.jetbrains.kotlin.jvm` | Java and Kotlin target 17 |
| `next.module-graph` | – | the tasks `moduleGraph` and `checkModuleGraph` (part of `check`), which write and check `modules.puml` from the declared dependencies (rule `mod-diagram`); applied in `:ui_next` |

A new library with Compose:

```groovy
plugins {
    id 'next.android.library'
    id 'next.android.compose'
}

android {
    namespace 'org.totschnig.myexpenses.example'
}
```

The versions of the Android and Kotlin plugins come from `gradle/libs.versions.toml`. The plugins are
only `compileOnly` here; the root `build.gradle` puts them on the classpath, so that all modules use
the same ones.
