package pcd.ass02.reactive.parser

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import pcd.ass02.reactive.implementation.JavaDependencyVisitor
import pcd.ass02.reactive.model.Dependency

class Parser {

  companion object {
    private const val JAVA_EXTENSION = ".java"
  }

  /**
   * Analyses the Java files in the given project path and emits the dependencies found.
   * Uses a functional reactive approach to process files and extract dependencies.
   */
  fun analyse(projectPath: String): Flowable<Dependency> {
    return validateProjectPath(projectPath)
      .flatMap { dir ->
        Flowable.fromIterable(findJavaFiles(dir))
      }

      .flatMap { file ->
        processJavaFile(file) }
      .subscribeOn(Schedulers.io())
  }

  /**
   * Validates if the given project path exists and is a directory.
   */
  private fun validateProjectPath(projectPath: String): Flowable<File> {
    return Flowable.defer {
      val projectDir = File(projectPath)
      if (!projectDir.exists() || !projectDir.isDirectory) {
        Flowable.error(IllegalArgumentException("Path not valid: $projectPath"))
      } else {
        Flowable.just(projectDir)
      }
    }
  }

  private fun processJavaFile(file: File): Flowable<Dependency> {
    return Flowable.create({ emitter ->
      try {
        val compilationUnit = parseJavaFile(file)
        if (compilationUnit != null) {
          processDependencies(compilationUnit, emitter)
        }

        if (!emitter.isCancelled) {
          emitter.onComplete()
        }
      } catch (e: Exception) {
        println("Error during analysis of file ${file.name}: ${e.message}")
      }
    }, BackpressureStrategy.BUFFER)
  }

  private fun parseJavaFile(file: File): CompilationUnit? {
    return runCatching {
      val parser = JavaParser()
      val parseResult = parser.parse(file.inputStream())

      if (!parseResult.isSuccessful) {
        println("Parse error in ${file.name}: ${parseResult.problems}")
        null
      } else {
        parseResult.result.orElse(null)
      }
    }.onFailure { e ->
      println("Failed to parse file ${file.name}: ${e.message}")
    }.getOrNull()
  }

  private fun processDependencies(
    compilationUnit: CompilationUnit,
    emitter: FlowableEmitter<Dependency>
  ) {
    val packageName: String = compilationUnit.packageDeclaration
      .map { it.nameAsString }
      .orElse("")

    // Find all classes (also multiple classes in a single file) and interfaces
    compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
      val className: String = classDecl.nameAsString
      val fullClassName: String = if (packageName.isEmpty()) className else "$packageName.$className"

      val visitor = JavaDependencyVisitor(fullClassName)
      visitor.visit(compilationUnit, null)

      visitor.dependencies.forEach { dependency ->
        if (!emitter.isCancelled) {
          emitter.onNext(dependency)
        }
      }
    }
  }

  private fun findJavaFiles(dir: File): List<File> {
    val files = dir.listFiles() ?: return emptyList()

    return files.flatMap { file ->
      when {
        file.isDirectory -> findJavaFiles(file)
        file.isFile && file.name.endsWith(JAVA_EXTENSION, ignoreCase = true) -> listOf(file)
        else -> emptyList()
      }
    }
  }
}
