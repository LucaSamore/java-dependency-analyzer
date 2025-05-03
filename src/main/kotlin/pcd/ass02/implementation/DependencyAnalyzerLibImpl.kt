package pcd.ass02.implementation

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import java.nio.file.Path
import java.util.concurrent.Callable
import pcd.ass02.ClassDepsReport
import pcd.ass02.DependencyAnalyzerLib
import pcd.ass02.PackageDepsReport
import pcd.ass02.ProjectDepsReport

internal class DependencyAnalyzerLibImpl(private val vertx: Vertx) : DependencyAnalyzerLib {

  override fun getClassDependencies(classFile: Path): Future<ClassDepsReport> {
    val promise = Promise.promise<ClassDepsReport>()
    readSourceFile(classFile)
        .compose(this::parseSourceCode)
        .compose(this::visitAST)
        .onFailure(promise::fail)
        .onSuccess { TODO("Build ClassDepsReport") }
    return promise.future()
  }

  override fun getPackageDependencies(packageFolder: Path): Future<PackageDepsReport> {
    val promise = Promise.promise<PackageDepsReport>()
    vertx.fileSystem().readDir(packageFolder.toAbsolutePath().toString(), "*.java") { ar ->
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
    TODO("Remember that vertx readDir does not perform a recursive read")
  }

  private fun readSourceFile(path: Path): Future<String> {
    return vertx.fileSystem().readFile(path.toString()).map { it.toString("UTF-8") }
  }

  private fun parseSourceCode(sourceCode: String): Future<CompilationUnit> {
    return vertx.executeBlocking(Callable { StaticJavaParser.parse(sourceCode) }, false)
  }

  private fun visitAST(compilationUnit: CompilationUnit): Future<String> {
    return vertx.executeBlocking(Callable { TODO("Get dependencies") })
  }
}
