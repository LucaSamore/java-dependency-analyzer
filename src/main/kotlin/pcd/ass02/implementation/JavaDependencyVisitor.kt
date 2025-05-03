package pcd.ass02.implementation

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

internal class JavaDependencyVisitor : VoidVisitorAdapter<MutableSet<String>>() {

  // TODO: add more visits

  override fun visit(n: ClassOrInterfaceDeclaration?, arg: MutableSet<String>?) {
    super.visit(n, arg)
    n?.extendedTypes?.forEach { arg?.add(it.nameAsString) }
    n?.implementedTypes?.forEach { arg?.add(it.nameAsString) }
  }
}
