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
        
        // Create Java file
        JavaFileObject javaFile = filer.createSourceFile(
                packageName + "." + className + "_MiniLombok", classElement);
        
        try (PrintWriter out = new PrintWriter(javaFile.openWriter())) {
            // Package
            if (!packageName.isEmpty()) {
                out.println("package " + packageName + ";");
                out.println();
            }
            
            // Class declaration
            out.println("/**");
            out.println(" * Generated code by MiniLombok for " + className);
            out.println(" */");
            out.println("public class " + className + "_MiniLombok {");
            
            // Generate methods
            for (GeneratedMethod method : methods) {
                switch (method.kind) {
                    case CONSTRUCTOR:
                        generateConstructor(out, classElement, method.fields);
                        break;
                    case GETTER:
                        for (VariableElement field : method.fields) {
                            generateGetter(out, classElement, field);
                        }
                        break;
                    case SETTER:
                        for (VariableElement field : method.fields) {
                            generateSetter(out, classElement, field);
                        }
                        break;
                    case TOSTRING:
                        generateToString(out, classElement, method.fields);
                        break;
                }
            }
            
            // Close class
            out.println("}");
        }
    }

    private void generateConstructor(PrintWriter out, TypeElement classElement, List<VariableElement> fields) {
        String className = classElement.getSimpleName().toString();
        
        out.println();
        out.println("    /**");
        out.println("     * Creates a new instance of " + className + " with all fields initialized.");
        out.println("     */");
        out.print("    public static " + className + " create(");
        
        // Constructor parameters
        for (int i = 0; i < fields.size(); i++) {
            VariableElement field = fields.get(i);
            TypeMirror fieldType = field.asType();
            String fieldName = field.getSimpleName().toString();
            
            out.print(fieldType + " " + fieldName);
            if (i < fields.size() - 1) {
                out.print(", ");
            }
        }
        out.println(") {");
        
        // Constructor body
        out.println("        " + className + " instance = new " + className + "();");
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            out.println("        instance." + fieldName + " = " + fieldName + ";");
        }
        out.println("        return instance;");
        out.println("    }");
    }

    private void generateGetter(PrintWriter out, TypeElement classElement, VariableElement field) {
        String className = classElement.getSimpleName().toString();
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String capitalizedFieldName = capitalize(fieldName);
        
        // Use "is" prefix for boolean fields
        String prefix = fieldType.getKind() == TypeKind.BOOLEAN ? "is" : "get";
        
        out.println();
        out.println("    /**");
        out.println("     * Gets the value of " + fieldName + ".");
        out.println("     */");
        out.println("    public static " + fieldType + " " + prefix + capitalizedFieldName + "(" + className + " instance) {");
        out.println("        return instance." + fieldName + ";");
        out.println("    }");
    }

    private void generateSetter(PrintWriter out, TypeElement classElement, VariableElement field) {
        String className = classElement.getSimpleName().toString();
        String fieldName = field.getSimpleName().toString();
        TypeMirror fieldType = field.asType();
        String capitalizedFieldName = capitalize(fieldName);
        
        out.println();
        out.println("    /**");
        out.println("     * Sets the value of " + fieldName + ".");
        out.println("     */");
        out.println("    public static void set" + capitalizedFieldName + "(" + className + " instance, " + fieldType + " value) {");
        out.println("        instance." + fieldName + " = value;");
        out.println("    }");
    }

    private void generateToString(PrintWriter out, TypeElement classElement, List<VariableElement> fields) {
        String className = classElement.getSimpleName().toString();
        
        out.println();
        out.println("    /**");
        out.println("     * Returns a string representation of the " + className + " instance.");
        out.println("     */");
        out.println("    public static String toString(" + className + " instance) {");
        out.println("        if (instance == null) return \"null\";");
        out.println("        StringBuilder sb = new StringBuilder(\"" + className + "{\");");
        
        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i).getSimpleName().toString();
            if (i > 0) {
                out.println("        sb.append(\", \");");
            }
            out.println("        sb.append(\"" + fieldName + "=\").append(String.valueOf(instance." + fieldName + "));");
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
