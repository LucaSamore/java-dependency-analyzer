package pcd.ass02.async

import io.vertx.core.Vertx
import java.nio.file.Path
import pcd.ass02.async.implementation.DependencyAnalyzerLibImpl

fun main() {
  val vertx = Vertx.vertx()
  val analyzer = DependencyAnalyzerLibImpl(vertx)

  val file = Path.of("src/main/kotlin/pcd/ass02/samples/Sample.java")

  analyzer
      .getClassDependencies(file)
      .onSuccess {
        println("=== Class dependencies ===")
        println("Class: ${it.className}")
        println("Dependencies: ${it.usedTypes}")
      }
      .onFailure { println("Error: ${it.message}") }

  val folder = Path.of("src/main/kotlin/pcd/ass02/samples")

  analyzer
      .getPackageDependencies(folder)
      .onSuccess {
        println("=== Package dependencies ===")
        println("Package: ${it.packageName}")
        it.classReports.forEach(::println)
      }
      .onFailure { println("Error: ${it.message}") }

  val project = Path.of(".")

  analyzer
      .getProjectDependencies(project)
      .onSuccess {
        println("=== Project dependencies ===")
        it.packageReports.forEach(::println)
      }
      .onFailure { println("Error: ${it.message}") }
}
