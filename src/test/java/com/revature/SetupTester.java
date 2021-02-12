package com.revature;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.*;

public class SetupTester {
    private static Properties props = new Properties();

    @BeforeClass
    public static void setupVariables() {
        try {
            props.load(new FileReader("src/main/resources/application.properties"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void printFinished() {
        System.out.println("All tests finished.");
    }

    @After
    public void teardown() {
        Setup.close();
    }

    @Test
    public void testOpen_withValidInputs_andNoSchema() {
        try {
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"));
        } catch (SQLException e) {
            // Shouldn't get here
            assertTrue(false);
        }
        assertNotNull(Setup.getConnection());
    }

    @Test
    public void testOpen_withInvalidInputs_andNoSchema() {
        try {
            Setup.open(props.getProperty("url"), "invalidUsername", "invalidPassword");
            // Should throw an error and never get here
            assertTrue(false);
        } catch (SQLException e) {
            // Should enter here
        }
        assertNull(Setup.getConnection());
    }

    @Test
    public void testOpen_withValidInputs_andSchema() {
        try {
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"), props.getProperty("schema"));
            assertNotNull(Setup.getConnection());
            assertEquals(props.getProperty("schema"), Setup.getConnection().getSchema());
        } catch (SQLException e) {
            // Shouldn't get here
            assertTrue(false);
        }
    }
    
    @Test
    public void testOpen_withValidInputs_andInvalidSchema() {
        try {
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"), "invalidSchema");
            // Shouldn't get here
            assertTrue(false);
        } catch (SQLException e) {
            // Should enter here
        }
        assertNull(Setup.getConnection());
    }

    @Test
    public void testOpen_withPreexistingConnection() {
        try {
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"));
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"), props.getProperty("schema"));
        } catch (SQLException e) {
            // Shouldn't throw an exception
            assertTrue(false);
        }
        assertNotNull(Setup.getConnection());
    }

    @Test
    public void testClose_withOpenConnection() {
        
        try {
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"));
            assertNotNull(Setup.getConnection());
            Setup.close();
            assertTrue(Setup.getConnection().isClosed());
        } catch (SQLException e) {
            // Shouldn't throw an exception
            assertTrue(false);
        }
    }

    @Test
    public void testOpen_afterClose() {
        
        try {
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"));
            assertNotNull(Setup.getConnection());
            Setup.close();
            assertTrue(Setup.getConnection().isClosed());
            Setup.open(props.getProperty("url"), props.getProperty("username"), props.getProperty("password"), props.getProperty("schema"));
            assertFalse(Setup.getConnection().isClosed());
        } catch (SQLException e) {
            // Shouldn't throw an exception
            assertTrue(false);
        }
    }
}
