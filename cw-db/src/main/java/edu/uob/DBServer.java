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
                return useDatabase(tokens.get(1));

            /* <Create>          ::=  <CreateDatabase> | <CreateTable>
               <CreateDatabase>  ::=  "CREATE " "DATABASE " [DatabaseName]
               <CreateTable>     ::=  "CREATE " "TABLE " [TableName] | "CREATE " "TABLE " [TableName] "(" <AttributeList> ")"
               [AttributeName] | [AttributeName] "," <AttributeList> */
            case "CREATE":
                //Create DATABASE
                if (tokens.get(1).equalsIgnoreCase("DATABASE") && tokens.size() == 4) {
                    return createDatabase(tokens.get(2));
                }

                //CREATE TABLE
                else if (tokens.get(1).equalsIgnoreCase("TABLE")) {
                    tableName = tokens.get(2);
                    List<String> columns = extractValuesFromParenthesis(tokens);
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
                if (tokens.get(1).equalsIgnoreCase("INTO") && tokens.size() > 3) {
                    tableName = tokens.get(2);
                    List<String> values = extractValuesFromParenthesis(tokens);
                    return insertInto(tableName, values);
                }
                break;

            /* "SELECT " <WildAttribList> " FROM " [TableName]
             | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition> */
            case "SELECT":
                tableName = extractTableNameFromSelect(tokens);
                List<String> columnNames = extractColumnsFromSelect(tokens);
                whereClause = extractWhereClause(tokens);
                return selectFrom(tableName, columnNames, whereClause);


            /* "UPDATE " [TableName] " SET " <NameValueList> " WHERE " <Condition>  */
            case "UPDATE":
                tableName = tokens.get(1);
                ArrayList<String> setClause = extractSetClauseFromUpdate(tokens);
                whereClause = extractWhereClause(tokens);
                return updateTable(tableName, setClause, whereClause);

            /* "DELETE " "FROM " [TableName] " WHERE " <Condition>*/
            case "DELETE":
                if (tokens.get(1).equalsIgnoreCase("FROM")) {
                    tableName = tokens.get(2);
                    whereClause = extractWhereClause(tokens);
                    return deleteFrom(tableName, whereClause);
                }
                break;

            /* "JOIN " [TableName] " AND " [TableName] " ON " [AttributeName] " AND " [AttributeName] */
            /*case "JOIN":
                String firstTable = tokens.get(1);
                String secondTable = tokens.get(3);
                String firstAttributeName = tokens.get(5);
                String secondAttributeName = tokens.get(7);
                return joinTables(firstTable, secondTable, firstAttributeName, secondAttributeName);
                break;*/

                /* "ALTER " "TABLE " [TableName] " " <AlterationType> " " [AttributeName]
               <AlterationType>  ::=  "ADD" | "DROP" */
            case "ALTER":
                if (tokens.get(1).equalsIgnoreCase("TABLE") && tokens.get(3).equalsIgnoreCase("ADD")) {
                    tableName = tokens.get(2);
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
        Path newDatabasePath = Paths.get(storageFolderPath, databaseName.toLowerCase());
        if (Files.exists(newDatabasePath)){
            return "[ERROR]: Database '" + databaseName + "' already exists.";
        } else {
            Files.createDirectories(Paths.get(newDatabasePath.toString()));
            Database newDatabase = new Database(databaseName.toLowerCase());
            databases.put(databaseName.toLowerCase(), newDatabase);
            return "[OK]";
        }
    }

    public String useDatabase(String databaseName) {
        Path DatabasePath = Paths.get(storageFolderPath, databaseName);

        if (Files.exists(DatabasePath)){
            this.currentDatabasePath = DatabasePath;
            this.currentDatabase = databases.get(databaseName.toLowerCase());
            return "[OK]";
        } else {
            return "[ERROR]: Database '" + databaseName + "' does not exist.";
        }
    }

    private Path getTablePath(String tableName) {
        return this.currentDatabasePath.resolve(tableName.toLowerCase() + ".tab");
    }

    public String createTable(String tableName, List<String> columnNames) throws IOException {
        Path tablePath = getTablePath(tableName.toLowerCase());

        // Check if the table has already exist
        if (Files.exists(tablePath)) {
            return "[ERROR]: Table '" + tableName + "' already exists.";
        }

        Table table = new Table(tableName.toLowerCase(), tablePath, columnNames);
        currentDatabase.addTable(table);
        table.updateTableFile();
        return "[OK]";
    }

    public String insertInto(String tableName, List<String> values) throws IOException {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);


            if (table != null){
                table.insertRow(values);
                table.updateTableFile();
                return "[OK]";
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

    private List<String> extractValuesFromParenthesis(ArrayList<String> tokens) {
        // Find the opening parenthesis to start of value list
        int startIndex = tokens.indexOf("(") + 1;
        int endIndex = tokens.indexOf(")");
        // Extract the subList containing the values, split by comma
        List<String> valueTokens = tokens.subList(startIndex, endIndex);
        List<String> cleanedTokens = new ArrayList<>();


        for (String token : valueTokens) {
            String cleanedToken = token.replace("'", "");
            cleanedTokens.add(cleanedToken);
            cleanedTokens.remove(",");
        }
        // Further processing may be needed if values contain commas, for example in strings
        return cleanedTokens;
    }

    /* "SELECT " <WildAttribList> " FROM " [TableName]
     | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition> */
    private String extractTableNameFromSelect(ArrayList<String> tokens) {
        // Assuming the token following "FROM" is always the table name
        int fromIndex = tokens.indexOf("FROM");
        // Handle the case where "FROM" is not found or is the last word without following table name
        if (fromIndex < 0 || fromIndex + 1 >= tokens.size()) {
            throw new IllegalStateException("Malformed SELECT query: 'FROM' clause is missing or incomplete.");
        }
        return tokens.get(fromIndex + 1);
    }

    private List<String> extractColumnsFromSelect(ArrayList<String> tokens) {
        // Assuming the columns are listed after "SELECT" and before "FROM"
        int selectIndex = tokens.indexOf("SELECT") + 1;
        int fromIndex = tokens.indexOf("FROM");
        if (fromIndex < 0 || selectIndex >= fromIndex) {
            throw new IllegalStateException("Malformed SELECT query: Columns section is missing or incomplete.");
        }
        // Join tokens to handle columns like "table.column" and split by comma
        String columnsCombined = String.join(" ", tokens.subList(selectIndex, fromIndex));
        String[] columns = columnsCombined.split(",");

        // Create a new list for trimmed column names
        List<String> trimmedColumns = new ArrayList<>();
        for (String column : columns) {
            trimmedColumns.add(column.trim());
        }
        return trimmedColumns;
    }


    /* <NameValueList>   ::=  <NameValuePair> | <NameValuePair> "," <NameValueList>
       <NameValuePair>   ::=  [AttributeName] "=" [Value] */
    private ArrayList<String> extractSetClauseFromUpdate(ArrayList<String> tokens) {
        // Find the indexes for "SET" and "WHERE" (if it exists)
        int setIndex = tokens.indexOf("SET") + 1;
        int whereIndex = tokens.indexOf("WHERE");
        if (whereIndex != -1 && setIndex >= whereIndex) {
            throw new IllegalStateException("Malformed UPDATE query: SET clause is missing or incomplete.");
        }
        // If there is a WHERE clause, the SET clause ends just before it
        // Otherwise, it goes to the end of the query
        int endIndex = (whereIndex != -1) ? whereIndex : tokens.size();
        // Join tokens to handle set clauses like "column = value" and split by comma
        return new ArrayList<>(tokens.subList(setIndex, endIndex));

    }

    /* " WHERE " <Condition>
     * <Condition>       ::=  "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> | "(" [AttributeName] <Comparator> [Value] ")" | [AttributeName] <Comparator> [Value]
       <BoolOperator>    ::= "AND" | "OR"
       <Comparator>      ::=  "==" | ">" | "<" | ">=" | "<=" | "!=" | " LIKE " */
    private ArrayList<String> extractWhereClause(ArrayList<String> tokens) {
        // Check if the WHERE clause exists
        int whereIndex = tokens.indexOf("WHERE");
        if (whereIndex == -1) {
            // No WHERE clause present
            return new ArrayList<>();
        }
        // Assuming WHERE clause is the last part of the query
        return new ArrayList<>(tokens.subList(whereIndex + 1, tokens.size()));
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

    /*public String joinTables(String firstTableName, String secondTableName, String firstAttribute, String SecondAttribute) {
        if (currentDatabase != null) {
            Table firstTable = currentDatabase.getTable(firstTableName);
            Table secondTable = currentDatabase.getTable(secondTableName);

            List<String> firstColumnNames = firstTable.getColumnNames();

            Table joinTable = new Table(joinTable, getTablePath(joinTable),
        }
    }*/
}
