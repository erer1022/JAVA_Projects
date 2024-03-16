package edu.uob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Handler {

    String[] specialCharacters = {"(",")",",",";","<",">"};
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
        //for (String token : tokens) System.out.println(token);
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





    /*public void dropDatabase(String databaseName) {
        if (databases.containsKey(databaseName.toLowerCase())) {
            databases.remove(databaseName.toLowerCase());
            System.out.println("Database " + databaseName + " dropped successfully.");
        } else {
            System.out.println("Database " + databaseName + " does not exist.");
        }
    }

    public void dropTable(String tableName) {
        if (this.currentDatabase != null) {
            this.currentDatabase.dropTable(tableName.toLowerCase());
        } else {
            System.out.println("No database selected.");
        }
    }*/



    /*public void updateTable(String tableName, ArrayList<String> setClause, ArrayList<String> whereClause) {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                table.updateRowsWithCondition(setClause, whereClause);
            } else {
                System.err.println("Table '" + tableName + "' does not exist in the current database.");
            }
        } else {
            System.err.println("No current database is selected.");
        }
    }

    public void deleteFrom(String tableName, ArrayList<String> whereClause) {
        if (currentDatabase != null) {
            Table table = currentDatabase.getTable(tableName);
            if (table != null) {
                table.deleteRowsWithCondition(whereClause);
            } else {
                System.err.println("Table '" + tableName + "' does not exist in the current database.");
            }
        } else {
            System.err.println("No current database is selected.");
        }
    }

    /* TODO:
    public void joinTables(String firstTableName, String secondTableName, String firstAttributeName, String secondAttributeName) {


    }*/

}

