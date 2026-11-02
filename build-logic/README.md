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
| `next.data` | `next.android.library` | `:next:core:common` as `api`; for tests as `next.feature`. Fails on a dependency on a feature, `:next:core:ui`, `:designsystem`, `:charts`, the shell or the app (rule `mod-data-headless`) |
| `next.feature` | `next.android.library`, `next.android.compose` | `:next:core:ui`, activity, lifecycle, icons, data store, Navigation 3; for tests JUnit, org.json, coroutines test and the fixtures `FakeLedgerSource`, `withTestDataStore`. Fails on a dependency on another feature, the shell or the app (rule `mod-feature-isolated`) |
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

## Module graph

`next.module-graph` keeps `ui_next/modules.puml` in line with the build files, so that the diagram
of the modules is never drawn by hand (rule `mod-diagram` of `ui_next/ARCHITECTURE.md`):

- `./gradlew :ui_next:moduleGraph` writes the file again; run it after adding a module or a
  dependency between modules, and commit the file with the change.
- `./gradlew :ui_next:checkModuleGraph` (also part of `check`) fails while the file is out of date.

The graph starts at the `roots` in the `moduleGraph { }` block of `ui_next/build.gradle` and follows
the modules with a `next.*` convention plugin; others like `:myExpenses` are shown but not followed.
Test dependencies are left out, and so is an arrow that follows from a longer path. View the file
with the PlantUML plugin of Android Studio or `java -jar plantuml.jar modules.puml`. How it works
and where to change what is described at the top of `ModuleGraphPlugin.kt`.
