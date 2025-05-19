package pcd.ass02.reactive.implementation

import pcd.ass02.reactive.JavaParserVisitorBase
import pcd.ass02.reactive.model.Dependency

class JavaDependencyVisitor(private val className: String) : JavaParserVisitorBase<Void?>() {

  private val dependencyMap = mutableMapOf<Pair<String, String>, Dependency>()

  val dependencies: List<Dependency>
    get() = dependencyMap.values.toList()

  override fun handleImport(importName: String, isAsterisk: Boolean) {
    if (isAsterisk) {
      addDependency(importName, "package import")
    } else {
      val packageName = getPackageFromImport(importName)
      addDependency(importName, "type import from package: $packageName")
    }
  }

  override fun handleExtends(typeName: String) {
    addDependency(typeName, "extends")
  }

  override fun handleImplements(typeName: String) {
    addDependency(typeName, "implements")
  }

  override fun handleVariableType(typeName: String) {
    addDependency(typeName, "variable")
  }

  override fun handleFieldType(typeName: String) {
    addDependency(typeName, "field")
  }

  override fun handleObjectCreation(typeName: String) {
    addDependency(typeName, "creation")
  }

  override fun handleReturnType(typeName: String) {
    addDependency(typeName, "return type")
  }

  override fun handleParameterType(typeName: String) {
    addDependency(typeName, "parameter")
  }

  private fun addDependency(targetClass: String, type: String) {
    val key = Pair(targetClass, type)
    if (key !in dependencyMap) {
      dependencyMap[key] = Dependency(className, targetClass, type)
    }
  }
}
