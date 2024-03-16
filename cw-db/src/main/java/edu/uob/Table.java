package edu.uob;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Table {
    private String name;
    private Path tablePath;
    private List<Column> columns;
    private List<Row> rows;
    private int nextRowId;

    public Table(String name, Path tablePath, List<String> columnNames) {
        this.name = name.toLowerCase();  // Convert to lowercase for case insensitivity
        this.tablePath = tablePath;
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.nextRowId = 1;  //Initialize the ID

        columns.add(new Column("id"));
        for (String columnName : columnNames) {
            this.columns.add(new Column(columnName));
        }
    }

    public String getName() {
        return name;
    }


    public List<String> getColumnNames(){
        List<String> columnNames = new ArrayList<>();
        for (Column column : columns) {
            columnNames.add(column.getName());
        }
        return columnNames;
    }

    public void addColumn(String columnName) {
        // Check if the column already exists
        for (Column column : columns) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                throw new IllegalArgumentException("Column " + columnName + " already exists.");
            }
        }
        columns.add(new Column(columnName));
    }

    public void dropColumn(String columnName) {
        // Find the column to remove
        Column toRemove = null;
        for (Column column : columns) {
            if (column.getName().equalsIgnoreCase(columnName)) {
                toRemove = column;
                break;
            }
        }
        // If column exists, remove it
        if (toRemove != null) {
            columns.remove(toRemove);
            for (Row row : rows) {
                row.dropValue(columnName);
            }
        } else {
            throw new IllegalArgumentException("Column " + columnName + " does not exist.");
        }
    }


    public void insertRow(List<String> values) throws IOException {
        //Add to the Data structure
        List<String> columnNames = this.getColumnNames();
        Row row = new Row(nextRowId, columnNames, values);
        rows.add(row);
        nextRowId++;
    }

    public void updateTableFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tablePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            // Write column headers
            for (Column column : columns) {
                writer.write(column.getName() + whitespace(column.getName()));

            }
            writer.newLine(); // End the line for column headers

            // Write rows
            if (rows != null) {
                for (Row row : rows) {
                    for (Column column : columns) {
                        writer.write(row.getValue(column.getName()) + whitespace(row.getValue(column.getName())));
                    }
                    writer.newLine(); // End the line for each row
                }
            }
        } // Try-with-resources ensures that the writer is closed properly, even if an exception is thrown
    }

    private String whitespace(String value) {
        StringBuilder whitespace = new StringBuilder();
        int length = value.length();
        while(length < 8) {
            whitespace.append(" ");
            length++;
        }
        return whitespace.toString();
    }

    public void deleteTableFile() {
        try {
            Files.deleteIfExists(tablePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public List<Row> getRows(){
        return rows;
    }
    public List<Row> selectRowsWithCondition(ArrayList<String> whereClause) {
        LogicalExpression conditions = LogicalExpression.parseConditions(whereClause);

        List<Row> filteredRows = new ArrayList<>();
        for (Row row : rows) {
            if (conditions.evaluate(row)) {
                filteredRows.add(row);
            }
        }
        return filteredRows;
    }

    public String returnSelectedRows(List<Row> rows, List<String> columnNames) {
        StringBuilder result = new StringBuilder();

        for (String columnName : columnNames) {
            result.append(columnName).append(whitespace(columnName));
        }
        result.append("\n");

        for (Row row : rows) {
            for (String columnName : columnNames) {
                int columnIndex = getColumnIndex(columnName);
                if (columnIndex != -1) {
                    result.append(row.getValue(columnName)).append(whitespace(row.getValue(columnName)));
                }
            }
            result.append("\n"); // Move to the next line after processing a row
        }
        return result.toString();
    }

    private int getColumnIndex(String columnName) {
        for(int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }

    public void updateRowsWithCondition(ArrayList<String> setClause, ArrayList<String> whereClause) {
        List<Row> rowsToUpdate = selectRowsWithCondition(whereClause);

        for (Row row : rowsToUpdate) {
            for (int i = 0; i < setClause.size(); i += 4) {
                /* <NameValueList>   ::=  <NameValuePair> | <NameValuePair> "," <NameValueList>
                   <NameValuePair>   ::=  [AttributeName] "=" [Value] */

                String columnName = setClause.get(i);
                String value = setClause.get(i + 2);
                row.updateValue(columnName, value);
            }
        }
    }

    public void deleteRowsWithCondition(ArrayList<String> whereClause) {
        List<Row> rowsToDelete = selectRowsWithCondition(whereClause);

        Iterator<Row> iterator = rows.iterator();
        while(iterator.hasNext()) {
            Row currentRow = iterator.next();
            if (rowsToDelete.contains(currentRow)) {
                iterator.remove();
            }
        }
    }

        /* public void readTableFromFile(String storageFolderPath, String fileName){
        String name = storageFolderPath + File.separator + fileName;
        File fileToOpen = new File(name);
        try(FileReader reader = new FileReader(fileToOpen)) {
            BufferedReader buffReader = new BufferedReader(reader);
            readAllLines(buffReader);
        } catch (IOException e) {
            // Handling possible IO exceptions like file not found
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }
    } */

    /* public void readAllLines(BufferedReader reader) throws IOException {
        String currentLine; */

    /* Read the first title line */
        /*if ((currentLine = reader.readLine()) != null) {
            columnNames = Arrays.asList(currentLine.split("\\t"));  // Assuming tab-delimited file

            for (String columnName : columnNames) {
                columnTypes.put(columnName, String.class);  // Initialize as String
            }
        } */

    /* for the rest of the lines */
        /*while((currentLine = reader.readLine()) != null) {
            String[] values = currentLine.split("\\t");
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < values.length; i++) {
                row.put("id", nextId++);   //Automatically increment the id

                String value = values[i];
                String columnName = columnNames.get(i);
                Object typedValue = determineTypeAndConvert(value);
                row.put(columnName, typedValue);       //Map(String, Object)

                //Update column type
                Class<?> valueType = typedValue.getClass();
                if (valueType != String.class && columnTypes.get(columnName) == String.class){
                    columnTypes.put(columnName, valueType);
                }
            }
            rows.add(row);  //List<Map<String, Object>>
        }
    } */

    /* private Object determineTypeAndConvert(String value){
        if (value.matches("\\d+")) {
            return Integer.parseInt(value);
        } else if (value.matches("\\d+\\.\\d+")) {
            return Double.parseDouble(value);
        } else {
            return value;
        }
    } */

}
