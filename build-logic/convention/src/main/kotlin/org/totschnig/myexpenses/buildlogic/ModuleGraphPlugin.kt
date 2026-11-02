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
 *
 * Why: a diagram drawn by hand is out of date after the next new module. Generated, it always shows
 * the real dependencies, and since the file is committed, a new edge shows up in the diff of a
 * commit and can be reviewed (`mod-diagram` asks for a reason in the commit message). No published plugin does
 * the same: they write Graphviz or Mermaid, or show every edge, or have no check that fails.
 *
 * How it works, in the order it runs:
 *
 * 1. [apply] registers the extension `moduleGraph { roots; output }` and the two tasks.
 * 2. After all projects are evaluated, i.e. once every `build.gradle` has declared its
 *    dependencies, [moduleGraph] walks from the roots over the project dependencies,
 *    [transitiveReduction] drops the edges that follow from longer paths, and [renderPlantUml]
 *    turns the result into text. The text goes into a [Property], so that the tasks only see a
 *    string and no [Project]; that keeps them compatible with the configuration cache.
 * 3. [WriteModuleGraph] writes that text to the file, [CheckModuleGraph] compares it with the file
 *    character by character. So any change of the output, even of a comment, needs `moduleGraph`
 *    to be run once, otherwise `check` fails.
 *
 * Where to change what:
 *
 * - a new convention plugin whose modules should be followed: [conventionPlugins],
 * - which dependencies count, e.g. to show test dependencies: [projectDependencies],
 * - the label in brackets under a module name: [kind],
 * - the look of the diagram (packages, direction, aliases): [renderPlantUml],
 * - another start module or file: the `moduleGraph { }` block in `ui_next/build.gradle`.
 */
class ModuleGraphPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val extension = extensions.create<ModuleGraphExtension>("moduleGraph")
        extension.roots.convention(listOf(path))
        extension.output.convention(layout.projectDirectory.file("modules.puml"))

        val writeTaskPath = "${path.removeSuffix(":")}:moduleGraph"
        val diagram = objects.property(String::class.java)
        // Only now the dependencies of the other modules are declared; earlier they would be missing.
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

/** Writes the diagram; `./gradlew :ui_next:moduleGraph` after a dependency between modules changed. */
abstract class WriteModuleGraph : DefaultTask() {
    @get:Input
    abstract val diagram: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun write() = output.get().asFile.writeText(diagram.get())
}

/**
 * Fails when the committed file differs from what [WriteModuleGraph] would write, so that a new or
 * removed dependency between modules cannot go unnoticed. It does not write the file itself, a
 * changed edge should be looked at before it is committed.
 */
abstract class CheckModuleGraph : DefaultTask() {
    @get:Input
    abstract val diagram: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val file: RegularFileProperty

    /** Path of the write task, only for the message, e.g. `:ui_next:moduleGraph`. */
    @get:Input
    abstract val taskPath: Property<String>

    @TaskAction
    fun check() {
        if (file.get().asFile.readText() != diagram.get()) {
            throw GradleException(
                "${file.get().asFile.name} does not match the dependencies of the modules (mod-diagram). " +
                    "Run ./gradlew ${taskPath.get()} and review the changed edges."
            )
        }
    }
}

/** A module in the diagram: its Gradle path, e.g. `:next:feature:budgets`, and [kind]. */
internal data class Module(val path: String, val kind: String?)

/** Modules sorted by path, edges as (module, module it depends on), both as Gradle paths. */
internal data class ModuleGraph(val modules: List<Module>, val edges: Set<Pair<String, String>>)

/**
 * A module with one of these plugins belongs to the new UI, so its dependencies are followed. The
 * others, above all `:myExpenses`, are shown as a box, but their own dependencies are not: they
 * would fill the diagram with upstream modules that `mod-diagram` is not about. `next.data` and
 * `next.android.compose` are missing on purpose, they always come with `next.android.library`.
 */
private val conventionPlugins = listOf(
    "next.android.library", "next.android.feature", "next.feature", "next.jvm",
)

/** Label under the module name for what is not a plain Android library; null for a library. */
private fun Project.kind(): String? = when {
    pluginManager.hasPlugin("com.android.application") -> "app"
    pluginManager.hasPlugin("com.android.dynamic-feature") -> "dynamic feature"
    pluginManager.hasPlugin("org.jetbrains.kotlin.jvm") -> "Kotlin JVM"
    else -> null
}

/**
 * Dependencies of the production code: no tests, test fixtures, annotation processors or desugaring.
 * Matches `api`, `implementation`, `compileOnly`, `runtimeOnly` and their variants such as
 * `debugImplementation` or `internApi`; any name with "test" in it, e.g. `testImplementation` or
 * `androidTestImplementation`, is left out.
 */
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

/**
 * Breadth-first walk from [roots]: every module reached becomes a box; the dependencies of a root or
 * of a module with one of the [conventionPlugins] become edges and are walked in turn.
 */
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

/**
 * Leaves out an edge a → c when a reaches c over other edges, so that the diagram stays readable.
 * Example: when a feature declares `:next:core:ui` and `:next:core:common`, and `:next:core:ui`
 * depends on `:next:core:common` too, the arrow from the feature to `:next:core:common` is
 * dropped, because it follows from feature → `:next:core:ui` → `:next:core:common`. Without this,
 * almost every module would have an arrow to the core modules. Which modules depend on which stays
 * the same, only the direct arrows are fewer; the build files still declare all of them.
 *
 * For each edge it searches depth-first for another way from its start to its end, with this edge
 * itself skipped. The result is sorted, so that the file does not change from run to run.
 */
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

/**
 * PlantUML component diagram. Modules on the top level, e.g. `:designsystem`, are single boxes;
 * nested ones, e.g. `:next:core:ui`, go into a package named after their parent (`next/core`) and
 * show only their last name (`:ui`). An arrow points from a module to the module it depends on.
 *
 * When every module in a package has an edge to the same outside module X (or X has one to every
 * member), the individual arrows are replaced by a single arrow from (or to) the package alias.
 * A hidden edge from the deepest module in a package to the next package keeps the layers stacked
 * top to bottom without adding visible arrows.
 */
internal fun renderPlantUml(graph: ModuleGraph): String = buildString {
    val (grouped, single) = graph.modules.partition { packageOf(it.path) != null }
    // packageMembers: parent path → sorted list of module paths in that package.
    val packageMembers: Map<String, List<String>> = grouped
        .groupBy { it.path.substringBeforeLast(':') }
        .mapValues { (_, ms) -> ms.map { it.path }.sorted() }
    val remainingEdges = graph.edges.toMutableSet()
    val packageEdges = collapsePackageEdges(graph, packageMembers, remainingEdges)
    val hiddenEdges = hiddenEdges(graph, packageMembers, packageEdges)

    appendLine("@startuml modules")
    appendLine("' Generated by the task moduleGraph of build-logic (ModuleGraphPlugin), do not edit.")
    appendLine("' Dependencies between the modules of the new UI, rule `mod-diagram` of ARCHITECTURE.md.")
    appendLine("' An edge that follows from a longer path is left out.")
    if (packageEdges.isNotEmpty()) {
        appendLine("' An edge from or to a package stands for an edge from or to every module in it.")
    }
    appendLine("skinparam componentStyle rectangle")
    appendLine("skinparam linetype ortho")
    appendLine("skinparam ranksep 80")
    appendLine("top to bottom direction")
    appendLine()
    single.forEach { appendComponent(it, it.path, "") }
    packageMembers.keys.sorted().forEach { parent ->
        appendLine()
        appendLine("package \"${parent.removePrefix(":").replace(':', '/')}\" as ${packageAlias(parent)} {")
        grouped.filter { it.path.substringBeforeLast(':') == parent }
            .forEach { appendComponent(it, ":" + it.path.substringAfterLast(':'), "    ") }
        appendLine("}")
    }
    if (hiddenEdges.isNotEmpty()) {
        appendLine()
        appendLine("' Layout only")
        hiddenEdges.forEach { appendLine(it) }
    }
    appendLine()
    packageEdges.forEach { (from, to) -> appendLine("$from --> $to") }
    remainingEdges.forEach { (from, to) -> appendLine("${moduleAlias(from)} --> ${moduleAlias(to)}") }
    appendLine("@enduml")
}

// PlantUML needs an identifier without ':' or '-' for the arrows, e.g. :next:core:ui → mNextCoreUi.
private fun moduleAlias(path: String) = "m" + path.split(':', '-', '_').filter { it.isNotEmpty() }
    .joinToString("") { it.replaceFirstChar(Char::uppercase) }

// Package alias, e.g. ":next:core" → "pNextCore".
private fun packageAlias(parent: String) = "p" + parent.removePrefix(":")
    .split(':', '/', '-', '_').filter { it.isNotEmpty() }
    .joinToString("") { it.replaceFirstChar(Char::uppercase) }

/** The package of a nested module, e.g. `:next:core` for `:next:core:ui`; null on the top level. */
private fun packageOf(path: String): String? =
    if (path.count { it == ':' } > 1) path.substringBeforeLast(':') else null

private fun StringBuilder.appendComponent(module: Module, name: String, indent: String) {
    val label = listOfNotNull(name, module.kind?.let { "($it)" }).joinToString("\\n")
    appendLine("$indent[$label] as ${moduleAlias(module.path)}")
}

/**
 * When ALL modules in a package have an edge to X (or from X), replaces them with one package edge.
 * Removes the replaced edges from [remainingEdges] and returns the package edges as aliases, sorted.
 */
private fun collapsePackageEdges(
    graph: ModuleGraph,
    packageMembers: Map<String, List<String>>,
    remainingEdges: MutableSet<Pair<String, String>>,
): List<Pair<String, String>> {
    val allPaths = graph.modules.map { it.path }.toSet()
    val packageEdges = mutableListOf<Pair<String, String>>()
    for ((parent, members) in packageMembers.entries.sortedBy { it.key }) {
        if (members.size < 2) continue
        val membersSet = members.toSet()
        for (x in (allPaths - membersSet).sorted()) {
            if (membersSet.all { Pair(it, x) in remainingEdges }) {      // P --> X
                membersSet.forEach { remainingEdges.remove(it to x) }
                packageEdges += packageAlias(parent) to moduleAlias(x)
            }
            if (membersSet.all { Pair(x, it) in remainingEdges }) {      // X --> P
                membersSet.forEach { remainingEdges.remove(x to it) }
                packageEdges += moduleAlias(x) to packageAlias(parent)
            }
        }
    }
    return packageEdges.sortedWith(compareBy({ it.first }, { it.second }))
}

/** Longest path from the roots of the package to [modulePath], over edges inside [members] only. */
private fun depthWithin(graph: ModuleGraph, modulePath: String, members: Set<String>): Int {
    val internal = graph.edges.filter { (a, b) -> a in members && b in members }
    val dist = members.associateWith { 0 }.toMutableMap()
    val inDeg = members.associateWith { m -> internal.count { it.second == m } }.toMutableMap()
    val queue = ArrayDeque(members.filter { (inDeg[it] ?: 0) == 0 }.sorted())
    while (queue.isNotEmpty()) {
        val cur = queue.removeFirst()
        internal.filter { it.first == cur }.sortedBy { it.second }.forEach { (_, next) ->
            dist[next] = maxOf(dist[next] ?: 0, (dist[cur] ?: 0) + 1)
            inDeg[next] = (inDeg[next] ?: 0) - 1
            if ((inDeg[next] ?: 0) == 0) queue.addLast(next)
        }
    }
    return dist[modulePath] ?: 0
}

/** One hidden edge per package dependency, to keep the layers stacked without visible arrows. */
private fun hiddenEdges(
    graph: ModuleGraph,
    packageMembers: Map<String, List<String>>,
    packageEdges: List<Pair<String, String>>,
): List<String> {
    // Package dependency graph: P → Q if any module in P has an edge to any module in Q.
    val packageDeps: Set<Pair<String, String>> = graph.edges.mapNotNull { (a, b) ->
        val pa = packageOf(a)
        val pb = packageOf(b)
        if (pa != null && pb != null && pa != pb) pa to pb else null
    }.toSet()
    // Skip a pair when a package edge already connects P to a module inside Q.
    val covered: Set<Pair<String, String>> = packageEdges.mapNotNull { (from, to) ->
        val fromPkg = packageMembers.keys.find { packageAlias(it) == from } ?: return@mapNotNull null
        val toMod = graph.modules.find { moduleAlias(it.path) == to } ?: return@mapNotNull null
        val toPkg = packageOf(toMod.path) ?: return@mapNotNull null
        fromPkg to toPkg
    }.toSet()
    return packageDeps
        .filter { it !in covered }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .map { (p, q) ->
            val members = packageMembers.getValue(p).toSet()
            // Bottom = module with the longest path from package roots; alphabetical on ties.
            val bottom = members.maxWith(compareBy<String> { depthWithin(graph, it, members) }.thenBy { it })
            "${moduleAlias(bottom)} -[hidden]--> ${packageAlias(q)}"
        }
}
