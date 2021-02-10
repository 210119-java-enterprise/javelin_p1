package com.revature;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class provides a simple interface to
 * initialize a {@code Connection} to a database.
 */
public final class Setup {

    private static Connection conn = null;
    private static final Logger logger = LogManager.getLogger(Setup.class);

    private Setup() {
        super();
    }

    
    /**
     * Force loads the PostgreSQL driver so that we can use it
     * as soon as needed.
     */
    {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            logger.fatal(e.getMessage());
        }
    }

    /**
     * Opens a connection to database with given parameters.
     * Default schema is Public
     * @param url database url of the form {@code jdbc:subprotocol:subname}
     * @param user the database user on whose behalf the connection is being made
     * @param password the user's password
     * @throws SQLException - if a database access error occurs or the url is {@code null}
     */
    public static void open(String url, String user, String password) throws SQLException {
        if (conn != null) {
            logger.info("Closing open connection" + conn.toString());
            close();
        }
        try {
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            logger.fatal(e.getMessage());
            throw new SQLException(e);
        }
        
    }

    /**
     * Opens a connection to database with given parameters.
     * Sets the schema of database.
     * @param url database url of the form {@code jdbc:subprotocol:subname}
     * @param user the database user on whose behalf the connection is being made
     * @param password the user's password
     * @param schema the name of the schema in which to work
     * @throws SQLException - if a database access error occurs or the url is {@code null}
     */
    public static void open(String url, String user, String password, String schema) throws SQLException {
        try {
            open(url, user, password);
            conn.setSchema(schema);
        } catch (SQLException e) {
            logger.fatal(e.getMessage());
            throw new SQLException(e);
        }
        
    }

    /**
     * Closes the connection to the PostgreSQL database.
     */
    public static void close() {
        // Can't use try-with-resources
        try {
            conn.close();
        } catch (SQLException e) {
            logger.error("Error when closing connection: " + e.getMessage());
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.fatal("Error when closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the instace of {@code Connection}
     * @return the connection to the PostgreSQL database
     */
    protected static Connection getConnection() {
        return conn;
    }
    
}
