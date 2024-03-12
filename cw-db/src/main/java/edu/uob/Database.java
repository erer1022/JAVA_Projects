package edu.uob;

import java.util.HashMap;

public class Database {
    private String name;
    private HashMap<String, Table> tables;

    public Database(String name){
        this.name = name;
        tables = new HashMap<>();
    }

    public Table getTable(String tableName){
        return tables.get(tableName);
    }
    public void addTable(Table table){
        if (tables.containsKey(table.getName())) {
            throw new IllegalArgumentException("A table with the name '" + table.getName() + "' already exists.");
        }
        tables.put(table.getName(), table);
    }

    public void dropTable(String tableName) {
        if (this.tables.containsKey(tableName.toLowerCase())) {
            this.tables.remove(tableName.toLowerCase());
            System.out.println("Table " + tableName + " dropped successfully.");
        } else {
            System.out.println("Table " + tableName + " does not exist.");
        }
    }
}
