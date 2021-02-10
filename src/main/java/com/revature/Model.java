package com.revature;

import java.util.HashMap;
import java.util.Set;

import com.revature.annotations.Table;

public abstract class Model {

    /**
     * Holds the name of the table associated with
     * the class
     */
    private String tableName;

    /**
     * Holds all the fields and values of the table
     */
    private HashMap<String, Object> fieldsAndValues;

    /**
     * Stores the sql query that will be run by the
     * {@code execute()} method
     */
    private String sqlString = "";

    /**
     * Creates a {@code Model} object. Sets
     * the name of table to the name of the class
     * or the value given in {@code @Table} annotation
     * if present.
     */
    public Model() {
        fieldsAndValues = new HashMap<>();
        if (this.getClass().isAnnotationPresent(Table.class) && this.getClass().getAnnotation(Table.class).value() != "") {
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
     * Adds all {@code fields} and {@code values}
     * in object to {@code sqlString} and executes it.
     * This will create a new record in the database
     * corresponding to this class with the already given
     * values.
     */
    public void create() {
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
        // Add all fields and values to sqlString
        execute();

    }

    public <T extends Model> Set<T> findAll() {
        return null;
    }
    
    public <T extends Model> T findById(int id) {
        return (T) this;
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
