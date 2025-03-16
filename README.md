# Mini Lombok

A lightweight Java library that provides Lombok-like functionality with annotations for constructors, getters, setters, and toString methods.

## Features

- `@AllArgsConstructor`: Generates a constructor with all fields
- `@Getter`: Generates getter methods for fields
- `@Setter`: Generates setter methods for fields
- `@ToString`: Generates a toString method

## Quick Start

### 1. Add to your project

#### Gradle

```gradle
dependencies {
    implementation 'io.github.netanelshriki:mini-lombok:0.1.0'
    annotationProcessor 'io.github.netanelshriki:mini-lombok:0.1.0'
}
```

#### Maven

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

### 2. Use in your classes

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

### 3. Use the generated code

```java
// Create a new instance
Person person = Person.create("John Doe", 30, true);

// Use getters
String name = person.getName();
int age = person.getAge();
boolean active = person.isActive();

// Use setters
person.setName("Jane Doe");
person.setAge(31);
person.setActive(false);

// Use toString
System.out.println(person); // Person{name=Jane Doe, age=31, active=false}
```

## IntelliJ IDEA Setup

For detailed instructions on setting up Mini-Lombok with IntelliJ IDEA, please see the [IntelliJ Setup Guide](INTELLIJ_SETUP.md).

## How It Works

Mini-Lombok uses standard Java annotation processing to generate code at compile time. Unlike the full Lombok library, Mini-Lombok:

1. Generates code directly in the annotated class
2. Uses a standard approach compatible with all Java IDEs
3. Creates easy-to-understand and debug code
4. Requires no special IDE plugins

## License

MIT License
