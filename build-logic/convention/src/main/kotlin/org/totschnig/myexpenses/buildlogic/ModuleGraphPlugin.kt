package org.totschnig.myexpenses.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/**
 * Writes the dependencies between the modules of the new UI as a PlantUML diagram (rule `mod-diagram`), e.g.
 * `ui_next/modules.puml`, so that it is never drawn by hand:
 *
 * - `moduleGraph` writes the file from the declared project dependencies,
 * - `checkModuleGraph` fails when the file no longer matches them, and runs with `check`.
 *
 * The graph starts at [ModuleGraphExtension.roots] and follows the modules that apply one of the
 * `next.*` convention plugins. Other modules, e.g. `:myExpenses`, are shown but not followed.
 */
class ModuleGraphPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<ModuleGraphExtension>("moduleGraph")
        extension.roots.convention(listOf(path))
        extension.output.convention(layout.projectDirectory.file("modules.puml"))

        val writeTaskPath = "${path.removeSuffix(":")}:moduleGraph"
        val diagram = objects.property(String::class.java)
        gradle.projectsEvaluated {
            diagram.set(renderPlantUml(moduleGraph(rootProject, extension.roots.get())))
        }

        val write = tasks.register<WriteModuleGraph>("moduleGraph") {
            group = "documentation"
            description = "Writes the dependencies between the modules of the new UI as PlantUML"
            this.diagram.set(diagram)
            output.set(extension.output)
        }
        val check = tasks.register<CheckModuleGraph>("checkModuleGraph") {
            group = "verification"
            description = "Fails when the module diagram does not match the declared dependencies"
            this.diagram.set(diagram)
            file.set(extension.output)
            taskPath.set(writeTaskPath)
            mustRunAfter(write)
        }
        tasks.named("check") { dependsOn(check) }
        Unit
    }
}

interface ModuleGraphExtension {
    /** The modules the graph starts at, e.g. the shell and the fixture collector. */
    val roots: ListProperty<String>
    val output: RegularFileProperty
}

abstract class WriteModuleGraph : DefaultTask() {
    @get:Input
    abstract val diagram: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun write() = output.get().asFile.writeText(diagram.get())
}

abstract class CheckModuleGraph : DefaultTask() {
    @get:Input
    abstract val diagram: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val file: RegularFileProperty

    @get:Input
    abstract val taskPath: Property<String>

    @TaskAction
    fun check() {
        if (file.get().asFile.readText() != diagram.get()) {
            throw GradleException(
                "${file.get().asFile.name} does not match the dependencies of the modules (`mod-diagram`). " +
                    "Run ./gradlew ${taskPath.get()} and review the changed edges."
            )
        }
    }
}

internal data class Module(val path: String, val kind: String?)

internal data class ModuleGraph(val modules: List<Module>, val edges: Set<Pair<String, String>>)

private val conventionPlugins = listOf(
    "next.android.library", "next.android.feature", "next.feature", "next.jvm",
)

private fun Project.kind(): String? = when {
    pluginManager.hasPlugin("com.android.application") -> "app"
    pluginManager.hasPlugin("com.android.dynamic-feature") -> "dynamic feature"
    pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") -> "Kotlin JVM"
    else -> null
}

/** Dependencies of the production code: no tests, test fixtures, annotation processors or desugaring. */
private fun Project.projectDependencies(): Set<String> = configurations
    .filter { configuration ->
        val name = configuration.name
        !name.contains("test", ignoreCase = true) &&
            listOf("api", "implementation", "compileOnly", "runtimeOnly")
                .any { name == it || name.endsWith(it.replaceFirstChar(Char::uppercase)) }
    }
    .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
    .map { it.path }
    .filter { it != path }
    .toSet()

internal fun moduleGraph(root: Project, roots: List<String>): ModuleGraph {
    val modules = linkedMapOf<String, Module>()
    val edges = mutableSetOf<Pair<String, String>>()
    val queue = ArrayDeque(roots)
    while (queue.isNotEmpty()) {
        val path = queue.removeFirst()
        if (path in modules) continue
        val project = root.project(path)
        modules[path] = Module(path, project.kind())
        if (path in roots || conventionPlugins.any { project.pluginManager.hasPlugin(it) }) {
            project.projectDependencies().forEach { dependency ->
                edges += path to dependency
                queue += dependency
            }
        }
    }
    return ModuleGraph(modules.values.sortedBy { it.path }, transitiveReduction(edges))
}

/** Leaves out an edge a → c when a reaches c over other edges, so that the diagram stays readable. */
internal fun transitiveReduction(edges: Set<Pair<String, String>>): Set<Pair<String, String>> {
    val targets = edges.groupBy({ it.first }, { it.second })
    fun reaches(from: String, to: String, skip: Pair<String, String>): Boolean {
        val seen = mutableSetOf<String>()
        val stack = ArrayDeque(targets[from].orEmpty().filter { from to it != skip })
        while (stack.isNotEmpty()) {
            val next = stack.removeLast()
            if (next == to) return true
            if (seen.add(next)) stack += targets[next].orEmpty()
        }
        return false
    }
    return edges.filterTo(sortedSetOf(compareBy({ it.first }, { it.second }))) { !reaches(it.first, it.second, it) }
}

internal fun renderPlantUml(graph: ModuleGraph): String = buildString {
    fun alias(path: String) = "m" + path.split(':', '-', '_').filter { it.isNotEmpty() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }
    fun component(module: Module, name: String, indent: String) {
        val label = listOfNotNull(name, module.kind?.let { "($it)" }).joinToString("\\n")
        appendLine("$indent[$label] as ${alias(module.path)}")
    }

    appendLine("@startuml modules")
    appendLine("' Generated by the task moduleGraph of build-logic (ModuleGraphPlugin), do not edit.")
    appendLine("' Dependencies between the modules of the new UI, rule `mod-diagram` of ARCHITECTURE.md.")
    appendLine("' An edge that follows from a longer path is left out.")
    appendLine("skinparam componentStyle rectangle")
    appendLine("top to bottom direction")
    appendLine()
    val (grouped, single) = graph.modules.partition { it.path.count { c -> c == ':' } > 1 }
    single.forEach { component(it, it.path, "") }
    grouped.groupBy { it.path.substringBeforeLast(':') }.forEach { (parent, members) ->
        appendLine()
        appendLine("package \"${parent.removePrefix(":").replace(':', '/')}\" {")
        members.forEach { component(it, ":" + it.path.substringAfterLast(':'), "    ") }
        appendLine("}")
    }
    appendLine()
    graph.edges.forEach { (from, to) -> appendLine("${alias(from)} --> ${alias(to)}") }
    appendLine("@enduml")
}
