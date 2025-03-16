package io.github.netanelshriki.minilombok.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Mini Lombok functionality.
 */
public class MiniLombokTest {

    @Test
    public void testAllArgsConstructor() {
        // Create an instance using the generated constructor
        TestModel model = TestModel.create("John Doe", 30, true);
        
        assertEquals("John Doe", model.getName());
        assertEquals(30, model.getAge());
        assertTrue(model.isActive());
    }
    
    @Test
    public void testGetters() {
        // Create a model and set fields directly
        TestModel model = new TestModel();
        model.name = "Jane Doe";
        model.age = 25;
        model.active = false;
        
        // Use getters to verify values
        assertEquals("Jane Doe", model.getName());
        assertEquals(25, model.getAge());
        assertFalse(model.isActive());
    }
    
    @Test
    public void testSetters() {
        // Create a model
        TestModel model = new TestModel();
        
        // Use setters to set values
        model.setName("Alice");
        model.setAge(28);
        model.setActive(true);
        
        // Verify values directly
        assertEquals("Alice", model.name);
        assertEquals(28, model.age);
        assertTrue(model.active);
    }
    
    @Test
    public void testToString() {
        // Create a model
        TestModel model = new TestModel();
        model.name = "Bob";
        model.age = 35;
        model.active = true;
        
        // Verify toString output
        String expected = "TestModel{name=Bob, age=35, active=true}";
        assertEquals(expected, model.toString());
    }
}
