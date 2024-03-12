package edu.uob;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Table {
    private List<String> columnNames;
    private List<Map<String, Object>> rows;
    private Map<String, Class<?>> columnTypes;
    private String name;
    private Path filepath;
    private int nextId;

    public Table(String name, List<String> columns) {
        this.name = name.toLowerCase();  // Convert to lowercase for case insensitivity
        this.nextId = 1;
        this.columnNames = columns;
        this.rows = new ArrayList<>();
        this.columnTypes = new HashMap<>();
    }

    public void readTableFromFile(String storageFolderPath, String fileName){
        String name = storageFolderPath + File.separator + fileName;
        File fileToOpen = new File(name);
        try(FileReader reader = new FileReader(fileToOpen)) {
            BufferedReader buffReader = new BufferedReader(reader);
            readAllLines(buffReader);
        } catch (IOException e) {
            // Handling possible IO exceptions like file not found
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }
    }

    public void readAllLines(BufferedReader reader) throws IOException {
        String currentLine;

        /* Read the first title line */
        if ((currentLine = reader.readLine()) != null) {
            columnNames = Arrays.asList(currentLine.split("\\t"));  // Assuming tab-delimited file

            for (String columnName : columnNames) {
                columnTypes.put(columnName, String.class);  // Initialize as String
            }
        }

        /* for the rest of the lines */
        while((currentLine = reader.readLine()) != null) {
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
    }

    private Object determineTypeAndConvert(String value){
        if (value.matches("\\d+")) {
            return Integer.parseInt(value);
        } else if (value.matches("\\d+\\.\\d+")) {
            return Double.parseDouble(value);
        } else {
            return value;
        }
    }

    public String getName() {
        return name;
    }
    public List<String> getColumnNames(){
        return columnNames;
    }

    public List<Map<String, Object>> getRows(){
        return rows;
    }

    public Map<String, Class<?>> getColumnTypes(){
        return columnTypes;
    }

}
