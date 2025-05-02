package pcd.ass02

import io.vertx.core.Future
import java.io.File

interface DependencyAnalyzerLib {

  fun getClassDependencies(classFile: File): Future<ClassDepsReport>

  fun getPackageDependencies(packageFolder: File): Future<PackageDepsReport>

  fun getProjectDependencies(projectFolder: File): Future<ProjectDepsReport>
}

data class ClassDepsReport(val className: String, val usedTypes: Set<String>)

data class PackageDepsReport(val packageName: String, val classReports: List<ClassDepsReport>)

data class ProjectDepsReport(val packageReports: List<PackageDepsReport>)
