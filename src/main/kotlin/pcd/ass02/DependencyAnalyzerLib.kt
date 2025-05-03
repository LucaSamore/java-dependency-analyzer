package pcd.ass02

import io.vertx.core.Future
import java.nio.file.Path

interface DependencyAnalyzerLib {
  fun getClassDependencies(classFile: Path): Future<ClassDepsReport>

  fun getPackageDependencies(packageFolder: Path): Future<PackageDepsReport>

  fun getProjectDependencies(projectFolder: Path): Future<ProjectDepsReport>
}

data class ClassDepsReport(val className: String, val usedTypes: Set<String>)

data class PackageDepsReport(val packageName: String, val classReports: List<ClassDepsReport>)

data class ProjectDepsReport(val projectName: String, val packageReports: List<PackageDepsReport>)
