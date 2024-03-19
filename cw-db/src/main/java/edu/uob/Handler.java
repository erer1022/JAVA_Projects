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
    private ReservedWordsDetector reservedWordsDetector= new ReservedWordsDetector();

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

    public List<String> extractValuesFromParenthesis(ArrayList<String> tokens) {
        // Find the opening parenthesis to start of value list
        int startIndex = tokens.indexOf("(") + 1;
        int endIndex = tokens.indexOf(")");
        // Extract the subList containing the values, split by comma
        List<String> valueTokens = tokens.subList(startIndex, endIndex);
        List<String> cleanedTokens = new ArrayList<>();


        for (String token : valueTokens) {
            /* Detect reserved words */
            if (reservedWordsDetector.isReservedWord(token)) {
                cleanedTokens.add("[ERROR]: Using reserved words for token");
                return cleanedTokens;
            }
            String cleanedToken = token.replace("'", "");
            cleanedTokens.add(cleanedToken);
            cleanedTokens.remove(",");
        }
        // Further processing may be needed if values contain commas, for example in strings
        return cleanedTokens;
    }

    /* "SELECT " <WildAttribList> " FROM " [TableName]
     | "SELECT " <WildAttribList> " FROM " [TableName] " WHERE " <Condition> */
    public String extractTableNameFromSelect(ArrayList<String> tokens) {
        // Assuming the token following "FROM" is always the table name
        int fromIndex = tokens.indexOf("FROM");
        // Handle the case where "FROM" is not found or is the last word without following table name
        if (fromIndex < 0 || fromIndex + 1 >= tokens.size()) {
            return "[ERROR]: Attribute does not exist";
        }
        return tokens.get(fromIndex + 1);
    }

    public List<String> extractColumnsFromSelect(ArrayList<String> tokens) {
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
    public ArrayList<String> extractSetClauseFromUpdate(ArrayList<String> tokens) {
        // Find the indexes for "SET" and "WHERE" (if it exists)
        int setIndex = tokens.indexOf("SET") + 1;
        int whereIndex = tokens.indexOf("WHERE");

        if (whereIndex != -1 && setIndex >= whereIndex) {
            List<String> setClause = new ArrayList<>();
            setClause.add("[ERROR]: SET clause is missing or incomplete.");
        }
        // If there is a WHERE clause, the SET clause ends just before it
        // Otherwise, it goes to the end of the query
        int endIndex = (whereIndex != -1) ? whereIndex : tokens.size();
        return new ArrayList<>(tokens.subList(setIndex, endIndex));
    }

    /* " WHERE " <Condition>
     * <Condition>       ::=  "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> | "(" [AttributeName] <Comparator> [Value] ")" | [AttributeName] <Comparator> [Value]
       <BoolOperator>    ::= "AND" | "OR"
       <Comparator>      ::=  "==" | ">" | "<" | ">=" | "<=" | "!=" | " LIKE " */
    public ArrayList<String> extractWhereClause(ArrayList<String> tokens) {
        // Check if the WHERE clause exists
        int whereIndex = tokens.indexOf("WHERE");
        if (whereIndex == -1) {
            // No WHERE clause present
            return new ArrayList<>();
        }
        // Extract the WHERE clause, assuming it is the last part of the query
        List<String> whereClauseTokens = new ArrayList<>(tokens.subList(whereIndex + 1, tokens.size()));

        // Initialize a new list to hold the modified tokens
        ArrayList<String> modifiedTokens = new ArrayList<>();

        // Iterate through the whereClauseTokens
        for (int i = 0; i < whereClauseTokens.size(); i++) {
            String currentToken = whereClauseTokens.get(i);
            // Check if the current token is ">" or "<" and the next token is "=", then combine them
            if (i < whereClauseTokens.size() - 1) { // Ensure there is a next token
                String nextToken = whereClauseTokens.get(i + 1);
                if ((">".equals(currentToken) || "<".equals(currentToken)) && "=".equals(nextToken)) {
                    modifiedTokens.add(currentToken + "="); // Combine ">" or "<" with "="
                    i++; // Skip the next token since it's combined with the current one
                    continue;
                }
            }
            // Add the current token to modifiedTokens if not combined
            modifiedTokens.add(currentToken);
        }

        return modifiedTokens;
    }

}

