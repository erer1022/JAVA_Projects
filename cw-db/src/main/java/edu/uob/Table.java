package edu.uob;

import java.lang.reflect.Array;
import java.util.*;

public class Table {
    private String name;
    private List<Column> columns;
    private List<Row> rows;
    private int nextRowId;

    public Table(String name, List<String> columnNames) {
        this.name = name.toLowerCase();  // Convert to lowercase for case insensitivity
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
        this.nextRowId = 1;  //Initialize the ID
        for (String columnName : columnNames) {
            this.columns.add(new Column(columnName));
        }
    }

    public void insertRow(List<String> values) {
        if (values.size() != columns.size()){
            System.out.println("Invalid number of values.");
            return;
        }
        List<String> columnNames = this.getColumnNames();
        rows.add(new Row(nextRowId++, columnNames, values));
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
        return filteredRows ;
    }

    public void printSelectedRows(List<Row> rows, List<String> columnNames) {
        for (Row row : rows) {
            for (String columnName : columnNames) {
                int columnIndex = getColumnIndex(columnName);
                if (columnIndex != -1) {
                    System.out.print(row.getValue(columnName) + " ");
                }
            }
        }
        System.out.println(); // Move to the next line after printing a row
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
