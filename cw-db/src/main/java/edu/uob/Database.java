package edu.uob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Database {
    private String name;
    private Path path;
    private HashMap<String, Table> tables;

    public Database(String name){
        this.name = name;
        tables = new HashMap<>();
    }

    public void addTable(Table table){
        if (tables.containsKey(table.getName())) {
            throw new IllegalArgumentException("A table with the name '" + table.getName() + "' already exists.");
        }
        tables.put(table.getName(), table);
    }

    public Table getTable(String tableName){
        return tables.get(tableName.toLowerCase());
    }


    public void dropTable(String tableName) {
        tableName = tableName.toLowerCase();
        tables.remove(tableName);
    }

    public void loadDatabase(String databaseName){
        String storageFolderPath = Paths.get("databases" + File.separator + databaseName).toAbsolutePath().toString();
        List<String> fileNames = getFileNamesInDirectory(storageFolderPath);

        for (String fileName : fileNames) {
            /* load each file in the database */
            String name = storageFolderPath + File.separator + fileName;
            File fileToOpen = new File(name);
            try(FileReader reader = new FileReader(fileToOpen)) {
                BufferedReader buffreader = new BufferedReader(reader);
                String currentLine;

                /* Read the first title line */
                if ((currentLine = buffreader.readLine()) != null) {
                    List<String> columnNames = new ArrayList<>(Arrays.asList(currentLine.split("\\s+")));
                    columnNames.remove("id");

                    String tableName = fileName.replace(".tab", "");
                    Table loadTable = new Table(tableName, columnNames);
                    loadTable.tablePath = Paths.get(fileName);
                    addTable(loadTable);

                    while ((currentLine = buffreader.readLine()) != null) {
                        List<String> rowValues = new ArrayList<>(Arrays.asList(currentLine.split("\\s+")));
                        rowValues.remove(0);
                        loadTable.insertRow(rowValues);
                    }
                }

            } catch (IOException e) {
                // Handling possible IO exceptions like file not found
                System.err.println("An error occurred while reading the file: " + e.getMessage());
            }
        }
    }

    public List<String> getFileNamesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        List<String> fileNames = new ArrayList<>();

        // Get all the files and directories within the specified directory
        File[] files = directory.listFiles();

        if (files != null) { // Ensure the directory is not empty
            for (File file : files) {
                if (file.isFile()) { // Check if the File object is a file
                    fileNames.add(file.getName()); // Add the file name to the list
                }
            }
        }
        return fileNames; // Return the list of file names
    }
}
