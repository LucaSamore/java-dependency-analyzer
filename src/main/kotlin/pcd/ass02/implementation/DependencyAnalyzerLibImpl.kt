package pcd.ass02.implementation

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import pcd.ass02.ClassDepsReport
import pcd.ass02.DependencyAnalyzerLib
import pcd.ass02.PackageDepsReport
import pcd.ass02.ProjectDepsReport

private typealias PackageName = String

private typealias Dependencies = Set<String>

internal class DependencyAnalyzerLibImpl(private val vertx: Vertx) : DependencyAnalyzerLib {

  override fun getClassDependencies(classFile: Path): Future<ClassDepsReport> {
    val promise = Promise.promise<ClassDepsReport>()
    readSourceFile(classFile)
        .compose(this::parseSourceCode)
        .compose(this::visitAST)
        .onFailure(promise::fail)
        .onSuccess {
          val (packageName, dependencies) = it
          val className = classFile.fileName.toString().removeSuffix(JAVA_EXTENSION)
          promise.complete(ClassDepsReport("${packageName}.${className}", dependencies))
        }
    return promise.future()
  }

  override fun getPackageDependencies(packageFolder: Path): Future<PackageDepsReport> {
    val promise = Promise.promise<PackageDepsReport>()
    vertx.fileSystem().readDir(packageFolder.toAbsolutePath().toString(), JAVA_FILES) { ar ->
      if (ar.succeeded()) {
        val javaFiles = ar.result().map { Path.of(it) }
        val futures = javaFiles.map { getClassDependencies(it) }
        Future.all(futures).onComplete { result ->
          if (result.succeeded()) {
            val reports = result.result().list<ClassDepsReport>()
            val packageName = packageFolder.fileName.toString()
            promise.complete(PackageDepsReport(packageName, reports))
          } else {
            promise.fail(result.cause())
          }
        }
      } else {
        promise.fail(ar.cause())
      }
    }
    return promise.future()
  }

  override fun getProjectDependencies(projectFolder: Path): Future<ProjectDepsReport> {
    val promise = Promise.promise<ProjectDepsReport>()
    getPackageFolders(projectFolder)
        .onSuccess { packageFolders ->
          val packageFutures = packageFolders.map { getPackageDependencies(it) }
          Future.all(packageFutures)
              .onSuccess { result ->
                val packageReports = result.list<PackageDepsReport>()
                promise.complete(ProjectDepsReport(packageReports))
              }
              .onFailure { err -> promise.fail(err) }
        }
        .onFailure { promise.fail(it) }
    return promise.future()
  }

  private fun readSourceFile(path: Path): Future<String> {
    return vertx.fileSystem().readFile(path.toString()).map { it.toString(ENCODING) }
  }

  private fun parseSourceCode(sourceCode: String): Future<CompilationUnit> {
    return vertx.executeBlocking(Callable { StaticJavaParser.parse(sourceCode) }, false)
  }

  private fun visitAST(compilationUnit: CompilationUnit): Future<Pair<PackageName, Dependencies>> {
    return vertx.executeBlocking(
        Callable {
          val usedTypes = mutableSetOf<String>()
          JavaDependencyVisitor().also { it.visit(compilationUnit, usedTypes) }
          val packageName =
              compilationUnit.packageDeclaration.map { it.nameAsString }.orElse(DEFAULT_PACKAGE)
          packageName to usedTypes
        })
  }

  private fun getPackageFolders(basePath: Path): Future<List<Path>> {
    return vertx.executeBlocking(
        Callable {
          Files.walk(basePath)
              .filter { Files.isDirectory(it) }
              .filter { dir ->
                Files.list(dir).anyMatch { file ->
                  Files.isRegularFile(file) && file.toString().endsWith(".java")
                }
              }
              .toList()
        },
        false)
  }

  companion object {
    private const val JAVA_FILES = ".*\\.java\$"
    private const val JAVA_EXTENSION = ".java"
    private const val ENCODING = "UTF-8"
    private const val DEFAULT_PACKAGE = "pcd.ass02"
  }
}
