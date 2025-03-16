# Setting up Mini-Lombok in IntelliJ IDEA

This guide will help you set up Mini-Lombok in your IntelliJ IDEA projects.

## Prerequisites

- IntelliJ IDEA (Community or Ultimate edition)
- Java 8 or higher
- Gradle or Maven build system

## Installation Steps

### 1. Add Mini-Lombok to your project

#### For Gradle projects:

Add the following to your `build.gradle` file:

```groovy
repositories {
    mavenCentral()
    // Add Maven repository where the library is hosted, or use local Maven
    mavenLocal()
}

dependencies {
    implementation 'io.github.netanelshriki:mini-lombok:0.1.0'
    annotationProcessor 'io.github.netanelshriki:mini-lombok:0.1.0'
}
```

#### For Maven projects:

Add the following to your `pom.xml` file:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.netanelshriki</groupId>
        <artifactId>mini-lombok</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
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

### 2. Configure IntelliJ IDEA

#### Enable Annotation Processing

1. Open your project in IntelliJ IDEA
2. Go to **File** > **Settings** (or IntelliJ IDEA > Preferences on macOS)
3. Navigate to **Build, Execution, Deployment** > **Compiler** > **Annotation Processors**
4. Check the box for **Enable annotation processing**
5. Select **Obtain processors from project classpath**
6. Set the **Store generated sources relative to:** to **Module content root**
7. Click **Apply** and **OK**

#### Configure IntelliJ to recognize generated code

1. Refresh your Gradle or Maven project
2. Go to **File** > **Project Structure**
3. Select **Modules** on the left
4. Select your module
5. Go to the **Sources** tab
6. Find the directory where generated sources are stored (typically `build/generated/sources/annotationProcessor` for Gradle)
7. Mark it as **Generated Sources Root** by right-clicking on it and selecting the option

### 3. Using Mini-Lombok in your code

After setting up Mini-Lombok, you can use it in your code as follows:

```java
import io.github.netanelshriki.minilombok.annotations.AllArgsConstructor;
import io.github.netanelshriki.minilombok.annotations.Getter;
import io.github.netanelshriki.minilombok.annotations.Setter;
import io.github.netanelshriki.minilombok.annotations.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class Person {
    private String name;
    private int age;
    private boolean active;
}
```

Then in your code, you can use the generated utility class:

```java
// Create an instance
Person person = Person_MiniLombok.create("John Doe", 30, true);

// Get values
String name = Person_MiniLombok.getName(person);
int age = Person_MiniLombok.getAge(person);
boolean active = Person_MiniLombok.isActive(person);

// Set values
Person_MiniLombok.setName(person, "Jane Doe");
Person_MiniLombok.setAge(person, 28);
Person_MiniLombok.setActive(person, false);

// ToString
String personString = Person_MiniLombok.toString(person);
```

## Troubleshooting

### Generated code not found

If IntelliJ doesn't recognize the generated code:

1. Make sure annotation processing is enabled
2. Rebuild the project
3. Refresh your Gradle/Maven project
4. Make sure the generated sources directory is marked as a source root

### Cannot find symbol errors

If you get "Cannot find symbol" errors for the generated classes:

1. Check if the annotation processor is correctly configured
2. Check if your IDE has properly indexed the generated sources
3. Try invalidating caches and restarting IntelliJ (File > Invalidate Caches / Restart)
