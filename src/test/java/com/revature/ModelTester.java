package com.revature;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.revature.exceptions.InvalidColumnsException;
import com.revature.exceptions.ResourcePersistenceException;
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
            
            String sql = "DROP TABLE IF EXISTS ModelExtension";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
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
            child.create(ModelExtension.class);
            assertTrue(false);
        } catch (InvalidColumnsException e) {
            // Yay!
        }
    }

    @Test
    public void testCreate_withColumnsInModel_andPreExistingTable() {
        String column0 = "string_column";
        String column1 = "int_column";
        String value0 = "Hello there.";
        int value1 = 0;
        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " varchar(25), " +
            column1 + " int);commit";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        child.setColumn(column0, value0)
                .setColumn(column1, value1);
        child.create(ModelExtension.class);

        try {
            String sql = "SELECT * FROM ModelExtension";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            assertEquals(value0, rs.getString(column0));
            assertEquals(value1, rs.getInt(column1));
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    @Test
    public void testCreate_withColumnsInModel_andNoExistingTable() {
        String column0 = "string_column";
        String column1 = "int_column";
        String value0 = "Hello there.";
        int value1 = 0;

        try {
            child.setColumn(column0, value0)
                    .setColumn(column1, value1);
            child.create(ModelExtension.class);
            assertTrue(false);
        } catch (ResourcePersistenceException e) {
            
        }
    }

    @Test
    public void testCreate_withNoColumnsInModel() {
        try {
            child.create(ModelExtension.class);
            assertTrue(false);
        } catch (InvalidColumnsException e) {

        }
    }

    @Test
    public void testFindAll_withExecute_andNoObjectsInTable() {
        String column0 = "string_column";
        String column1 = "int_column";
        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " varchar(25), " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAll().execute(ModelExtension.class);

        assertEquals(0, models.size());
    }

    @Test
    public void testFindAll_withExecute_andOneObjectInTable() {
        String column0 = "string_column";
        String column1 = "int_column";
        String stringValue = "Hello there.";
        int intValue = 0;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " varchar(25), " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            stringValue + "', " +
            intValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAll().execute(ModelExtension.class);

        assertEquals(1, models.size());
        assertEquals(stringValue, models.get(0).get(column0));
        assertEquals(intValue, models.get(0).get(column1));
        assertEquals(stringValue, child.get(column0));
        assertEquals(intValue, child.get(column1));
    }

    @Test
    public void testFindAll_withExecute_andTwoObjectsInTable() {
        String column0 = "string_column";
        String column1 = "int_column";
        String stringValue0 = "Hello there.";
        String stringValue1 = "General Kenobi!";
        int intValue0 = 0;
        int intValue1 = 100;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " varchar(25), " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            stringValue0 + "', " +
            intValue0 + "), ('" +
            stringValue1 + "', " +
            intValue1 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        
        List<ModelExtension> models = child.findAll().execute(ModelExtension.class);

        assertEquals(2, models.size());
        assertEquals(stringValue0, models.get(0).get(column0));
        assertEquals(intValue0, models.get(0).get(column1));
        assertEquals(stringValue1, models.get(1).get(column0));
        assertEquals(intValue1, models.get(1).get(column1));
        assertEquals(new HashMap<String, Object>(), child.getFieldsAndValues());
        
    }

    @Test
    public void testFindById_withExecute_andNoObjectsInTable() {
        String column0 = "user_id";
        String column1 = "age";
        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAllById(column0, 0).execute(ModelExtension.class);
        assertEquals(0, models.size());
    }

    @Test
    public void testFindById_withExecute_andOneObjectInTable_andValidIdColumn() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            idValue + "', " +
            ageValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAllById(column0, 0).execute(ModelExtension.class);
        
        assertEquals(1, models.size());
        assertEquals(idValue, models.get(0).get(column0));
        assertEquals(ageValue, models.get(0).get(column1));
        assertEquals(idValue, child.get(column0));
        assertEquals(ageValue, child.get(column1));
    }

    @Test
    public void testFindById_withExecute_andInvalidIdColumn() {
        String invalidColumnName = ";DROP ALL";

        try {
            child.findAllById(invalidColumnName, 0).execute(ModelExtension.class);
            assertTrue(false);
        } catch (InvalidColumnsException e) {
            // Yay
        }

    }

    @Test
    public void testFindById_withExecute_andTwoObjectsInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 22;
        int idValue1 = 1;
        int ageValue1 = 24;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES (" +
            idValue0 + ", " +
            ageValue0 + "), (" +
            idValue1 + ", " +
            ageValue1 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAllById(column0, 1).execute(ModelExtension.class);
        
        assertEquals(1, models.size());
        assertEquals(idValue1, models.get(0).get(column0));
        assertEquals(ageValue1, models.get(0).get(column1));
        assertEquals(idValue1, child.get(column0));
        assertEquals(ageValue1, child.get(column1));
        
    }

    @Test
    public void testFindByColumn_withExecute_andNoObjectsInTable() {
        String column0 = "user_id";
        String column1 = "age";
        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAllByColumn(column1, 22).execute(ModelExtension.class);

        assertEquals(0, models.size());
    }

    @Test
    public void testFindByColumn_withExecute_andOneObjectInTable_andValidIdColumn() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            idValue + "', " +
            ageValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAllById(column1, 22).execute(ModelExtension.class);
        
        assertEquals(1, models.size());
        assertEquals(idValue, models.get(0).get(column0));
        assertEquals(ageValue, models.get(0).get(column1));
        assertEquals(idValue, child.get(column0));
        assertEquals(ageValue, child.get(column1));
    }

    @Test
    public void testFindByColumn_withExecute_andInvalidIdColumn() {
        String invalidColumnName = ";DROP ALL";

        try {
            child.findAllById(invalidColumnName, 0).execute(ModelExtension.class);
            assertTrue(false);
        } catch (InvalidColumnsException e) {
            // Yay
        }

    }

    @Test
    public void testFindByColumn_withExecute_andTwoObjectsInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 22;
        int idValue1 = 1;
        int ageValue1 = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES (" +
            idValue0 + ", " +
            ageValue0 + "), (" +
            idValue1 + ", " +
            ageValue1 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAllByColumn(column0, 1).execute(ModelExtension.class);
        
        assertEquals(1, models.size());
        assertEquals(idValue1, models.get(0).get(column0));
        assertEquals(ageValue1, models.get(0).get(column1));
        assertEquals(idValue1, child.get(column0));
        assertEquals(ageValue1, child.get(column1));
        
    }

    @Test
    public void testFindColumns_withOneValidColumn_andOneObjectInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            idValue + "', " +
            ageValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findColumns(column0).execute(ModelExtension.class);

        assertEquals(1, models.size());
        assertEquals(idValue, models.get(0).get(column0));
        assertEquals(idValue, child.get(column0));
    }

    @Test
    public void testFindColumns_withTwoValidColumns_andOneObjectInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            idValue + "', " +
            ageValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findColumns(column0, column1).execute(ModelExtension.class);

        assertEquals(1, models.size());
        assertEquals(idValue, models.get(0).get(column0));
        assertEquals(ageValue, models.get(0).get(column1));
        assertEquals(idValue, child.get(column0));
        assertEquals(ageValue, child.get(column1));
    }

    @Test
    public void testFindColumns_withTwoValidColumns_andMultipleObjectsInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 22;
        int idValue1 = 1;
        int ageValue1 = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES (" +
            idValue0 + ", " +
            ageValue0 + "), (" +
            idValue1 + ", " +
            ageValue1 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findColumns(column0, column1).execute(ModelExtension.class);
        
        assertEquals(2, models.size());
        assertEquals(idValue0, models.get(0).get(column0));
        assertEquals(ageValue0, models.get(0).get(column1));
        assertEquals(idValue1, models.get(1).get(column0));
        assertEquals(ageValue1, models.get(1).get(column1));
    }
    
    @Test
    public void testFindColumns_withTwoValidColumns_andNoObjectsInTable() {
        String column0 = "user_id";
        String column1 = "age";

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findColumns(column0, column1).execute(ModelExtension.class);
        
        assertEquals(0, models.size());
    }

    // TODO test where() and whereAnd() functions

    // TODO test joinUsing() and joinOn() functions

    @Test
    public void testUpdate_withOneObjectInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            idValue + "', " +
            ageValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        child.setColumn(column0, idValue).setColumn(column1, ageValue + 5).update(column0, ModelExtension.class);

        try {
            String sql = "SELECT * FROM ModelExtension";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            assertEquals(idValue, rs.getInt(column0));
            assertEquals(ageValue + 5, rs.getInt(column1));
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }

    @Test
    public void testUpdate_withNoObjectInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        try {
            child.setColumn(column0, idValue).setColumn(column1, ageValue).update(column0, ModelExtension.class);
            assertTrue(false);
        } catch (ResourcePersistenceException e) {
            // Yay
        }
    }

    @Test
    public void testDelete_withOneObjectInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            column0 + ", " +
            column1 + ") VALUES ('" +
            idValue + "', " +
            ageValue + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child
                                        .setColumn(column0, idValue)
                                        .setColumn(column1, ageValue)
                                        .delete()
                                        .execute(ModelExtension.class);

        try {
            String sql = "SELECT * FROM ModelExtension";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            assertFalse(rs.next());
            assertEquals(0, models.size());
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }
    @Test
    public void testDelete_withNoObjectInTable() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue = 0;
        int ageValue = 22;

        try {
            String sql = "CREATE TABLE ModelExtension (" +
            column0 + " int, " +
            column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        List<ModelExtension> models = null;
        try {
            models = child
                        .setColumn(column0, idValue)
                        .setColumn(column1, ageValue)
                        .delete()
                        .execute(ModelExtension.class);
            assertTrue(false);
        } catch (ResourcePersistenceException e) {
            // Yay
        }
        assertNull(models);
    }
}
