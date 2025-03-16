package io.github.netanelshriki.minilombok.processor;

import com.google.auto.service.AutoService;
import io.github.netanelshriki.minilombok.annotations.AllArgsConstructor;
import io.github.netanelshriki.minilombok.annotations.Getter;
import io.github.netanelshriki.minilombok.annotations.Setter;
import io.github.netanelshriki.minilombok.annotations.ToString;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Annotation processor for Mini Lombok annotations.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "io.github.netanelshriki.minilombok.annotations.AllArgsConstructor",
        "io.github.netanelshriki.minilombok.annotations.Getter",
        "io.github.netanelshriki.minilombok.annotations.Setter",
        "io.github.netanelshriki.minilombok.annotations.ToString"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({"lombok.addGeneratedAnnotation"})
public class MiniLombokProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Map<TypeElement, List<GeneratedMethod>> generatedMethods = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Clear previously generated methods for new round
        generatedMethods.clear();

        try {
            // Collect all classes to process
            Set<TypeElement> classElements = new HashSet<>();
            collectClassesWithAnnotation(classElements, roundEnv, AllArgsConstructor.class);
            collectClassesWithAnnotation(classElements, roundEnv, Getter.class);
            collectClassesWithAnnotation(classElements, roundEnv, Setter.class);
            collectClassesWithAnnotation(classElements, roundEnv, ToString.class);

            // Process each class
            for (TypeElement classElement : classElements) {
                processClass(classElement, roundEnv);
            }

            // Generate code for all processed classes
            for (Map.Entry<TypeElement, List<GeneratedMethod>> entry : generatedMethods.entrySet()) {
                generateClassFile(entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Error processing annotations: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private <T extends java.lang.annotation.Annotation> void collectClassesWithAnnotation(
            Set<TypeElement> classes, RoundEnvironment roundEnv, Class<T> annotationClass) {
        // Add classes with the annotation
        for (Element element : roundEnv.getElementsAnnotatedWith(annotationClass)) {
            if (element.getKind() == ElementKind.CLASS) {
                classes.add((TypeElement) element);
            } else if (element.getKind() == ElementKind.FIELD) {
                // If the annotation is on a field, add the enclosing class
                Element enclosingElement = element.getEnclosingElement();
                if (enclosingElement.getKind() == ElementKind.CLASS) {
                    classes.add((TypeElement) enclosingElement);
                }
            }
        }
    }

    private void processClass(TypeElement classElement, RoundEnvironment roundEnv) {
        String className = classElement.getSimpleName().toString();
        
        // Get all fields (excluding static fields)
        List<VariableElement> fields = classElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .map(e -> (VariableElement) e)
                .collect(Collectors.toList());

        // Process annotations
        processAllArgsConstructor(classElement, fields);
        processGetters(classElement, fields, roundEnv);
        processSetters(classElement, fields, roundEnv);
        processToString(classElement, fields);
    }

    private void processAllArgsConstructor(TypeElement classElement, List<VariableElement> fields) {
        if (classElement.getAnnotation(AllArgsConstructor.class) != null) {
            GeneratedMethod constructorMethod = new GeneratedMethod();
            constructorMethod.kind = MethodKind.CONSTRUCTOR;
            constructorMethod.fields = fields;
            
            // Add to generated methods for this class
            generatedMethods.computeIfAbsent(classElement, k -> new ArrayList<>())
                    .add(constructorMethod);
        }
    }

    private void processGetters(TypeElement classElement, List<VariableElement> fields, RoundEnvironment roundEnv) {
        boolean classHasGetterAnnotation = classElement.getAnnotation(Getter.class) != null;
        
        for (VariableElement field : fields) {
            boolean fieldHasGetterAnnotation = field.getAnnotation(Getter.class) != null;
            
            if (classHasGetterAnnotation || fieldHasGetterAnnotation) {
                GeneratedMethod getterMethod = new GeneratedMethod();
                getterMethod.kind = MethodKind.GETTER;
                getterMethod.fields = Collections.singletonList(field);
                
                // Add to generated methods for this class
                generatedMethods.computeIfAbsent(classElement, k -> new ArrayList<>())
                        .add(getterMethod);
            }
        }
    }

    private void processSetters(TypeElement classElement, List<VariableElement> fields, RoundEnvironment roundEnv) {
        boolean classHasSetterAnnotation = classElement.getAnnotation(Setter.class) != null;
        
        for (VariableElement field : fields) {
            boolean fieldHasSetterAnnotation = field.getAnnotation(Setter.class) != null;
            
            if (classHasSetterAnnotation || fieldHasSetterAnnotation) {
                GeneratedMethod setterMethod = new GeneratedMethod();
                setterMethod.kind = MethodKind.SETTER;
                setterMethod.fields = Collections.singletonList(field);
                
                // Add to generated methods for this class
                generatedMethods.computeIfAbsent(classElement, k -> new ArrayList<>())
                        .add(setterMethod);
            }
        }
    }

    private void processToString(TypeElement classElement, List<VariableElement> fields) {
        if (classElement.getAnnotation(ToString.class) != null) {
            GeneratedMethod toStringMethod = new GeneratedMethod();
            toStringMethod.kind = MethodKind.TOSTRING;
            toStringMethod.fields = fields;
            
            // Add to generated methods for this class
            generatedMethods.computeIfAbsent(classElement, k -> new ArrayList<>())
                    .add(toStringMethod);
        }
    }

    private void generateClassFile(TypeElement classElement, List<GeneratedMethod> methods) throws IOException {
        String className = classElement.getSimpleName().toString();
        String packageName = elementUtils.getPackageOf(classElement).getQualifiedName().toString();
        
        // Create Java file directly for the annotated class instead of a companion class
        JavaFileObject javaFile = filer.createSourceFile(packageName + "." + className, classElement);
        
        // Original class content
        List<? extends Element> elements = classElement.getEnclosedElements();
        StringBuilder existingMethods = new StringBuilder();
        
        try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
            // Package
            if (!packageName.isEmpty()) {
                out.println("package " + packageName + ";");
                out.println();
            }
            
            // Imports for annotations
            out.println("// Generated by MiniLombok");
            out.println("import io.github.netanelshriki.minilombok.annotations.*;");
            out.println();
            
            // Get original annotations
            for (AnnotationMirror annotation : classElement.getAnnotationMirrors()) {
                out.println(annotation);
            }
            
            // Class declaration
            out.print("public class " + className);
            
            // Add superclass/interfaces
            TypeMirror superClass = classElement.getSuperclass();
            if (superClass != null && !superClass.toString().equals("java.lang.Object")) {
                out.print(" extends " + superClass);
            }
            
            List<? extends TypeMirror> interfaces = classElement.getInterfaces();
            if (!interfaces.isEmpty()) {
                out.print(" implements ");
                for (int i = 0; i < interfaces.size(); i++) {
                    if (i > 0) {
                        out.print(", ");
                    }
                    out.print(interfaces.get(i));
                }
            }
            
            out.println(" {");
            
            // Keep original fields
            for (Element element : elements) {
                if (element.getKind() == ElementKind.FIELD) {
                    String modifiers = element.getModifiers().stream()
                            .map(Modifier::toString)
                            .collect(Collectors.joining(" "));
                    String fieldName = element.getSimpleName().toString();
                    TypeMirror fieldType = element.asType();
                    
                    if (!modifiers.isEmpty()) {
                        out.print("    " + modifiers + " ");
                    } else {
                        out.print("    ");
                    }
                    out.println(fieldType + " " + fieldName + ";");
                }
            }
            out.println();
            
            // Keep original methods
            for (Element element : elements) {
                if (element.getKind() == ElementKind.METHOD || element.getKind() == ElementKind.CONSTRUCTOR) {
                    // Skip abstract methods for now - would need more complex handling
                    if (element.getModifiers().contains(Modifier.ABSTRACT)) {
                        continue;
                    }
                    
                    // We'd need the full method body here, but that's tough without a parser
                    // This is a simplified approach - in a real implementation you'd want 
                    // to preserve the original method bodies
                    if (element.getKind() == ElementKind.METHOD) {
                        ExecutableElement method = (ExecutableElement) element;
                        String methodName = method.getSimpleName().toString();
                        
                        // Skip methods we'll be generating
                        if (isGetterMethod(methodName) || isSetterMethod(methodName) || 
                            methodName.equals("toString") || methodName.equals("create")) {
                            continue;
                        }
                    }
                    
                    // For this demo, we'll just keep a stub of original methods
                    if (element.getKind() == ElementKind.METHOD) {
                        ExecutableElement method = (ExecutableElement) element;
                        String methodName = method.getSimpleName().toString();
                        TypeMirror returnType = method.getReturnType();
                        
                        out.print("    // Original method preserved: ");
                        out.print(returnType + " " + methodName + "(");
                        
                        List<? extends VariableElement> parameters = method.getParameters();
                        for (int i = 0; i < parameters.size(); i++) {
                            if (i > 0) {
                                out.print(", ");
                            }
                            VariableElement param = parameters.get(i);
                            out.print(param.asType() + " " + param.getSimpleName());
                        }
                        
                        out.println(") { /* original implementation */ }");
                    }
                }
            }
            
            // Add constructor if annotated
            for (GeneratedMethod method : methods) {
                if (method.kind == MethodKind.CONSTRUCTOR) {
                    generateConstructorMethod(out, classElement, method.fields);
                }
            }
            
            // Add getters
            for (GeneratedMethod method : methods) {
                if (method.kind == MethodKind.GETTER) {
                    for (VariableElement field : method.fields) {
                        generateGetterMethod(out, field);
                    }
                }
            }
            
            // Add setters
            for (GeneratedMethod method : methods) {
                if (method.kind == MethodKind.SETTER) {
                    for (VariableElement field : method.fields) {
                        generateSetterMethod(out, field);
                    }
                }
            }
            
            // Add toString
            for (GeneratedMethod method : methods) {
                if (method.kind == MethodKind.TOSTRING) {
                    generateToStringMethod(out, classElement, method.fields);
                }
            }
            
            // Close class
            out.println("}");
        }
    }

    private boolean isGetterMethod(String methodName) {
        return methodName.startsWith("get") || methodName.startsWith("is");
    }
    
    private boolean isSetterMethod(String methodName) {
        return methodName.startsWith("set");
    }

    private void generateConstructorMethod(PrintWriter out, TypeElement classElement, List<VariableElement> fields) {
        String className = classElement.getSimpleName().toString();
        
        out.println();
        out.println("    /**");
        out.println("     * Creates a new instance of " + className + " with all fields initialized.");
        out.println("     */");
        out.println("    public static " + className + " create(");
        
        // Constructor parameters
        for (int i = 0; i < fields.size(); i++) {
            VariableElement field = fields.get(i);
            TypeMirror fieldType = field.asType();
            String fieldName = field.getSimpleName().toString();
            
            out.print("            " + fieldType + " " + fieldName);
            if (i < fields.size() - 1) {
                out.println(",");
            } else {
                out.println(") {");
            }
        }
        
        // Constructor body
        out.println("        " + className + " instance = new " + className + "();");
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            out.println("        instance." + fieldName + " = " + fieldName + ";");
        }
        out.println("        return instance;");
        out.println("    }");
    }

    private void generateGetterMethod(PrintWriter out, VariableElement field) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String capitalizedFieldName = capitalize(fieldName);
        
        // Use "is" prefix for boolean fields
        String prefix = fieldType.getKind() == TypeKind.BOOLEAN ? "is" : "get";
        
        out.println();
        out.println("    /**");
        out.println("     * Gets the value of " + fieldName + ".");
        out.println("     */");
        out.println("    public " + fieldType + " " + prefix + capitalizedFieldName + "() {");
        out.println("        return this." + fieldName + ";");
        out.println("    }");
    }

    private void generateSetterMethod(PrintWriter out, VariableElement field) {
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String capitalizedFieldName = capitalize(fieldName);
        
        out.println();
        out.println("    /**");
        out.println("     * Sets the value of " + fieldName + ".");
        out.println("     */");
        out.println("    public void set" + capitalizedFieldName + "(" + fieldType + " " + fieldName + ") {");
        out.println("        this." + fieldName + " = " + fieldName + ";");
        out.println("    }");
    }

    private void generateToStringMethod(PrintWriter out, TypeElement classElement, List<VariableElement> fields) {
        String className = classElement.getSimpleName().toString();
        
        out.println();
        out.println("    /**");
        out.println("     * Returns a string representation of this " + className + " instance.");
        out.println("     */");
        out.println("    @Override");
        out.println("    public String toString() {");
        out.println("        StringBuilder sb = new StringBuilder(\"" + className + "{\");");
        
        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i).getSimpleName().toString();
            if (i > 0) {
                out.println("        sb.append(\", \");");
            }
            out.println("        sb.append(\"" + fieldName + "=\").append(String.valueOf(this." + fieldName + "));");
        }
        
        out.println("        sb.append(\"}\");");
        out.println("        return sb.toString();");
        out.println("    }");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // Utility classes to store generated methods
    private enum MethodKind {
        CONSTRUCTOR, GETTER, SETTER, TOSTRING
    }

    private static class GeneratedMethod {
        MethodKind kind;
        List<VariableElement> fields;
    }
}
