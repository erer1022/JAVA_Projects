package edu.uob;

import java.io.IOException;
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

    public void createTableFile() throws IOException {
        StringBuilder columnHeaders = new StringBuilder();
        for (Column column : columns) {
            columnHeaders.append(column.getName());
            columnHeaders.append("\t");
        }
        // Write the Column headers to the .tab file
        Files.writeString(tablePath, columnHeaders, StandardOpenOption.CREATE_NEW);
    }


    public List<String> getColumnNames(){
        List<String> columnNames = new ArrayList<>();
        for (Column column : columns) {
            columnNames.add(column.getName());
        }
        return columnNames;
    }


    public void insertRow(List<String> values) throws IOException {
        //Write to the .tab file
        String currentIdAsString = String.valueOf(nextRowId);
        values.add(0, currentIdAsString);
        StringBuilder insertRow = new StringBuilder();
        for (String value : values) {
            insertRow.append(value);
            insertRow.append("\t");
        }

        String fileContent = Files.readString(tablePath);
        String newContent = fileContent + System.lineSeparator() + insertRow;
        Files.writeString(tablePath, newContent, StandardOpenOption.TRUNCATE_EXISTING);

        //Add to the Data structure
        List<String> columnNames = this.getColumnNames();
        Row row = new Row(nextRowId, columnNames, values);
        rows.add(row);
        nextRowId++;
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
            result.append(columnName).append("\t");
        }
        result.append("\n");

        for (Row row : rows) {
            for (String columnName : columnNames) {
                int columnIndex = getColumnIndex(columnName);
                if (columnIndex != -1) {
                    result.append(row.getValue(columnName)).append("\t");
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

    /*public void updateRowsWithCondition(ArrayList<String> setClause, ArrayList<String> whereClause) {
        List<Row> rowsToUpdate = selectRowsWithCondition(whereClause);

        for (Row row : rowsToUpdate) {
            for (int i = 0; i < setClause.size(); i += 4) {
                /* <NameValueList>   ::=  <NameValuePair> | <NameValuePair> "," <NameValueList>
                   <NameValuePair>   ::=  [AttributeName] "=" [Value] */
                /*String columnName = setClause.get(i);
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
