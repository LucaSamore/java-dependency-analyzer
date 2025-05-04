package pcd.ass02.implementation

import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

internal class JavaDependencyVisitor : VoidVisitorAdapter<MutableSet<String>>() {

  override fun visit(n: ClassOrInterfaceDeclaration?, arg: MutableSet<String>?) {
    super.visit(n, arg)
    n?.extendedTypes?.forEach { arg?.add(it.nameAsString) }
    n?.implementedTypes?.forEach { arg?.add(it.nameAsString) }
  }

  override fun visit(n: PackageDeclaration?, arg: MutableSet<String>?) {
    super.visit(n, arg)
    arg?.add(n?.nameAsString ?: DEFAULT)
  }

  override fun visit(n: FieldDeclaration?, arg: MutableSet<String>?) {
    super.visit(n, arg)
    n?.variables?.forEach { arg?.add(it.type.asString()) }
  }

  override fun visit(n: MethodDeclaration?, arg: MutableSet<String>?) {
    super.visit(n, arg)
    n?.parameters?.forEach { arg?.add(it.nameAsString) } // params type
    arg?.add(n?.type?.asString() ?: DEFAULT) // return type
  }

  companion object {
    const val DEFAULT = "Unknown"
  }
}
