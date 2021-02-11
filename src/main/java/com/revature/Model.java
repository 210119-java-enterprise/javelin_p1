package com.revature;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.revature.annotations.Table;
import com.revature.exceptions.InvalidColumnsException;
import com.revature.exceptions.InvalidQueryException;
import com.revature.exceptions.ResourcePersistenceException;

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
     * Stores the sql query that will be run by the {@code execute()} method. Used
     * primarily in SQL query building functions (where, update, delete)
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

    private Model(HashMap<String, Object> fieldsAndValues) {
        this();
        this.fieldsAndValues = fieldsAndValues;
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
     * {@code InvalidColumnsException} if no fields are set. This is a starting and
     * terminal operation.
     */
    public <T extends Model> void create(Class<T> clazz) {
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
        execute(clazz);

    }

    /**
     * Adds query to find all objects in associated table to be executed later. This
     * is an starting operation, use {@code execute()} to finish the query and get
     * desired results or chain intermediary operations onto this.
     * 
     * @param <T>
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findAll() {
        sqlString = "SELECT * FROM " + tableName;
        return (T) this;
    }

    /**
     * Adds query to find object by given {@code id} to be executed later. Will not
     * return the object with the specified id. This is an starting operation, use
     * {@code execute()} to finish the query and get desired results or chain
     * intermediary operations onto this.
     * 
     * @param <T>
     * @param id  the id of the object to search for
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
            throw new InvalidColumnsException(
                    "Could not find an appropriate id key! Make sure to set your primary key and that it contains \"id\" in it");
        }
        sqlString = "SELECT * FROM " + tableName + " WHERE " + idKey + " = ?";
        userSqlList.add(id);

        return (T) this;
    }

    /**
     * Adds query to find object with the given {@code value} for {@code columnName}
     * to be executed later. This is an starting operation, use {@code execute()} to
     * finish the query and get desired results or chain intermediary operations
     * onto this.
     * 
     * @param <T>
     * @param columnName the name of the column to be searched
     * @param value      the value to be searched for within {@code columnName}
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T findAllByColumn(String columnName, Object value) {
        sqlString = "SELECT * FROM " + tableName + " WHERE ? = ? ";
        userSqlList.add(columnName);
        userSqlList.add(value);
        return (T) this;
    }

    /**
     * Adds query to find specified columns of an object in the table associated
     * with class. This is a starting operation, use {@code execute()} to finish the
     * query and get desired results or chain intermediary operations onto this.
     * 
     * @param <T>
     * @param columnList the name of columns for desired values
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
     * Adds a {@code WHERE} clause onto the SQL query. Use this to start a
     * {@code WHERE} clause - after using {@code findColumns()} or
     * {@code findAll()}. This is an intermediary operation. Use after a starting
     * operation and before a terminal operation.
     * 
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
     * Continues a {@code WHERE} clause onto the SQL query. Use this to add onto a
     * {@code WHERE} clause - like after using {@code findAllById()} or
     * {@code where()}. This is an intermediary operation. Use after a starting
     * operation and before a terminal operation.
     * 
     * @param <T>
     * @param query SQL query to add to a {@code where} clause
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T whereAnd(String query) {
        sqlString += "AND ? ";
        userSqlList.add(query);
        return (T) this;
    }

    /**
     * Updates all {@code fields} and {@code values} in object to table. This will
     * update a record with the same {@code id} value in the database corresponding
     * to this class. Will {@code throw InvalidColumnsException} if no fields are
     * set. This is a starting and terminal operation.
     */
    public <T extends Model> void update(Class<T> clazz) {
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
            throw new InvalidColumnsException(
                    "Could not find an appropriate id key! Make sure to set your primary key and that it contains \"id\" in it");
        }
        sqlString += idKey + "=" + fieldsAndValues.get(idKey) + " ";
        execute(clazz);
    }

    /**
     * This method will start a {@code DELETE} query. Use intermediary operations to
     * specify what to delete in the table associated with this class. This is a
     * starting operation, use {@code execute()} to finish the query and get desired
     * results or chain intermediary operations onto this.
     * 
     * @return {@code this} to allow for method chaining
     */
    public <T extends Model> T delete() {
        sqlString = "DELETE FROM " + tableName + " ";
        return (T) this;
    }

    /**
     * 
     * @param <T>
     * @param clazz
     * @return
     */
    public <T extends Model> List<T> execute(Class<T> clazz) {
        ResultSet rs = null;
        boolean isQuery;
        // Make sure a starting operation was used
        if (sqlString.startsWith("SELECT")) {
            isQuery = true;
        } else if (sqlString.startsWith("CREATE") || sqlString.startsWith("UPDATE") || sqlString.startsWith("DELETE")) {
            isQuery = false;
        } else {
            throw new InvalidQueryException(
                    "A starting operation was not used. Start a query by using methods like delete() or find().");
        }

        try {
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sqlString);
            for (int i = 0; i < userSqlList.size(); i++) {
                pstmt.setObject(i + 1, userSqlList.get(i));
            }
            // If the sqlString is a SELECT query, get result in resultSet
            if (isQuery) {
                rs = pstmt.executeQuery();
            } else {
                if (pstmt.executeUpdate() > 0) {
                    rs = pstmt.getGeneratedKeys();
                } else {
                    throw new ResourcePersistenceException("Update failed, please try again");
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<T> newModelList = new ArrayList<>();

        try {
            // Need to get all objects from resultSet
            while (rs.next()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                // Put them into HashMap<String,Object> and create Model object
                HashMap<String, Object> fieldMap = new HashMap<>();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    String columnName = rsmd.getColumnName(i+1);
                    Object value = rs.getObject(columnName);
                    fieldMap.put(columnName, value);
                }

                // Get constructor for T and create a new object of that class
                Constructor<T> ctor = clazz.getConstructor(HashMap.class);

                // Add it to the list
                newModelList.add(ctor.newInstance(fieldMap));
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | SQLException | NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        sqlString = "";
        userSqlList = new ArrayList<>();
        return newModelList;
    }
    
}
