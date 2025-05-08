package pcd.ass02.common.visitor

import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

abstract class JavaParserVisitorBase<T> : VoidVisitorAdapter<T>() {

  override fun visit(n: ImportDeclaration, arg: T) {
    super.visit(n, arg)
    val importName = n.nameAsString

    if (!importName.startsWith("java.lang.")) {
      handleImport(importName, n.isAsterisk)
    }
  }

  override fun visit(n: ClassOrInterfaceDeclaration, arg: T) {
    n.extendedTypes.forEach { extendedType ->
      handleExtends(extendedType.nameAsString)
    }

    n.implementedTypes.forEach { implementedType ->
      handleImplements(implementedType.nameAsString)
    }

    super.visit(n, arg)
  }

  override fun visit(n: VariableDeclarator, arg: T) {
    super.visit(n, arg)
    handleVariableType(n.type.asString())
  }

  override fun visit(n: FieldDeclaration, arg: T) {
    super.visit(n, arg)
    handleFieldType(n.elementType.asString())
  }

  override fun visit(n: ObjectCreationExpr, arg: T) {
    super.visit(n, arg)
    handleObjectCreation(n.type.nameAsString)
  }

  override fun visit(n: MethodDeclaration, arg: T) {
    if (!n.type.isVoidType) {
      handleReturnType(n.type.asString())
    }

    for (param in n.parameters) {
      handleParameterType(param.type.asString())
    }

    super.visit(n, arg)
  }

  protected abstract fun handleImport(importName: String, isAsterisk: Boolean)
  protected abstract fun handleExtends(typeName: String)
  protected abstract fun handleImplements(typeName: String)
  protected abstract fun handleVariableType(typeName: String)
  protected abstract fun handleFieldType(typeName: String)
  protected abstract fun handleObjectCreation(typeName: String)
  protected abstract fun handleReturnType(typeName: String)
  protected abstract fun handleParameterType(typeName: String)

  protected fun getPackageFromImport(importName: String): String {
    val lastDotIndex = importName.lastIndexOf('.')
    return if (lastDotIndex > 0) importName.substring(0, lastDotIndex) else ""
  }
}
