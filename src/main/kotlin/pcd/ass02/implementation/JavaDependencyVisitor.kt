package pcd.ass02.implementation

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

internal class JavaDependencyVisitor: VoidVisitorAdapter<Unit>() {

  val foundDependencies = mutableSetOf<String>()
  var currentPackage = ""

  override fun visit(cu: CompilationUnit, arg: Unit?) {
    cu.packageDeclaration.ifPresent {
      currentPackage = it.nameAsString
    }
    super.visit(cu, arg)
  }

  override fun visit(id: ImportDeclaration, arg: Unit?) {
    if (!id.isAsterisk) {
      foundDependencies.add(id.nameAsString)
    }
    super.visit(id, arg)
  }

  override fun visit(cd: ClassOrInterfaceDeclaration, arg: Unit?) {
    cd.extendedTypes.forEach { foundDependencies.add(it.nameAsString) }
    cd.implementedTypes.forEach { foundDependencies.add(it.nameAsString) }
    super.visit(cd, arg)
  }

  override fun visit(fd: FieldDeclaration, arg: Unit?) {
    addType(fd.elementType)
    super.visit(fd, arg)
  }

  override fun visit(md: MethodDeclaration, arg: Unit?) {
    addType(md.type)
    md.parameters.forEach { addType(it.type) }
    super.visit(md, arg)
  }

  override fun visit(vd: VariableDeclarator, arg: Unit?) {
    addType(vd.type)
    super.visit(vd, arg)
  }

  override fun visit(oce: ObjectCreationExpr, arg: Unit?) {
    val typeName = oce.type.let {
      it.scope.map { s -> "${s.nameAsString}.${it.nameAsString}" }
        .orElse(it.nameAsString)
    }
    foundDependencies.add(typeName)
    super.visit(oce, arg)
  }

  override fun visit(tp: TypeParameter, arg: Unit?) {
    foundDependencies.add(tp.nameAsString)
    tp.typeBound.forEach { addType(it) }
    super.visit(tp, arg)
  }

  private fun addType(type: Type) {
    when {
      type.isPrimitiveType -> return

      type.isArrayType -> {
        addType(type.asArrayType().componentType)
      }

      type.isClassOrInterfaceType -> {
        val coit = type.asClassOrInterfaceType()
        foundDependencies.add(coit.nameWithScope)
        coit.typeArguments.ifPresent { args ->
          args.forEach { addType(it) }
        }
      }

      type.isWildcardType -> {
        type.asWildcardType().extendedType.ifPresent { addType(it) }
      }

      type.isUnionType -> {
        type.asUnionType().elements.forEach { addType(it) }
      }

      type.isIntersectionType -> {
        type.asIntersectionType().elements.forEach { addType(it) }
      }

      else -> {
        foundDependencies.add(type.asString())
      }
    }
  }
}
