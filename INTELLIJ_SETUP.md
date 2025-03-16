# Setting up Mini-Lombok with IntelliJ IDEA

This guide explains how to set up the Mini-Lombok library in IntelliJ IDEA to ensure proper code completion and error detection.

## Installation Steps

### 1. Add Mini-Lombok to your project

Add the dependency to your `build.gradle` file:

```gradle
dependencies {
    implementation 'io.github.netanelshriki:mini-lombok:0.1.0'
    annotationProcessor 'io.github.netanelshriki:mini-lombok:0.1.0'
}
```

Or in Maven (`pom.xml`):

```xml
<dependency>
    <groupId>io.github.netanelshriki</groupId>
    <artifactId>mini-lombok</artifactId>
    <version>0.1.0</version>
</dependency>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.github.netanelshriki</groupId>
                        <artifactId>mini-lombok</artifactId>
                        <version>0.1.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. Enable Annotation Processing in IntelliJ

1. Go to **File** > **Settings** (or **IntelliJ IDEA** > **Preferences** on macOS)
2. Navigate to **Build, Execution, Deployment** > **Compiler** > **Annotation Processors**
3. Check the box for **Enable annotation processing**
4. Set the processor path to **Classpath of module**, which will use your project's dependencies
5. Click **Apply** and **OK**

### 3. Configure Build Process

To ensure IntelliJ processes annotations correctly:

1. Go to **File** > **Settings** (or **IntelliJ IDEA** > **Preferences** on macOS)
2. Navigate to **Build, Execution, Deployment** > **Build Tools**
    - For Gradle projects: Go to **Gradle** and ensure "Build and run using" is set to "IntelliJ IDEA"
    - For Maven projects: Go to **Maven** and enable "Always update snapshots"
3. Click **Apply** and **OK**

## Using Mini-Lombok

### Basic Usage

1. Import the annotations:
   ```java
   import io.github.netanelshriki.minilombok.annotations.AllArgsConstructor;
   import io.github.netanelshriki.minilombok.annotations.Getter;
   import io.github.netanelshriki.minilombok.annotations.Setter;
   import io.github.netanelshriki.minilombok.annotations.ToString;
   ```

2. Apply annotations to your classes:
   ```java
   @AllArgsConstructor
   @Getter
   @Setter
   @ToString
   public class Person {
       private String name;
       private int age;
   }
   ```

3. Use the generated methods:
   ```java
   Person person = Person.create("John", 30);
   String name = person.getName();
   person.setAge(31);
   System.out.println(person); // ToString implementation
   ```

### Troubleshooting

If IntelliJ shows errors for generated code:

1. **Rebuild the project**: Go to **Build** > **Rebuild Project**
2. **Clean the project**: Go to **Build** > **Clean Project**
3. **Invalidate caches**: Go to **File** > **Invalidate Caches / Restart**
4. Ensure your annotation processor is correctly configured in your build setup

### Notes for Future Versions

In future versions, we might support IntelliJ-specific features like:

1. Adding a proper IntelliJ plugin to integrate better with the IDE
2. Supporting incremental annotation processing for faster builds
3. Adding IDE-specific hints about generated code

## Comparing with Lombok

Unlike the original Lombok:

1. Mini-Lombok uses a more traditional annotation processing approach without bytecode manipulation
2. Generated code is more transparent and directly visible
3. No need for special IDE plugins (though setup is still required)
4. More limited feature set (focused on the core functionality)
