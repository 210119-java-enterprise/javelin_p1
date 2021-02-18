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
import java.util.Set;

import com.revature.annotations.Table;
import com.revature.exceptions.InvalidColumnsException;
import com.revature.exceptions.InvalidQueryException;
import com.revature.exceptions.ResourcePersistenceException;
import com.revature.exceptions.TypeMismatchException;

// TODO: add logging
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
     * Stores whether the database has been checked to see if
     * the table exists or not. If this value has been changed
     * to true, the table will either already exist or will
     * be created when this value is changed.
     */
    private boolean tableChecked = false;

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
            tableName = this.getClass().getSimpleName();
        }
    }

    /**
     * Creates a {@code Model} object with specified {@code fieldsAndValues}.
     * Sets the name of the table to the name of the class or the value given
     * in the {@code @Table} annotation if present.
     * @param fieldsAndValues
     *      column names and values
     */
    public Model(HashMap<String, Object> fieldsAndValues) {
        this();
        this.fieldsAndValues = fieldsAndValues;
    }

    /**
     * Retrieves the value for the given {@code columnName}
     * for the object currently stored. Does not retrieve
     * any values from database.
     * @param columnName
     * @return value of {@code columnName} in {@code this}
     *      or {@code null} if the {@code columnName} does not exist
     */
    public Object get(String columnName) {
        return fieldsAndValues.get(columnName.toUpperCase());
    }

    /**
     * Adds given {@code column} and {@code value} to {@code this}.
     * Will override previous value if {@code column} already
     * exists. Does not interact with database.
     * @param <T>
     * @param column
     *      name of column to add or modify
     * @param value
     *      value attributed to column
     * @return
     *      {@code this} to encourage method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T setColumn(String column, Object value) {
        column = column.toUpperCase();
        if (!fieldsAndValues.containsKey(column)) {
            fieldsAndValues.put(column, value);
            return (T) this;
        }
        if (fieldsAndValues.get(column).getClass().getName().equals(value.getClass().getName())) {
            fieldsAndValues.put(column, value);
        } else {
            throw new TypeMismatchException("Types " + 
                fieldsAndValues.get(column).getClass().getName() +
                " and " + 
                value.getClass().getName() +
                " are not compatible. If this was intentional, use changeColumn()");
        }
        return (T) this;
    }

    /**
     * Adds given {@code column} and {@code value} to {@code this}.
     * Will override previous value if {@code column} already
     * exists. Does not interact with database.
     * @param <T>
     * @param column
     *      name of column to add or modify
     * @param value
     *      value attributed to column
     * @return
     *      {@code this} to encourage method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T changeColumn(String column, Object value) {
        fieldsAndValues.put(column.toUpperCase(), value);
        return (T) this;
    }

    // -------------------------------------------
    // POJO methods

    @Override
    public String toString() {
        String res = "Table name: " + tableName;
        Set<String> keyset = fieldsAndValues.keySet();
        for (String key : keyset) {
            res += "\n" + key + ": " + fieldsAndValues.get(key);
        }
        return res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fieldsAndValues == null) ? 0 : fieldsAndValues.hashCode());
        result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Model other = (Model) obj;
        if (fieldsAndValues == null) {
            if (other.fieldsAndValues != null)
                return false;
        } else if (!fieldsAndValues.equals(other.fieldsAndValues))
            return false;
        if (tableName == null) {
            if (other.tableName != null)
                return false;
        } else if (!tableName.equals(other.tableName))
            return false;
        return true;
    }

    

    // -------------------------------------------
    // Getters and Setters

    public String getTableName() { return tableName; }

    public void setTableName(String tableName) { this.tableName = tableName; }
    
    protected HashMap<String, Object> getFieldsAndValues() { return fieldsAndValues; }

    protected void setFieldsAndValues(HashMap<String, Object> fieldsAndValues) { this.fieldsAndValues = fieldsAndValues; }

    // -------------------------------------------
    // CRUD methods

    /**
     * Adds all {@code fields} and {@code values} in object to {@code sqlString} and
     * executes it. This will create a new record in the database corresponding to
     * this class with the already given values. Will {@code throw} an
     * {@code InvalidColumnsException} if no fields are set. This is a starting and
     * terminal operation.
     * @param <T> object inheriting from {@code Model}
     * @param clazz the {@code Class} of this object
     */
    public <T extends Model> void create(Class<T> clazz) {
        if (fieldsAndValues.isEmpty()) {
            throw new InvalidColumnsException("No columns are set");
        }
        sqlString = "INSERT INTO " + tableName + " (";
        
        String[] keySet = (String[]) fieldsAndValues.keySet().toArray(new String[0]);
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
     * @param <T> object inheriting from {@code Model}
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
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
     * @param <T> object inheriting from {@code Model}
     * @param idColumnName the name of the column with the id value
     * @param id  the id of the object to search for
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T findAllById(String idColumnName, int id) {
        sanitizeColumn(idColumnName);
        sqlString = "SELECT * FROM " + tableName + " WHERE " + idColumnName + "=?";
        userSqlList.add(id);

        return (T) this;
    }

    /**
     * Checks user-given column names to make sure they are in a good format
     * @param columnName
     * @throws InvalidColumnsException
     */
    private void sanitizeColumn(String columnName) throws InvalidColumnsException {
        if (!columnName.matches("[A-Za-z_]+")) {
            throw new InvalidColumnsException("Invalid name for a column, " +
                "please ensure that your column only contains alphabetic characters and underscores");
        }
    }

    /**
     * Adds query to find object with the given {@code value} for {@code columnName}
     * to be executed later. This is an starting operation, use {@code execute()} to
     * finish the query and get desired results or chain intermediary operations
     * onto this.
     * 
     * @param <T>        object inheriting from {@code Model}
     * @param columnName the name of the column to be searched
     * @param value      the value to be searched for within {@code columnName}
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T findAllByColumn(String columnName, Object value) {
        sanitizeColumn(columnName);
        sqlString = "SELECT * FROM " + tableName + " WHERE " + columnName + "=? ";
        userSqlList.add(value);
        return (T) this;
    }

    /**
     * Adds query to find specified columns of an object in the table associated
     * with class. This is a starting operation, use {@code execute()} to finish the
     * query and get desired results or chain intermediary operations onto this.
     * 
     * @param <T> object inheriting from {@code Model}
     * @param columnList the name of columns for desired values
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T findColumns(String... columnList) {
        sqlString = "SELECT ";
        for (int i = 0; i < columnList.length; i++) {
            sanitizeColumn(columnList[i]);
            if (i != columnList.length - 1) {
                sqlString += columnList[i] + ", ";
            } else {
                sqlString += columnList[i] + " ";
            }
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
     * @param <T> object inheriting from {@code Model}
     * @param query
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T where(String query) {
        sanitizeQuery(query);
        sqlString += "WHERE " + query;
        return (T) this;
    }

    /**
     * Continues a {@code WHERE} clause onto the SQL query. Use this to add onto a
     * {@code WHERE} clause - like after using {@code findAllById()} or
     * {@code where()}. This is an intermediary operation. Use after a starting
     * operation and before a terminal operation.
     * 
     * @param <T> object inheriting from {@code Model}
     * @param query SQL query to add to a {@code where} clause
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T whereAnd(String query) {
        sanitizeQuery(query);
        sqlString += "AND " + query;
        return (T) this;
    }

    /**
     * Adds a {@code JOIN...USING} clause to the SQL query. This is an
     * intermediary operation. Use after a starting operation and before
     * a terminal operation.
     * @param <T> object inheriting from {@code Model}
     * @param other Other object to join tables with
     * @param columnName name of shared column to use in join
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T joinUsing(T other, String columnName) {
        sanitizeColumn(columnName);
        sqlString += "JOIN " + other.getTableName() +
            " USING (" + columnName + ") ";
        return (T) this;
    }

    /**
     * Adds a {@code JOIN...ON} clause to the SQL query. This is an
     * intermediary operation. Use after a starting operation and before
     * a terminal operation.
     * @param <T> object inheriting from {@code Model}
     * @param other Other object to join tables with
     * @param thiscolumnName name of column in this table to use in join
     * @param otherColumnName name of column in other table to use in join
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T joinOn(T other, String thisColumnName, String otherColumnName) {
        sanitizeColumn(thisColumnName);
        sanitizeColumn(otherColumnName);
        sqlString += "JOIN " + other.getTableName() +
            " ON (" + tableName + "." + thisColumnName + 
            " = " + other.getTableName() + "." + otherColumnName + ")";
        return (T) this;
    }

    /**
     * Checks user-given queries to make sure they are in a good format
     * @param query
     * @throws InvalidQueryException
     */
    private void sanitizeQuery(String query) throws InvalidQueryException {
        if (!query.matches("[A-Za-z_\\s]+")) {
            throw new InvalidQueryException("Invalid query, " +
                "please ensure that your query only contains alphabetic characters, underscores and whitespace");
        }
    }

    /**
     * Updates all {@code fields} and {@code values} in object to table. This will
     * update a record with the same {@code id} value in the database corresponding
     * to this class. Will {@code throw InvalidColumnsException} if no fields are
     * set. This is a starting and terminal operation.
     * 
     * @param <T>   object inheriting from {@code Model}
     * @param primaryKeyColumn, the name of the primary key column in the table
     * @param clazz the {@code Class} of this object
     */
    public <T extends Model> void update(String primaryKeyColumn, Class<T> clazz) {
        sqlString = "UPDATE " + tableName + " SET ";
        // for loop
        String[] keySet = (String[]) fieldsAndValues.keySet().toArray(new String[0]);
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
        sanitizeColumn(primaryKeyColumn);
        sqlString += primaryKeyColumn + "=" + fieldsAndValues.get(primaryKeyColumn.toUpperCase()) + " ";
        execute(clazz);
    }

    /**
     * This method will start a {@code DELETE} query. Use intermediary operations to
     * specify what to delete in the table associated with this class. This is a
     * starting operation, use {@code execute()} to finish the query and get desired
     * results or chain intermediary operations onto this.
     * 
     * @param <T> object inheriting from {@code Model}
     * @return {@code this} to allow for method chaining
     */
    @SuppressWarnings("unchecked") 
    public <T extends Model> T delete() {
        sqlString = "DELETE FROM " + tableName + " ";
        return (T) this;
    }

    /**
     * Executes the SQL command stored in {@code sqlString}.
     * Checks if SQL command is a {@code SELECT} statements
     * or other valid command. Checks if table already exists
     * in the database and throws a {@code ResourcePersistenceException}
     * if it does not. Will add all previously given
     * user input, column names and values into a {@code PreparedStatement}
     * and execute it. Will return an empty list if the
     * command is not a {@code SELECT} statement, otherwise
     * returns a list of <T> objects given by the {@code SELECT}
     * statement. If the {@code SELECT} query returns only 1 item,
     * sets {@code this} to the returned item.
     * @param <T> object inheriting from {@code Model}
     * @param clazz the {@code Class} of this object
     * @return a list of objects returned by query,
     *      or empty list.
     */
    public <T extends Model> List<T> execute(Class<T> clazz) {
        ResultSet rs = null;
        boolean isQuery;
        // Make sure a starting operation was used
        if (sqlString.startsWith("SELECT")) {
            isQuery = true;
        } else if (sqlString.startsWith("INSERT") || sqlString.startsWith("UPDATE") || sqlString.startsWith("DELETE")) {
            isQuery = false;
        } else {
            throw new InvalidQueryException(
                    "A starting operation was not used. Start a query by using methods like delete() or find()." +
                    " Your query was: `" + sqlString + "`");
        }

        // Execute sqlString on the database
        try {
            if (!tableChecked) {
                // Check if table already exists in database
                boolean tableExists = false;
                ResultSet tables = Setup.getConnection().getMetaData().getTables(null, null, "%", null);
                while (tables.next()) {
                    // System.out.println(tables.getString(3));
                    if (tables.getString(3).equalsIgnoreCase(tableName)) {
                        tableExists = true;
                        break;
                    }
                }
                // Table does not exist in database, throw exception
                // TODO create table or add a create table method
                if (!tableExists) {
                    throw new ResourcePersistenceException("Table " +
                        tableName +
                        " could not be found, please create table and try again.");
                }
            }
            // sqlString = "SELECT * FROM ModelExtension WHERE ?=?";
            PreparedStatement pstmt = Setup.getConnection().prepareStatement(sqlString);
            // pstmt.setString(1, "user_id");
            // pstmt.setObject(2, 0);
            for (int i = 0; i < userSqlList.size(); i++) {
                pstmt.setObject(i + 1, userSqlList.get(i));
            }
            // If the sqlString is a SELECT query, get result in resultSet
            if (isQuery) {
                // System.out.println(pstmt.toString());
                // System.out.println(pstmt.sql);
                pstmt.execute();
                rs = pstmt.getResultSet();
            } else {
                if (pstmt.executeUpdate() > 0) {
                    rs = pstmt.getGeneratedKeys();
                } else {
                    if (sqlString.startsWith("INSERT")) {
                        throw new ResourcePersistenceException("Insert function failed, please please make sure columns " +
                        "and values are valid.");
                    }
                    else if (sqlString.startsWith("UPDATE")) {
                        throw new ResourcePersistenceException("Update function failed, please please make sure that an object " +
                        "with given primary key column and value exists in table to be updated.");
                    }
                    else if (sqlString.startsWith("DELETE")) {
                        throw new ResourcePersistenceException("Delete function failed, please make sure that an object " +
                                "with given columns and values exists in table to be deleted.");
                    }
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<T> newModelList = new ArrayList<>();

        // Get all objects from resultSet if a SELECT statement was used
        if (isQuery) {
            try {
                while (rs.next()) {
                    ResultSetMetaData rsmd = rs.getMetaData();
                    // Put all columns into HashMap<String,Object>
                    HashMap<String, Object> fieldMap = new HashMap<>();
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        String columnName = rsmd.getColumnName(i+1);
                        Object value = rs.getObject(columnName);
                        fieldMap.put(columnName, value);
                    }

                    // Get constructor for T and create a new object of that class
                    Constructor<T> ctor = clazz.getConstructor();


                    // Add it to the list
                    T temp = ctor.newInstance();
                    temp.setFieldsAndValues(fieldMap);
                    newModelList.add(temp);
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SQLException | NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (newModelList.size() == 1) {
                fieldsAndValues = newModelList.get(0).getFieldsAndValues();
                tableName = newModelList.get(0).getTableName();
            }
        }

        sqlString = "";
        userSqlList = new ArrayList<>();
        return newModelList;
    }
    
}
