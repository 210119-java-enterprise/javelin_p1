package com.revature;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import com.revature.exceptions.InvalidColumnsException;
import com.revature.exceptions.TypeMismatchException;

import org.junit.*;

public class ModelTester {
    private Model child;

    @Before
    public void setup() {
        child = new ModelExtension();
        Properties props = new Properties();
        try {
            props.load(new FileReader("src/main/resources/application.properties"));
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"));
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @After
    public void teardown() {
        Setup.close();
    }

    @AfterClass
    public static void printFinished() {
        System.out.println("All tests finished.");
    }

    @Test
    public void testTableName_withNoAnnotation() {
        String expectedTableName = "ModelExtension";
        assertEquals(expectedTableName, child.getTableName());
    }

    @Test
    public void testTableName_withAnnotation() {
        String expectedTableName = "Test";
        child = new ModelExtensionWithAnnotation();
        assertEquals(expectedTableName, child.getTableName());
    }

    @Test
    public void testColumnsEmpty_withNewModel() {
        assertTrue(child.getFieldsAndValues().isEmpty());
    }

    @Test
    public void testSetColumns_withValidColumns() {
        String columnName0 = "valid_column_name";
        String columnName1 = "valid_column_name_2";
        int intValue = 0;
        String stringValue = "valid_value";

        child.setColumn(columnName0, intValue);
        child.setColumn(columnName1, stringValue);
        assertFalse(child.getFieldsAndValues().isEmpty());
        assertEquals(intValue, child.get(columnName0));
        assertEquals(stringValue, child.get(columnName1));
    }

    @Test
    public void testSetColumns_WithValidOverwrite() {
        String columnName = "column_name";
        String tempValue = "hi";
        String newValue = "hello there";

        child.setColumn(columnName, tempValue);
        child.setColumn(columnName, newValue);
        assertEquals(1, child.getFieldsAndValues().size());
        assertEquals(newValue, child.get(columnName));
    }

    @Test
    public void testSetColumns_withInvalidOverwrite() {
        String columnName = "column_name";
        String oldValue = "hi";
        int newValue = 0;

        try {
            child.setColumn(columnName, oldValue);
            // Should throw a TypeMismatchException
            child.setColumn(columnName, newValue);
            assertTrue(false);
        } catch (TypeMismatchException e) {
            // Yay
        }
        assertEquals(1, child.getFieldsAndValues().size());
        assertEquals(oldValue, child.get(columnName));
    }

    @Test
    public void testSetColumn_withMethodChaining() {
        String columnName0 = "valid_column_name";
        String columnName1 = "valid_column_name_2";
        int intValue = 0;
        String stringValue = "valid_value";
        
        child.setColumn(columnName0, intValue)
                .setColumn(columnName1, stringValue);

        assertFalse(child.getFieldsAndValues().isEmpty());
        assertEquals(intValue, child.get(columnName0));
        assertEquals(stringValue, child.get(columnName1));
    }

    @Test
    public void testChangeColumn_withSameTypes() {
        String columnName = "string_column";
        String oldValue = "Hello there.";
        String newValue = "General Kenobi!";

        child.changeColumn(columnName, oldValue)
                .changeColumn(columnName, newValue);
        
        assertFalse(child.getFieldsAndValues().isEmpty());
        assertEquals(1, child.getFieldsAndValues().size());
        assertEquals(newValue, child.get(columnName));
    }

    @Test
    public void testChangeColumn_withDifferentTypes() {
        String columnName = "string_column?";
        int intValue = 0;
        String stringValue = "hi";
        child.changeColumn(columnName, stringValue)
                .changeColumn(columnName, intValue);
        
        assertFalse(child.getFieldsAndValues().isEmpty());
        assertEquals(1, child.getFieldsAndValues().size());
        assertEquals(intValue, child.get(columnName));
    }

    @Test
    public void testCreate_withEmptyModel() {
        try {
            // Should throw an InvalidColumnsException
            child.create(child.getClass());
            assertTrue(false);
        } catch (InvalidColumnsException e) {
            // Yay!
        }
    }

    // TODO:
    @Test
    public void testCreate_withColumnsInModel() {
        String column0 = "string_column";
        String column1 = "int_column";
        String value0 = "Hello there.";
        int value1 = 0;
        child.setColumn(column0, value0)
                .setColumn(column1, value1);
        child.create(child.getClass());
    }
}
