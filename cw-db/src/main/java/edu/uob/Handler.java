package edu.uob;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Handler {

    private HashMap<String, Database> databases = new HashMap<>();
    private Database currentDatabase = null;

    String[] specialCharacters = {"(",")",",",";"};
    ArrayList<String> tokens = new ArrayList<String>();

    public ArrayList<String> preprocessQuery(String query)
    {
            // Remove any whitespace at the beginning and end of the query
        query = query.trim();
            // Split the query on single quotes (to separate out query characters from string literals)
        String[] fragments = query.split("'");
        for (int i=0; i<fragments.length; i++) {
                // Every odd fragment is a string literal, so just append it without any alterations
            if (i%2 != 0) tokens.add("'" + fragments[i] + "'");
                    // If it's not a string literal, it must be query characters (which need further processing)
            else {
                    // Tokenize the fragments into an array of strings
                String[] nextBatchOfTokens = tokenize(fragments[i]);
                    // Then add these to the "result" array list (needs a bit of conversion)
                tokens.addAll(Arrays.asList(nextBatchOfTokens));
            }
        }
            // Finally, loop through the result array list, printing out each token a line at a time
        for (String token : tokens) System.out.println(token);
        return tokens;
    }

    public String[] tokenize(String input)
    {
        // Add in some extra padding spaces around the "special characters"
        // so we can be sure that they are separated by AT LEAST one space (possibly more)
        for(int i=0; i<specialCharacters.length ;i++) {
            input = input.replace(specialCharacters[i], " " + specialCharacters[i] + " ");
        }
        // Remove all double spaces (the previous replacements may had added some)
        // This is "blind" replacement - replacing if they exist, doing nothing if they don't
        while (input.contains("  ")) input = input.replaceAll("  ", " ");
        // Again, remove any whitespace from the beginning and end that might have been introduced
        input = input.trim();
        // Finally split on the space char (since there will now ALWAYS be a space between tokens)
        return input.split(" ");
    }

    public void createDatabase(String databaseName){
        if (databases.containsKey(databaseName)){
            throw new IllegalStateException("Database already exists.");
        } else {
            Database newDatabase = new Database(databaseName);
            databases.put(databaseName.toLowerCase(), newDatabase);
            System.out.println("[OK]");
        }
    }

    public void useDatabase(String databaseName) {
        if (databases.containsKey(databaseName.toLowerCase())){
            currentDatabase = databases.get(databaseName.toLowerCase());
            System.out.println("[OK]");
        } else {
            System.out.println("Database '" + databaseName + "' does not exist.");
        }
    }

    public void createTable(String tableName, List<String> columns){
        if (currentDatabase != null) {
            Table newTable = new Table(tableName.toLowerCase(), columns);
            currentDatabase.addTable(newTable);
            System.out.println("[OK]");
        } else {
            System.out.println("No database selected.");
        }
    }

    public void dropTable(String tableName) {
        if (this.currentDatabase != null) {
            this.currentDatabase.dropTable(tableName);
        } else {
            System.out.println("No database selected.");
        }
    }

    public void dropDatabase(String databaseName) {
        if (databases.containsKey(databaseName.toLowerCase())) {
            databases.remove(databaseName.toLowerCase());
            System.out.println("Database " + databaseName + " dropped successfully.");
        } else {
            System.out.println("Database " + databaseName + " does not exist.");
        }
    }






    public void insertInto(String tableName, List<String> values){
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null){
                table.addRow(values);
                System.out.println("[OK]");
            } else {
                System.out.println("Table '" + tableName + "' does not exist.");
            }
        } else {
            System.out.println("No database selected.");
        }
    }

    public void selectFrom(String tableName) {
        if (currentDatabase != null){
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                table.printRows();
            } else {
                System.out.println("Table '" + tableName + "' does not exist.");
            }
        } else {
            System.out.println("No database selected.");
        }
    }

}

