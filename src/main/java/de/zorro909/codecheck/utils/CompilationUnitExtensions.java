package de.zorro909.codecheck.utils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import de.zorro909.codecheck.checks.ValidationError;

import java.util.stream.Stream;

public class CompilationUnitExtensions {

    public static Stream<ClassOrInterfaceDeclaration> findAllClassesWithEnds(
            CompilationUnit compilationUnit, String endsWith) {
        return compilationUnit.findAll(ClassOrInterfaceDeclaration.class,
                                       decl -> decl.getNameAsString().endsWith(endsWith)).stream();
    }

    public static ValidationError validationError(CompilationUnit cu, NodeWithRange node,
                                                  ValidationError.Severity severity, String text) {
        return new ValidationError(cu.getStorage().orElseThrow().getPath(), text, node.getBegin(),
                                   severity);
    }


}
