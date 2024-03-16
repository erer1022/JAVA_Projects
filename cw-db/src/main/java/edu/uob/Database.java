package edu.uob;

import java.nio.file.Path;
import java.util.HashMap;

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
        if (tables.containsKey(tableName)) {
            tables.remove(tableName);
        } else {
        }
    }
}
