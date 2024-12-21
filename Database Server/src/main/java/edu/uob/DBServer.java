package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;


/** This class implements the DB server. */
public class

DBServer {

    private static final char END_OF_TRANSMISSION = 4;
    private String storageFolderPath;
    private HashMap<String, Database> databases;
    private Database currentDatabase;
    private Path currentDatabasePath;
    private ReservedWordsDetector reservedWordsDetector= new ReservedWordsDetector();


    public static void main(String args[]) throws IOException {
        DBServer server = new DBServer();
        server.blockingListenOn(8888);
    }

    /**
     * KEEP this signature otherwise we won't be able to mark your submission correctly.
     */
    public DBServer() {
        storageFolderPath = Paths.get("databases").toAbsolutePath().toString();
        currentDatabasePath = null;
        databases = new HashMap<>();

        try {
            // Create the database storage folder if it doesn't already exist !
            Files.createDirectories(Paths.get(storageFolderPath));
        } catch(IOException ioe) {
            System.out.println("Can't seem to create database storage folder " + storageFolderPath);
        }
    }

    /**
     * KEEP this signature (i.e. {@code edu.uob.DBServer.handleCommand(String)}) otherwise we won't be
     * able to mark your submission correctly.
     *
     * <p>This method handles all incoming DB commands and carries out the required actions.
     */
    public String handleCommand(String command) throws IOException {
        // TODO implement your server logic here
        Handler handler = new Handler();
        ArrayList<String> tokens = handler.preprocessQuery(command);
        String tableName;
        ArrayList<String> whereClause;

        if (!";".equals(tokens.get(tokens.size() - 1))) {
            return "[ERROR]: Semi colon missing at end of line";
        }

        switch (tokens.get(0).toUpperCase()){

            /* "USE " [DatabaseName] */
            case "USE":
                if (tokens.size() == 3) {
                    return useDatabase(tokens.get(1));
                }
                break;

            /* <Create>          ::=  <CreateDatabase> | <CreateTable>
               <CreateDatabase>  ::=  "CREATE " "DATABASE " [DatabaseName]
               <CreateTable>     ::=  "CREATE " "TABLE " [TableName] | "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
               [AttributeName] | [AttributeName] "," <AttributeList> */
            case "CREATE":
                if (tokens.size() < 4) {
                    return "[ERROR]: Missing database name or table name.";
                }
                //Create DATABASE
                if (tokens.get(1).equalsIgnoreCase("DATABASE") && tokens.size() == 4) {
                    return createDatabase(tokens.get(2));
                }

                //CREATE TABLE
                else if (tokens.get(1).equalsIgnoreCase("TABLE")) {
                    tableName = tokens.get(2);
                    List<String> columns = handler.extractValuesFromParenthesis(tokens);
                    return createTable(tableName, columns);
                }
                break;

            /* "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName] */
            case "DROP":
                if (tokens.size() < 4) {
                    return "[ERROR]: Missing database name or table name.";
                }

                if (tokens.get(1).equalsIgnoreCase("DATABASE") && tokens.size() == 4) {
                    return dropDatabase(tokens.get(2).toLowerCase());
                } else if (tokens.get(1).equalsIgnoreCase("TABLE") && tokens.size() == 4) {
                    return dropTable(tokens.get(2).toLowerCase());
                }
                break;


            /* "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")" */
            case "INSERT":
                if (tokens.get(1).equalsIgnoreCase("INTO") && tokens.get(3).equalsIgnoreCase("VALUES") && tokens.size() >= 8) {
                    tableName = tokens.get(2).toLowerCase();
                    List<String> values = handler.extractValuesFromParenthesis(tokens);
                    return insertInto(tableName, values);
                }
                break;

            /* "SELECT " <WildAttribList> " FROM " [TableName]
             | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition> */
            case "SELECT":
                List<String> columnNames = handler.extractColumnsFromSelect(tokens);
                if (columnNames.isEmpty()) {
                    return "[ERROR]: Missing attributes.";
                }

                if (tokens.size() >= 5) {
                    tableName = handler.extractTableNameFromSelect(tokens).toLowerCase();
                    whereClause = handler.extractWhereClause(tokens);
                    return selectFrom(tableName, columnNames, whereClause);
                }
                break;

            /* "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>  */
            case "UPDATE":
                tableName = tokens.get(1).toLowerCase();
                ArrayList<String> setClause = handler.extractSetClauseFromUpdate(tokens);
                if (setClause.size() < 3) {
                    return "[ERROR]: Missing set clause.";
                }
                whereClause = handler.extractWhereClause(tokens);
                if (whereClause.size() < 3) {
                    return "[ERROR]: Missing where clause.";
                }
                return updateTable(tableName, setClause, whereClause);

            /* "DELETE " "FROM " [TableName] " WHERE " <Condition>*/
            case "DELETE":
                if (tokens.get(1).equalsIgnoreCase("FROM")) {
                    tableName = tokens.get(2).toLowerCase();
                    whereClause = handler.extractWhereClause(tokens);
                    if (whereClause.size() < 3) {
                        return "[ERROR]: Missing where clause.";
                    }
                    return deleteFrom(tableName, whereClause);
                }
                break;


            /* "JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName] */
            case "JOIN":
                if (tokens.get(2).equalsIgnoreCase("AND") && tokens.get(4).equalsIgnoreCase("ON") && tokens.get(6).equalsIgnoreCase("AND")){
                    String firstTable = tokens.get(1).toLowerCase();
                    String secondTable = tokens.get(3).toLowerCase();
                    String firstAttributeName = tokens.get(5).toLowerCase();
                    String secondAttributeName = tokens.get(7).toLowerCase();
                    return joinTables(firstTable, secondTable, firstAttributeName, secondAttributeName);
                }
                break;


                /* "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
               <AlterationType>  ::=  "ADD" | "DROP" */
            case "ALTER":
                if (tokens.size() != 6) {
                    return "[ERROR]: Please check the query command.";
                }

                if (tokens.get(1).equalsIgnoreCase("TABLE") && tokens.get(3).equalsIgnoreCase("ADD")) {
                    tableName = tokens.get(2).toLowerCase();
                    String columnName = tokens.get(4);
                    return alterTableAddColumn(tableName, columnName);

                } else if (tokens.get(1).equalsIgnoreCase("TABLE") && tokens.get(3).equalsIgnoreCase("DROP")) {
                    tableName = tokens.get(2);
                    String columnName = tokens.get(4);
                    return alterTableDropColumn(tableName, columnName);
                }
                break;
        }
        return "[ERROR]: Unrecognized command.";
    }

    //  === Methods below handle networking aspects of the project - you will not need to change these ! ===

    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.err.println("Server encountered a non-fatal IO error:");
                    e.printStackTrace();
                    System.err.println("Continuing...");
                }
            }
        }
    }

    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {

            System.out.println("Connection established: " + serverSocket.getInetAddress());
            while (!Thread.interrupted()) {
                String incomingCommand = reader.readLine();
                System.out.println("Received message: " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }


    public String createDatabase(String databaseName) throws IOException {
        // Check if the database name is valid
        if (reservedWordsDetector.isReservedWord(databaseName)) {
            return "[ERROR]: Invalid database name, using reserved words";
        }
        // Check if the database name obey the plaintext rule
        if (!databaseName.matches("^[a-zA-Z0-9]+$")) {
            return "[ERROR]: Invalid database name, please obey plaintext rule";
        }
        // Check if the database is already exist
        Path newDatabasePath = Paths.get(storageFolderPath, databaseName.toLowerCase());
        if (Files.exists(newDatabasePath)){
            return "[ERROR]: Database '" + databaseName + "' already exists.";
        } else {
            Files.createDirectories(Paths.get(newDatabasePath.toString()));
            /* Any database/table names provided by the user should be converted into lowercase
               before saving out to the filesystem */
            Database newDatabase = new Database(databaseName.toLowerCase());
            databases.put(databaseName.toLowerCase(), newDatabase);
            return "[OK]";
        }
    }

    public String useDatabase(String databaseName) {
        Path DatabasePath = Paths.get(storageFolderPath, databaseName.toLowerCase());
        // Check if the database exist
        if (Files.exists(DatabasePath)){
            currentDatabasePath = DatabasePath;
            currentDatabase = databases.get(databaseName.toLowerCase());

            /* which means, the DBServer is restarted, thus reload and add to databases */
            if (currentDatabase == null) {
                currentDatabase = new Database(databaseName.toLowerCase());
                databases.put(databaseName.toLowerCase(), currentDatabase);
                currentDatabase.loadDatabase(databaseName);
            }

            return "[OK]";
        } else {
            return "[ERROR]: Database '" + databaseName + "' does not exist.";
        }
    }

    private Path getTablePath(String tableName) {
        return currentDatabasePath.resolve(tableName.toLowerCase() + ".tab");
    }

    public String createTable(String tableName, List<String> columnNames) throws IOException {
        // Check the table name is valid
        if (reservedWordsDetector.isReservedWord(tableName)) {
            return "[ERROR]: Invalid database name, using reserved word";
        }
        // Check the table name obey the plaintext rule
        if (!tableName.matches("^[a-zA-Z0-9]+$")) {
            return "[ERROR]: Invalid table name, please obey plaintext rule";
        }

        Path tablePath = getTablePath(tableName.toLowerCase());
        // Check if the table has already exist
        if (Files.exists(tablePath)) {
            return "[ERROR]: Table '" + tableName + "' already exists.";
        }
        /* Any database/table names provided by the user should be
           converted into lowercase before saving out to the filesystem */
        if(!checkColumnNames(columnNames)) {
            return "[Error]: Using duplicate column name.";
        }

        Table table = new Table(tableName.toLowerCase(), columnNames);
        table.tablePath = tablePath;
        currentDatabase.addTable(table);
        table.updateTableFile();
        return "[OK]";
    }

    private boolean checkColumnNames(List<String> columnNames) {
        // The HashSet will contain each unique name only once.
        Set<String> uniqueNames = new HashSet<>(columnNames);

        /* If the sizes of the original list and the set are the same,
         it means there were no duplicates. */
        return uniqueNames.size() == columnNames.size();
    }

    public String insertInto(String tableName, List<String> values) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);

            if (table != null){
                // The handler will return an empty arraylist if using reserved words
                if (values.isEmpty()) {
                    return "[ERROR]: Using reserved words.";
                }

                // table.getColumnNames().size() contains "id"
                if (table.getColumnNames().size() - 1 < values.size() || table.getColumnNames().size() - 1 > values.size()) {
                    return "[ERROR]: trying to insert too many (or too few) values into a table entry.";
                } else {
                    table.insertRow(values);
                    table.updateTableFile();
                    return "[OK]";
                }
            } else {
                return "[ERROR]: Table '" + tableName + "' not exists.";
            }
        } else {
            return "[ERROR]: No database selected.";
        }
    }

    public String selectFrom(String tableName, List<String> queryColumnNames, ArrayList<String> whereClause) {
        if (currentDatabase != null){
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                List<Row> rowsToPrint;

                if (!whereClause.isEmpty()) {
                    rowsToPrint = table.selectRowsWithCondition(whereClause);
                } else {
                    rowsToPrint = table.getRows(); // If there is no where clause, select all rows
                }

                if (queryColumnNames.contains("*")) {
                    //Print all columns for the selected rows
                    return "[OK]" + "\n" + table.returnSelectedRows(rowsToPrint, table.getColumnNames());
                } else { // Check the query column name is valid
                    if (!queryColumnCheck(table, queryColumnNames)) {
                        return "[ERROR]: Attribute does not exist ";
                    }
                    //Print only specified columns for the selected rows
                    return "[OK]" + "\n" + table.returnSelectedRows(rowsToPrint, queryColumnNames);
                }
            } else {
                return "[ERROR]: Table '" + tableName + "' does not exist.";
            }
        } else {
            return "[ERROR]: No database selected.";
        }
    }

    private boolean queryColumnCheck(Table table, List<String> queryColumnNames) {
        List<String> tableColumnNames = new ArrayList<>();
        for (String columnName : table.getColumnNames()) {
            tableColumnNames.add(columnName.toLowerCase());
        }
        /* treat column names as case-insensitive for querying, but preserve the case when storing them */
        for (String queryAttribute : queryColumnNames) {
            if (tableColumnNames.contains(queryAttribute.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String deleteFrom(String tableName, ArrayList<String> whereClause) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                table.deleteRowsWithCondition(whereClause);
                table.updateTableFile();
                return "[OK]";
            } else {
                return "[ERROR]: Table '" + tableName + "' does not exist in the current database.";
            }
        } else {
            return "[ERROR]: No current database is selected.";
        }
    }

    public String dropDatabase(String databaseName) throws IOException {
        Path databasePath = Paths.get(storageFolderPath, databaseName);
        if (databases.containsKey(databaseName)) {
            deleteDirectoryRecursively(databasePath); // Recursively delete all files and the directory
            databases.get(databaseName).tables.clear(); // remove tables in the database
            databases.remove(databaseName); // remove database from data structure
            return "[OK]";
        } else {
            return "[ERROR]: Database " + databaseName + " does not exist.";
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectoryRecursively(entry); // Recursively delete directory entries
                }
            }
        }
        Files.deleteIfExists(path); // Delete the directory (now empty) or the file
    }

    public String dropTable(String tableName) {
        if (this.currentDatabase != null) {
            if (currentDatabase.getTable(tableName) != null) {
                // Delete table's file
                currentDatabase.getTable(tableName).deleteTableFile();
                // Delete table in the data structure
                currentDatabase.dropTable(tableName);
                return "[OK]";
            } else {
                return "[ERROR]: Table " + tableName + " does not exist.";
            }
        } else {
            return "[ERROR]: No database selected.";
        }
    }

    public String updateTable(String tableName, ArrayList<String> setClause, ArrayList<String> whereClause) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                // User are not allowed to set the id
                if (setClause.get(0).equalsIgnoreCase("id")) {
                    return "[ERROR]: changing (updating) the ID of a record is not allowed.";
                }
                table.updateRowsWithCondition(setClause, whereClause);
                table.updateTableFile();
                return "[OK]";
            } else {
                return "[ERROR]: Table '" + tableName + "' does not exist in the current database.";
            }
        } else {
            return "[ERROR]: No current database is selected.";
        }
    }

    public String alterTableAddColumn(String tableName, String columnName) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                /* Detect if the Column name is reserved word */
                if (reservedWordsDetector.isReservedWord(columnName)) {
                    return "[ERROR]: Using reserved word for columnName";
                }
                // Check the column name obey the plain text rule
                if (!columnName.matches("^[a-zA-Z0-9]+$")) {
                    return "[ERROR]: Invalid column name, please obey plaintext rule";
                }
                // Check if the column has already been added
                for (Column column : table.columns){
                    if (column.getName().equalsIgnoreCase(columnName)){
                        return "[ERROR]: Column " + columnName + " already exists.";
                    }
                }
                table.addColumn(columnName);
                table.updateTableFile();
                return "[OK]";
            } else {
                return "[ERROR]: Table '" + tableName + "' does not exist in the current database.";
            }
        } else {
            return "[ERROR]: No current database is selected.";
        }
    }

    public String alterTableDropColumn(String tableName, String queryColumnName) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                // User are not allowed to drop "id"
                if (queryColumnName.equalsIgnoreCase("id")) {
                    return "[ERROR]: attempting to remove the ID column from a table.";
                }
                /* treat column names as case-insensitive for querying, but preserve the case when storing them */
                String exactColumnName = null;
                for (String columnName : table.getColumnNames()) {
                    if (columnName.equalsIgnoreCase(queryColumnName)) {
                        exactColumnName = columnName;
                        break;
                    }
                }

                /* If a matching column name was found, drop the column */
                if (exactColumnName != null) {
                    table.dropColumn(exactColumnName); // Use the exact case-sensitive name found
                    table.updateTableFile();
                    return "[OK]";
                } else {
                    // If no matching column name was found, return an error
                    return "[ERROR]: Column '" + queryColumnName + "' does not exist.";
                }
            } else {
                return "[ERROR]: Table '" + tableName + "' does not exist in the current database.";
            }
        } else {
            return "[ERROR]: No current database is selected.";
        }
    }

    public String joinTables(String firstTableName, String secondTableName, String firstAttribute, String secondAttribute) throws IOException {
        if (currentDatabase != null) {
            Table firstTable = currentDatabase.getTable(firstTableName);
            Table secondTable = currentDatabase.getTable(secondTableName);
            if(!(checkOrder(firstTable, firstAttribute) && checkOrder(secondTable, secondAttribute))) {
                return "[ERROR]: The ordering of the specified tables should be the same as the ordering of the specified attributes";
            }

            List<String> firstColumnNames = firstTable.getColumnNames();
            /* discard the ids from the original tables */
            firstColumnNames.remove("id");
            /* discard the columns that the tables were matched on */
            firstColumnNames.remove(firstAttribute);

            List<String> secondColumnNames = secondTable.getColumnNames();
            secondColumnNames.remove("id");
            secondColumnNames.remove(secondAttribute);

            List<String> columnNames = new ArrayList<>();
            columnNames.add("id");
            for (String firstColumnName : firstColumnNames){
                /* attribute names in the form OriginalTableName.AttributeName  */
                columnNames.add(firstTableName + "." + firstColumnName);
            }
            for (String secondColumnName : secondColumnNames){
                columnNames.add(secondTableName + "." + secondColumnName);
            }

            Table joinTable = new Table("joinTable", columnNames);

            int rowSize = firstTable.nextRowId - 1;

            for (int i = 0; i < rowSize; i++) {
                List<String> rowValues = new ArrayList<>();
                /* create a new unique id for each of row of the table produced */
                rowValues.add(Integer.toString(joinTable.nextRowId));
                /* First Table's row value keeps the same */
                for (String firstColumn : firstColumnNames) {
                    rowValues.add(firstTable.rows.get(i).getValue(firstColumn));
                }
                /* According to foreign key, find the row */
                for (Row secondTableRow : secondTable.getRows())
                    if (firstTable.rows.get(i).getValue(firstAttribute).equals(secondTableRow.getValue(secondAttribute))) {
                        for (String secondColumn : secondColumnNames) {
                            rowValues.add(secondTableRow.getValue(secondColumn));
                        }

                    }
                joinTable.insertRow(rowValues);
            }
            return "[OK]\n" + joinTable.returnSelectedRows(joinTable.getRows(), columnNames);

        } else {
            return "[ERROR]: No current database is selected.";
        }
    }

    private boolean checkOrder(Table table, String attribute) {
        if (!table.getColumnNames().contains(attribute)){
            return false;
        } else {
            return true;
        }
    }
}
