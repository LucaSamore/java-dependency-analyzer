package pcd.ass02

import io.vertx.core.Vertx
import java.nio.file.Path
import pcd.ass02.implementation.DependencyAnalyzerLibImpl

fun main() {
  val vertx = Vertx.vertx()
  val analyzer = DependencyAnalyzerLibImpl(vertx)

  val file = Path.of("src/main/kotlin/pcd/ass02/Sample.java")

  analyzer
      .getClassDependencies(file)
      .onSuccess {
        println("Class: ${it.className}")
        println("Dependencies: ${it.usedTypes}")
      }
      .onFailure { println("Error: ${it.message}") }
}
