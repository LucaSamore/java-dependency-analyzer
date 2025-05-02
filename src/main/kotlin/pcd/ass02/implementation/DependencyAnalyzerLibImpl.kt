package pcd.ass02.implementation

import com.github.javaparser.StaticJavaParser
import io.vertx.core.Future
import io.vertx.core.Vertx
import java.io.File
import pcd.ass02.ClassDepsReport
import pcd.ass02.DependencyAnalyzerLib
import pcd.ass02.PackageDepsReport
import pcd.ass02.ProjectDepsReport
import java.util.concurrent.Callable

internal class DependencyAnalyzerLibImpl(private val vertx: Vertx) : DependencyAnalyzerLib {

  override fun getClassDependencies(classFile: File): Future<ClassDepsReport> {
    return vertx.executeBlocking(Callable {
      val compilationUnit = StaticJavaParser.parse(classFile)
      val visitor = JavaDependencyVisitor()
      visitor.visit(compilationUnit, null)
      ClassDepsReport(
        className = compilationUnit.primaryTypeName.orElse("Unknown"),
        usedTypes = visitor.foundDependencies
      )
    }, false)
  }

  override fun getPackageDependencies(packageFolder: File): Future<PackageDepsReport> {
    TODO("Not yet implemented")
  }

  override fun getProjectDependencies(projectFolder: File): Future<ProjectDepsReport> {
    TODO("Not yet implemented")
  }
}
