package com.privalia.qa.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.*;


/**
 * Generic operations on SQL relational databases. Currently supports mysql/postgres
 * @author Jose Fernandez
 */
public class SqlUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlUtils.class);

    private String host;

    private int port;

    private String dataBaseType;

    private String dataBaseName;

    private Boolean security;

    private String user;

    private String password;

    private Connection sqlConnection;

    private List<List<String>> previousSqlResult;

    public List<List<String>> getPreviousSqlResult() {
        return previousSqlResult;
    }

    private void setPreviousSqlResult(List<List<String>> previousSqlResult) {
        this.previousSqlResult = previousSqlResult;
    }

    /**
     * Generic constructor.
     */
    public SqlUtils() {
        this.dataBaseType = System.getProperty("SQLDB_TYPE", "MYSQL");
        this.host = System.getProperty("SQLDB_HOST", "172.17.0.1");
        this.port = Integer.parseInt(System.getProperty("SQLDB_PORT", "3306"));
        this.dataBaseName = System.getProperty("SQLDB_NAME", "sys");
        this.security = Boolean.parseBoolean(System.getProperty("SQLDB_SECURITY", "false"));
        this.user = System.getProperty("SQLDB_USER", "root");
        this.password = System.getProperty("SQLDB_PASSWORD", "mysql");
    }


    /**
     * Attempts to establish a connection with the given parameters. The DriverManager attempts to select an appropriate driver from the set of registered JDBC drivers.
     * @param host          URL of remote host
     * @param port          Database port
     * @param dataBaseType  Database type (currently MYSQL/POSTGRESQL)
     * @param dataBaseName  Name of the remote database
     * @param security      True if secure connection
     * @param user          Database user
     * @param password      Database password
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public void connect(String host, int port, String dataBaseType, String dataBaseName, Boolean security, String user, String password) throws ClassNotFoundException, SQLException {

        LOGGER.debug(String.format("Database type set to: %s", this.dataBaseType));
        this.dataBaseType = dataBaseType;
        this.dataBaseName = dataBaseName;

        switch (dataBaseType.toUpperCase()) {
            case "MYSQL":
                Class.forName("com.mysql.cj.jdbc.Driver");
                break;

            case "POSTGRESQL":
                Class.forName("org.postgresql.Driver");
                break;

            default:
                Class.forName("org.postgresql.Driver");
                break;
        }


        /**
         * MySQL databases often fail when executing multiple queries separated by comma ";". This was causing problems when executing
         * a bunch of SQL statements from an SQL file. To avoid this problem, we had to set allowMultiQueries = true
         */
        Properties props = new Properties();
        String connectionString = "jdbc:" + dataBaseType.toLowerCase() + "://" + host + ":" + port + "/" + dataBaseName + "?user=" + user + "&password=" + password + "&allowMultiQueries=true";
        LOGGER.debug(String.format("Starting connection using %s", connectionString));

        if (this.security) {

            // TODO: 1/02/18 Create case when using SSL, adding correct props
            this.sqlConnection = DriverManager
                    .getConnection(connectionString, props);

            return;
        }

        this.sqlConnection = DriverManager
                .getConnection(connectionString + "&useSSL=false");

    }

    /**
     * Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE statement
     * or an SQL statement that returns nothing, such as an SQL DDL statement.
     * @param query An SQL Data Manipulation Language (DML) statement, such as INSERT, UPDATE or DELETE;
     *              or an SQL statement that returns nothing, such as a DDL statement.
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     * @throws SQLException
     */
    public int executeUpdateQuery(String query) throws SQLException {

        int result = 0;

        LOGGER.debug(String.format("Executing query %s", query));
        try (Statement myStatement = this.sqlConnection.createStatement()) {
            result = myStatement.executeUpdate(query);
            return result;
        }
    }

    /**
     * Executes the given SQL statement, which returns a single ResultSet object. Instead of a ResultSet, this method
     * returns a List of lists (List<List<String>>). This method will return in the first list the
     * columns name, and the remaining lists are the rows (if the query returned any). So, this method
     * will always return at least 1 List (size 1).
     * This way of representing a ResultSet is very similar to the structure of a {@link cucumber.api.DataTable} in
     * cucumber, making the comparison easier
     * @param query An SQL statement to be sent to the database, typically a static SQL SELECT statement
     * @return A list of Lists
     * @throws SQLException
     */
    public List<List<String>> executeSelectQuery(String query) throws SQLException {

        List<List<String>> sqlTable = new ArrayList<>();
        ResultSet rs = null;

        try (Statement myStatement = this.sqlConnection.createStatement()) {
            LOGGER.debug(String.format("Executing query %s", query));
            rs = myStatement.executeQuery(query);
            return this.resultSetToList(rs);
        }

    }

    /**
     * Transforms the given ResultSet to a List<List<String>>
     * @param resultSet
     * @return
     */
    private List<List<String>> resultSetToList(ResultSet resultSet) throws SQLException {

        List<List<String>> sqlTable = new ArrayList<>();

        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int count = resultSetMetaData.getColumnCount();

        /*Store the column names*/
        List<String> columnHeader = new LinkedList<>();
        for (int i = 1; i <= count; i++) {
            columnHeader.add(resultSetMetaData.getColumnName(i));
        }
        sqlTable.add(columnHeader);

        /*Store the values*/
        while (resultSet.next()) {
            List<String> sqlTableAux = new LinkedList<>();
            for (int i = 1; i <= count; i++) {
                sqlTableAux.add(resultSet.getObject(i).toString());
            }
            sqlTable.add(sqlTableAux);
        }

        return sqlTable;
    }

    /**
     * Executes the given SQL statement, which may return multiple results
     * If the SQL statement returned a ResultSet, it is converted to a List<List<String>> and stored
     * in an accessible variable in case it needs to be used
     * @param query Any SQL statement (SELECT or UPDATE type)
     * @return true if the result is a ResultSet object; false if it is an update count or there are no results
     * @throws SQLException
     */
    public boolean executeQuery(String query) throws SQLException {

        LOGGER.debug(String.format("Executing query %s", query));
        try (Statement myStatement = this.sqlConnection.createStatement()) {
            boolean r = myStatement.execute(query);

            if (r) {
                ResultSet resultSet = myStatement.getResultSet();
                this.setPreviousSqlResult(this.resultSetToList(resultSet));
            }

            return r;
        }
    }

    /**
     * Verify if a table exists
     * @param tableName Table name
     * @return true if the table exists, false otherwise
     */
    public boolean verifyTable(String tableName) throws SQLException {

        boolean exists = false;
        String query;

        if (this.dataBaseType.toLowerCase().matches("mysql")) {
            query = "SELECT * FROM information_schema.tables WHERE table_schema = '" + this.sqlConnection.getCatalog() + "' AND table_name = '" + tableName + "' LIMIT 1;";
        } else {
            query = "SELECT * FROM pg_tables WHERE tablename = " + "\'" + tableName + "\'" + ";";
        }

        LOGGER.debug(String.format("Verifying if table %s exists. Executing %s", tableName, query));
        try (Statement myStatement = this.sqlConnection.createStatement()) {

            ResultSet rs = myStatement.executeQuery(query);
            //if there are no data row, table doesn't exists
            if (rs.next()) {
                exists = true;
            }
            rs.close();
            return exists;
        }

    }

    /**
     * Verify the state of the connection
     * @return
     */
    public boolean connectionStatus() {

        LOGGER.debug("Checking DB connection status");
        boolean status = false;
        try {
            status = !this.sqlConnection.isClosed();
            return status;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Closes the connection to the database
     */
    public void disconnect() throws SQLException {

        LOGGER.debug(String.format("Closing connection to DB %s in %s:%s", this.dataBaseName, this.host, this.port));
        this.sqlConnection.close();
    }


}
