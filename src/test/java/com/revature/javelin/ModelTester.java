package com.revature.javelin;

import static org.junit.Assert.*;

import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.revature.javelin.exceptions.InvalidColumnsException;
import com.revature.javelin.exceptions.InvalidQueryException;
import com.revature.javelin.exceptions.ResourcePersistenceException;
import com.revature.javelin.exceptions.TypeMismatchException;

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
            sql = "DROP TABLE IF EXISTS Test";
            pstmt = Setup.getConnection().prepareStatement(sql);
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
    @Test
    public void testWhere_withValidQuery() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 23;
        int idValue1 = 1;
        int ageValue1 = 22;
        int idValue2 = 2;
        int ageValue2 = 18;

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
            ageValue1 + "), (" +
            idValue2 + ", " +
            ageValue2 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAll().where(column1 + ">20").execute(ModelExtension.class);

        assertEquals(2, models.size());
        assertEquals(idValue0, models.get(0).get(column0));
        assertEquals(ageValue0, models.get(0).get(column1));
        assertEquals(idValue1, models.get(1).get(column0));
        assertEquals(ageValue1, models.get(1).get(column1));
    }

    @Test
    public void testWhere_withInvalidQuery() {
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

        try {
            List<ModelExtension> models = child.findAll().where("; DROP TABLE").execute(ModelExtension.class);
            assertTrue(false);
        } catch (InvalidQueryException e) {
            // Yay
        }
    }

    @Test
    public void testWhereAnd_withNoExistingWhereClause_andValidQuery() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 23;
        int idValue1 = 1;
        int ageValue1 = 22;
        int idValue2 = 2;
        int ageValue2 = 18;

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
            ageValue1 + "), (" +
            idValue2 + ", " +
            ageValue2 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAll().whereAnd(column1 + ">20").execute(ModelExtension.class);

        assertEquals(2, models.size());
        assertEquals(idValue0, models.get(0).get(column0));
        assertEquals(ageValue0, models.get(0).get(column1));
        assertEquals(idValue1, models.get(1).get(column0));
        assertEquals(ageValue1, models.get(1).get(column1));
    }

    @Test
    public void testWhereAnd_withExistingWhereClause_andInvalidQuery() {
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

        try {
            List<ModelExtension> models = child.findAll().where(column1 + ">20").whereAnd(";Drop table").execute(ModelExtension.class);
            assertTrue(false);
        } catch (InvalidQueryException e) {
            // Yay
        }

    }

    @Test
    public void testWhereAnd_withExistingWhereClause_andValidQuery() {
        String column0 = "user_id";
        String column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 23;
        int idValue1 = 1;
        int ageValue1 = 22;
        int idValue2 = 2;
        int ageValue2 = 18;

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
            ageValue1 + "), (" +
            idValue2 + ", " +
            ageValue2 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAll().where(column1 + ">20").whereAnd(column0 + "<1").execute(ModelExtension.class);

        assertEquals(1, models.size());
        assertEquals(idValue0, models.get(0).get(column0));
        assertEquals(ageValue0, models.get(0).get(column1));
    }

    // TODO test joinUsing() and joinOn() functions
    @Test
    public void testJoinOn_withTwoValidTables() {
        String table1column0 = "user_id";
        String table1column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 23;
        int idValue1 = 1;
        int ageValue1 = 22;
        int idValue2 = 2;
        int ageValue2 = 18;
        String table2column0 = "pet_id";
        String table2column1 = "owner_id";
        int pet1_id = 0;
        int pet2_id = 1;

        try {
            // Set up ModelExtension Table
            String sql = "CREATE TABLE ModelExtension (" +
            table1column0 + " int, " +
            table1column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            table1column0 + ", " +
            table1column1 + ") VALUES (" +
            idValue0 + ", " +
            ageValue0 + "), (" +
            idValue1 + ", " +
            ageValue1 + "), (" +
            idValue2 + ", " +
            ageValue2 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
            // Set up Test Table
            sql = "CREATE TABLE Test (" +
            table2column0 + " int, " +
            table2column1 + " int)";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO Test (" +
            table2column0 + ", " +
            table2column1 + ") VALUES (" +
            pet1_id + ", " +
            idValue0 + "), (" +
            pet2_id + ", " +
            idValue1 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        List<ModelExtension> models = child.findAll()
                                        .joinOn(
                                            new ModelExtensionWithAnnotation(), 
                                            table1column0, 
                                            table2column1)
                                        .execute(ModelExtension.class);

        assertEquals(2, models.size());
        assertEquals(idValue0, models.get(0).get(table1column0));
        assertEquals(ageValue0, models.get(0).get(table1column1));
        assertEquals(pet1_id, models.get(0).get(table2column0));
        assertEquals(idValue0, models.get(0).get(table2column1));

        assertEquals(idValue1, models.get(1).get(table1column0));
        assertEquals(ageValue1, models.get(1).get(table1column1));
        assertEquals(pet2_id, models.get(1).get(table2column0));
        assertEquals(idValue1, models.get(1).get(table2column1));
    }

    @Ignore
    @Test
    // join using doesn't work with h2 database?
    public void testJoinUsing_withTwoValidTables_andSharedColumnName() {
        String table1column0 = "person_id";
        String table1column1 = "age";
        int idValue0 = 0;
        int ageValue0 = 23;
        int idValue1 = 1;
        int ageValue1 = 22;
        int idValue2 = 2;
        int ageValue2 = 18;
        String table2column0 = "pet_id";
        String table2column1 = "person_id";
        int pet1_id = 0;
        int pet2_id = 1;

        try {
            // Set up ModelExtension Table
            String sql = "CREATE TABLE ModelExtension (" +
            table1column0 + " int, " +
            table1column1 + " int)";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO ModelExtension (" +
            table1column0 + ", " +
            table1column1 + ") VALUES (" +
            idValue0 + ", " +
            ageValue0 + "), (" +
            idValue1 + ", " +
            ageValue1 + "), (" +
            idValue2 + ", " +
            ageValue2 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();
            // Set up Test Table
            sql = "CREATE TABLE Test (" +
            table2column0 + " int, " +
            table2column1 + " int)";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.execute();
            sql = "INSERT INTO Test (" +
            table2column0 + ", " +
            table2column1 + ") VALUES (" +
            pet1_id + ", " +
            idValue0 + "), (" +
            pet2_id + ", " +
            idValue1 + ")";
            pstmt = Setup.getConnection().prepareStatement(sql);
            pstmt.executeUpdate();


            sql = "SELECT ModelExtension.age FROM ModelExtension JOIN Test ON (ModelExtension.person_id = Test.person_id)";
            pstmt = Setup.getConnection().prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        // TODO: throwing a sql error? sql statement looks right
        List<ModelExtension> models = child.findAll()
                                        .joinUsing(
                                            new ModelExtensionWithAnnotation(), 
                                            table1column0)
                                        .execute(ModelExtension.class);

        assertEquals(2, models.size());
        System.out.println(models.get(0));
        assertEquals(idValue0, models.get(0).get(table1column0));
        assertEquals(ageValue0, models.get(0).get(table1column1));
        assertEquals(pet1_id, models.get(0).get(table2column0));
        assertEquals(idValue0, models.get(0).get(table2column1));

        assertEquals(idValue1, models.get(1).get(table1column0));
        assertEquals(ageValue1, models.get(1).get(table1column1));
        assertEquals(pet2_id, models.get(1).get(table2column0));
        assertEquals(idValue1, models.get(1).get(table2column1));
    }

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
