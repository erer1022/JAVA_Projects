package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


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
                else if (tokens.get(1).equalsIgnoreCase("TABLE") && tokens.size() >= 4) {
                    tableName = tokens.get(2);
                    List<String> columns = handler.extractValuesFromParenthesis(tokens);
                    return createTable(tableName, columns);
                }

                break;

            /* "DROP " "DATABASE " [DatabaseName] | "DROP " "TABLE " [TableName] */
            case "DROP":
                if (tokens.get(1).equalsIgnoreCase("DATABASE") && tokens.size() == 4) {
                    return dropDatabase(tokens.get(2));
                } else if (tokens.get(1).equalsIgnoreCase("TABLE") && tokens.size() == 4) {
                    return dropTable(tokens.get(2));
                }
                break;


            /* "INSERT " "INTO " [TableName] " VALUES" "(" <ValueList> ")" */
            case "INSERT":
                if (tokens.get(1).equalsIgnoreCase("INTO") && tokens.get(3).equalsIgnoreCase("VALUES") && tokens.size() >= 8) {
                    tableName = tokens.get(2);
                    List<String> values = handler.extractValuesFromParenthesis(tokens);
                    return insertInto(tableName, values);
                }
                break;

            /* "SELECT " <WildAttribList> " FROM " [TableName]
             | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition> */
            case "SELECT":
                tableName = handler.extractTableNameFromSelect(tokens);
                List<String> columnNames = handler.extractColumnsFromSelect(tokens);
                whereClause = handler.extractWhereClause(tokens);
                return selectFrom(tableName, columnNames, whereClause);


            /* "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>  */
            case "UPDATE":
                tableName = tokens.get(1);
                ArrayList<String> setClause = handler.extractSetClauseFromUpdate(tokens);
                whereClause = handler.extractWhereClause(tokens);
                return updateTable(tableName, setClause, whereClause);

            /* "DELETE " "FROM " [TableName] " WHERE " <Condition>*/
            case "DELETE":
                if (tokens.get(1).equalsIgnoreCase("FROM")) {
                    tableName = tokens.get(2);
                    whereClause = handler.extractWhereClause(tokens);
                    return deleteFrom(tableName, whereClause);
                }
                break;

            /* "JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName] */
            case "JOIN":
                String firstTable = tokens.get(1).toLowerCase();
                String secondTable = tokens.get(3).toLowerCase();
                String firstAttributeName = tokens.get(5).toLowerCase();
                String secondAttributeName = tokens.get(7).toLowerCase();
                return joinTables(firstTable, secondTable, firstAttributeName, secondAttributeName);

                /* "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
               <AlterationType>  ::=  "ADD" | "DROP" */
            case "ALTER":
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

        if (Files.exists(DatabasePath)){
            currentDatabasePath = DatabasePath;
            currentDatabase = databases.get(databaseName.toLowerCase());

            /* the DBServer is restart */
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
        if (reservedWordsDetector.isReservedWord(tableName)) {
            return "[ERROR]: Invalid database name";
        }

        Path tablePath = getTablePath(tableName.toLowerCase());
        // Check if the table has already exist
        if (Files.exists(tablePath)) {
            return "[ERROR]: Table '" + tableName + "' already exists.";
        }
        /* Any database/table names provided by the user should be
           converted into lowercase before saving out to the filesystem */
        Table table = new Table(tableName.toLowerCase(), columnNames);
        table.tablePath = tablePath;
        currentDatabase.addTable(table);
        table.updateTableFile();
        return "[OK]";
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
                if (table.getColumnNames().size() - 1 < values.size()) {
                    return "[ERROR]: Insert values exceed attributes.";
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

    public String selectFrom(String tableName, List<String> columnNames, ArrayList<String> whereClause) {
        if (currentDatabase != null){
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                List<Row> rowsToPrint;

                if (whereClause != null && !whereClause.isEmpty()) {
                    rowsToPrint = table.selectRowsWithCondition(whereClause);
                } else {
                    rowsToPrint = table.getRows(); // If there is no where clause, select all rows
                }

                if (columnNames.contains("*")) {
                    //Print all columns for the selected rows
                    return "[OK]" + "\n" + table.returnSelectedRows(rowsToPrint, table.getColumnNames());
                } else {
                    for (String queryAttribute : columnNames) {
                        if (!table.getColumnNames().contains(queryAttribute)) {
                            return "[ERROR]: Attribute does not exist ";
                        }
                    }
                    //Print only specified columns for the selected rows
                    return "[OK]" + "\n" + table.returnSelectedRows(rowsToPrint, columnNames);
                }
            } else {
                return "[ERROR]: Table '" + tableName + "' does not exist.";
            }
        } else {
            return "[ERROR]: No database selected.";
        }
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
        if (databases.containsKey(databaseName.toLowerCase())) {
            deleteDirectoryRecursively(databasePath); // Recursively delete all files and the directory
            databases.remove(databaseName.toLowerCase());
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
                currentDatabase.getTable(tableName).deleteTableFile();
                currentDatabase.dropTable(tableName.toLowerCase());
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

    public String alterTableDropColumn(String tableName, String columnName) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                /* Detect if the table contains the column to be dropped */
                if (!table.getColumnNames().contains(columnName)) {
                    return "[ERROR]: Column does not exist ";
                }

                table.dropColumn(columnName);
                table.updateTableFile();
                return "[OK]";
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
                StringBuilder columnBuilder = new StringBuilder();
                /* attribute names are prepended with name of table from which they originated */
                columnBuilder.append(firstTableName).append(".").append(firstColumnName);
                columnNames.add(columnBuilder.toString());
            }
            for (String secondColumnName : secondColumnNames){
                StringBuilder columnBuilder = new StringBuilder();
                columnBuilder.append(secondTableName).append(".").append(secondColumnName);
                columnNames.add(columnBuilder.toString());
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
}
