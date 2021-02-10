package com.revature;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;

import com.revature.annotations.Table;
import com.revature.exceptions.InvalidColumnsException;

public abstract class Model {

    /**
     * Holds the name of the table associated with the class
     */
    private String tableName;

    /**
     * Holds all the fields and values of the table
     */
    private HashMap<String, Object> fieldsAndValues;

    /**
     * Stores the sql query that will be run by the {@code execute()} method
     */
    private String sqlString = "";

    /**
     * Creates a {@code Model} object. Sets the name of table to the name of the
     * class or the value given in {@code @Table} annotation if present.
     */
    public Model() {
        fieldsAndValues = new HashMap<>();
        if (this.getClass().isAnnotationPresent(Table.class)
                && this.getClass().getAnnotation(Table.class).value() != "") {
            tableName = this.getClass().getAnnotation(Table.class).value();
        } else {
            tableName = this.getClass().getName();
        }
    }

    public Object get(String field) {
        return fieldsAndValues.get(field);
    }

    public <T extends Model> T setField(String field, Object value) {
        fieldsAndValues.put(field, value);
        return (T) this;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    // -------------------------------------------
    // CRUD methods

    /**
     * Adds all {@code fields} and {@code values} in object to {@code sqlString} and
     * executes it. This will create a new record in the database corresponding to
     * this class with the already given values. Will {@code throw} an
     * {@code InvalidColumnsException} if no fields are set.
     */
    public void create() {
        if (fieldsAndValues.isEmpty()) {
            throw new InvalidColumnsException("No columns are set");
        }
        sqlString = "INSERT INTO " + tableName + " (";
        String[] keySet = (String[]) fieldsAndValues.keySet().toArray();
        for (int i = 0; i < keySet.length; i++) {
            if (i != keySet.length - 1) {
                sqlString += keySet[i] + ", ";
            } else {
                sqlString += keySet[i] + ") ";
            }
        }
        sqlString += "VALUES (";
        for (int i = 0; i < keySet.length; i++) {
            if (i != keySet.length - 1) {
                sqlString += "?, ";
            } else {
                sqlString += "?)";
            }
        }
        // Add all fields and values to sqlString and execute it
        try {
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sqlString);
            for (int i = 0; i < keySet.length; i++) {
                pstmt.setObject(i + 1, fieldsAndValues.get(keySet[i]));
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // Reset sqlString to empty for other methods
        sqlString = "";

    }

    /**
     * Will retrieve all records in table associated
     * with this class.
     * @param <T>
     * @return a {@code Set} of all objects in the
     *      table associated with this class
     */
    public <T extends Model> Set<T> findAll() {
        sqlString = "SELECT * FROM " + tableName;
        
        return execute();
    }
    
    /**
     * Adds query to {@code sqlString} to be executed
     * later. Will not return the object with the specified
     * id. Use {@code execute()} to finish the query
     * and get desired results.
     * @param <T>
     * @param id
     *      the id of the object to search for
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findById(int id) {
        sqlString = "SELECT * FROM " + tableName + " WHERE";
        return (T) execute().toArray()[0];
    }

    public <T extends Model> T findByColumn(String columnName, Object value) {
        return (T) this;
    }

    public <T extends Model> T where(String query) {
        return (T) this;
    }

    public <T extends Model> T update() {
        return (T) this;
    }

    public <T extends Model> T delete() {
        return (T) this;
    }

    public <T extends Model> Set<T> execute() {
        return null;
    }
    
}
