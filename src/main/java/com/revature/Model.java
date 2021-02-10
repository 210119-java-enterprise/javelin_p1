package com.revature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
     * Stores the sql statement that will be run by the {@code execute()} method
     */
    private ArrayList<Object> userSqlList;

    /**
     * Stores the sql query that will be run by the {@code execute()} method.
     * Used primarily in SQL query building functions (where, update, delete)
     */
    private String sqlString = "";

    /**
     * Creates a {@code Model} object. Sets the name of table to the name of the
     * class or the value given in {@code @Table} annotation if present.
     */
    public Model() {
        fieldsAndValues = new HashMap<>();
        userSqlList = new ArrayList<>();
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
     * This is a terminal operation.
     */
    public void create() {
        if (fieldsAndValues.isEmpty()) {
            throw new InvalidColumnsException("No columns are set");
        }
        sqlString = "INSERT INTO " + tableName + " (";
        String[] keySet = (String[]) fieldsAndValues.keySet().toArray();
        for (int i = 0; i < keySet.length; i++) {
            sqlString += keySet[i];
            if (i != keySet.length - 1) {
                 sqlString += ", ";
            } else {
                sqlString += ") ";
            }
        }
        sqlString += "VALUES (";
        for (int i = 0; i < keySet.length; i++) {
            if (i != keySet.length - 1) {
                sqlString += "?, ";
            } else {
                sqlString += "?)";
            }
            userSqlList.add(fieldsAndValues.get(keySet[i]));
        }
        execute();

    }

    /**
     * Adds query to find all objects in associated table
     * to be executed later.
     * This is an starting operation, use {@code execute()}
     * to finish the query and get desired results
     * or chain intermediary operations onto this.
     * @param <T>
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findAll() {
        sqlString = "SELECT * FROM " + tableName;
        return (T) this;
    }
    
    /**
     * Adds query to find object by given {@code id} to be executed
     * later. Will not return the object with the specified
     * id. 
     * This is an starting operation, use {@code execute()}
     * to finish the query and get desired results
     * or chain intermediary operations onto this.
     * @param <T>
     * @param id
     *      the id of the object to search for
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findAllById(int id) {
        String idKey = "";
        for (String key : fieldsAndValues.keySet()) {
            if (key.contains("id") || key.contains("ID") || key.contains("Id")) {
                idKey = key;
                break;
            }
        }
        if (idKey.equals("")) {
            throw new InvalidColumnsException("Could not find an appropriate id key! Make sure to set your primary key and that it contains \"id\" in it");
        }
        sqlString = "SELECT * FROM " + tableName + " WHERE " + idKey + " = ?";
        userSqlList.add(id);
        
        return (T) this;
    }
    
    /**
     * Adds query to find object with the given {@code value}
     * for {@code columnName} to be executed later.
     * This is an starting operation, use {@code execute()}
     * to finish the query and get desired results
     * or chain intermediary operations onto this.
     * @param <T>
     * @param columnName
     *      the name of the column to be searched
     * @param value
     *      the value to be searched for within {@code columnName}
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findAllByColumn(String columnName, Object value) {
        sqlString = "SELECT * FROM " + tableName + " WHERE ? = ? ";
        userSqlList.add(columnName);
        userSqlList.add(value);
        return (T) this;
    }
    
    /**
     * Adds query to find specified columns of
     * an object in the table associated with class.
     * This is a starting operation, use {@code execute()}
     * to finish the query and get desired results
     * or chain intermediary operations onto this.
     * @param <T>
     * @param columnList
     *      the name of columns for desired values
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findColumns(String... columnList) {
        sqlString = "SELECT ";
        for (int i = 0; i < columnList.length; i++) {
            if (i != columnList.length - 1) {
                sqlString += "?, ";
            } else {
                sqlString += "? ";
            }
            userSqlList.add(columnList[i]);
        }
        sqlString += "FROM " + tableName + " ";
        return (T) this;
    }

    /**
     * Adds a {@code WHERE} clause onto the SQL query.
     * Use this to start a {@code WHERE} clause - after
     * using {@code findColumns()} or {@code findAll()}.
     * This is an intermediary operation. Use after a
     * starting operation and before a terminal operation.
     * @param <T>
     * @param query
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T where(String query) {
        sqlString += "WHERE ? ";
        userSqlList.add(query);
        return (T) this;
    }

    /**
     * Continues a {@code WHERE} clause onto the SQL query.
     * Use this to add onto a {@code WHERE} clause - like after
     * using {@code findAllById()} or {@code where()}.
     * This is an intermediary operation. Use after a
     * starting operation and before a terminal operation.
     * @param <T>
     * @param query
     *      SQL query to add to a {@code where} clause
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T whereAnd(String query) {
        sqlString += "AND ? ";
        userSqlList.add(query);
        return (T) this;
    }

    /**
     * Updates all {@code fields} and {@code values} in object to table.
     * This will update a record with the same {@code id} value in the database
     * corresponding to this class. Will {@code throw InvalidColumnsException}
     * if no fields are set.
     * This is a terminal operation.
     */
    public void update() {
        sqlString = "UPDATE " + tableName + " SET ";
        // for loop
        String[] keySet = (String[]) fieldsAndValues.keySet().toArray();
        for (int i = 0; i < keySet.length; i++) {
            sqlString += keySet[i] + "=" + fieldsAndValues.get(keySet[i]);
            if (i != keySet.length - 1) {
                sqlString += ", ";
            } else {
                sqlString += " ";
            }
        }
        sqlString += "WHERE ";
        // find id column and value
        String idKey = "";
        for (String key : keySet) {
            if (key.contains("id") || key.contains("ID") || key.contains("Id")) {
                idKey = key;
                break;
            }
        }
        if (idKey.equals("")) {
            throw new InvalidColumnsException("Could not find an appropriate id key! Make sure to set your primary key and that it contains \"id\" in it");
        }
        sqlString += idKey + "=" + fieldsAndValues.get(idKey) + " ";
        execute();
    }

    /**
     * 
     * @param <T>
     * @return
     */
    public void delete() {
        
        execute();
    }

    /**
     * 
     * @param <T>
     * @return
     */
    public <T extends Model> List<T> execute() {


        sqlString = "";
        userSqlList = new ArrayList<>();
        return null;
    }
    
}
