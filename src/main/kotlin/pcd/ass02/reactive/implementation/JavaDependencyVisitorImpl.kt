package pcd.ass02.reactive.implementation

import pcd.ass02.common.visitor.JavaParserVisitorBase
import pcd.ass02.reactive.model.Dependency

class JavaDependencyVisitor(private val className: String) : JavaParserVisitorBase<Void?>() {

  val dependencies = mutableListOf<Dependency>()

  override fun handleImport(importName: String, isAsterisk: Boolean) {
    if (isAsterisk) {
      dependencies.add(Dependency(className, importName, "package import"))
    } else {
      val packageName = getPackageFromImport(importName)
      dependencies.add(Dependency(className, importName, "type import from package: $packageName"))
    }
  }

  override fun handleExtends(typeName: String) {
    dependencies.add(Dependency(className, typeName, "extends"))
  }

  override fun handleImplements(typeName: String) {
    dependencies.add(Dependency(className, typeName, "implements"))
  }

  override fun handleVariableType(typeName: String) {
    dependencies.add(Dependency(className, typeName, "variable"))
  }

  override fun handleFieldType(typeName: String) {
    dependencies.add(Dependency(className, typeName, "field"))
  }

  override fun handleObjectCreation(typeName: String) {
    dependencies.add(Dependency(className, typeName, "creation"))
  }

  override fun handleReturnType(typeName: String) {
    dependencies.add(Dependency(className, typeName, "return type"))
  }

  override fun handleParameterType(typeName: String) {
    dependencies.add(Dependency(className, typeName, "parameter"))
  }
}
