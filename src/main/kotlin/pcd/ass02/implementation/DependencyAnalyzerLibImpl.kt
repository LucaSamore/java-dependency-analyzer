package pcd.ass02.implementation

import io.vertx.core.Future
import io.vertx.core.Vertx
import java.io.File
import pcd.ass02.ClassDepsReport
import pcd.ass02.DependencyAnalyzerLib
import pcd.ass02.PackageDepsReport
import pcd.ass02.ProjectDepsReport

internal class DependencyAnalyzerLibImpl(private val vertx: Vertx) : DependencyAnalyzerLib {

  override fun getClassDependencies(classFile: File): Future<ClassDepsReport> {
    TODO("Not yet implemented")
  }

  override fun getPackageDependencies(packageFolder: File): Future<PackageDepsReport> {
    TODO("Not yet implemented")
  }

  override fun getProjectDependencies(projectFolder: File): Future<ProjectDepsReport> {
    TODO("Not yet implemented")
  }
}
