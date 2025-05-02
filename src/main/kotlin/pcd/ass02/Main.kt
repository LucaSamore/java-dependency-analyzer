package pcd.ass02

import io.vertx.core.Vertx
import pcd.ass02.implementation.DependencyAnalyzerLibImpl
import java.io.File

fun main () {
  val vertx = Vertx.vertx()
  val analyser = DependencyAnalyzerLibImpl(vertx)

  val file = File("src/main/kotlin/pcd/ass02/Sample.java")

  analyser.getClassDependencies(file).onSuccess {
    println("Class: ${it.className}")
    println("Dependencies: ${it.usedTypes}")
  }.onFailure {
    println("Error: ${it.message}")
  }
}
