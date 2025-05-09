package pcd.ass02.reactive.implementation

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.schedulers.Schedulers
import pcd.ass02.reactive.ParserLib
import pcd.ass02.reactive.model.Dependency
import java.io.File

class ParserLibImpl : ParserLib {

  override fun parseProject(projectPath: String): Flowable<Dependency> {
    return validatePath(projectPath)
      .flatMap { directory -> Flowable.fromIterable(findJavaFiles(directory)) }
      .flatMap { file -> parseFile(file) }
      .subscribeOn(Schedulers.io())
  }

  private fun validatePath(projectPath: String): Flowable<File> {
    return Flowable.defer {
      val projectDir = File(projectPath)
      if (!projectDir.exists() || !projectDir.isDirectory) {
        Flowable.error(IllegalArgumentException("Invalid project path: $projectPath"))
      } else {
        Flowable.just(projectDir)
      }
    }
  }

  private fun findJavaFiles(directory: File): List<File> {
    val files = directory.listFiles() ?: return emptyList()

    return files.flatMap { file ->
      when {
        file.isDirectory -> findJavaFiles(file)
        file.isFile && file.name.endsWith(".java", ignoreCase = true) -> listOf(file)
        else -> emptyList()
      }
    }
  }

  private fun parseFile(file: File): Flowable<Dependency> {
    return Flowable.create({ emitter ->
      try {
        val parser = JavaParser()
        val parseResult = parser.parse(file)
        if (parseResult.isSuccessful) {
          val cu = parseResult.result.get()
          extractDependencies(cu, emitter)
        }
        emitter.onComplete()
      } catch (e: Exception) {
        println("Error analyzing file ${file.name}: ${e.message}")
        emitter.onComplete()
      }
    }, BackpressureStrategy.BUFFER)
  }

  private fun extractDependencies(cu: CompilationUnit, emitter: FlowableEmitter<Dependency>) {
    val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")

    cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
      val className = classDecl.nameAsString
      val fullClassName = if (packageName.isEmpty()) className else "$packageName.$className"

      val visitor = JavaDependencyVisitorImpl(fullClassName)
      visitor.visit(cu, null)

      visitor.dependencies.forEach { dependency ->
        if (!emitter.isCancelled) {
          emitter.onNext(dependency)
        }
      }
    }
  }
}
