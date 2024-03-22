package edu.uob;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Database {
    private String name;
    public HashMap<String, Table> tables;

    public Database(String name){
        this.name = name;
        tables = new HashMap<>();
    }

    public void addTable(Table table){
        tables.put(table.getName(), table);
    }

    public Table getTable(String tableName){
        return tables.get(tableName.toLowerCase());
    }

    public void dropTable(String tableName) {
        tableName = tableName.toLowerCase();
        tables.remove(tableName);
    }

    public void loadDatabase(String databaseName) {
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
                    /* Obtain the table's column name */
                    List<String> columnNames = new ArrayList<>(Arrays.asList(currentLine.split("\\s+")));
                    columnNames.remove("id");
                    /* Obtain the table's table name */
                    String tableName = fileName.replace(".tab", "");
                    /* Using constructor to generate a new table */
                    Table loadTable = new Table(tableName, columnNames);
                    loadTable.tablePath = Paths.get(fileName);
                    addTable(loadTable);
                    /* Read the other lines, obtain the value */
                    while ((currentLine = buffreader.readLine()) != null) {
                        List<String> rowValues = new ArrayList<>(Arrays.asList(currentLine.split("\\s+")));
                        /* remove the automate generated id */
                        rowValues.remove(0);
                        loadTable.insertRow(rowValues);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<String> getFileNamesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        List<String> fileNames = new ArrayList<>();

        // Get all the files and directories within the specified directory
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) { // Check if the File object is a file
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames;
    }
}
