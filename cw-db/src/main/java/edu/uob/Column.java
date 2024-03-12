package edu.uob;

public class Column {
    private String name;
    private Class<?> type;  /* needs to handle data of various types */

    public Column(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }


    public String getColumnName(){
        return name;
    }

    public Class<?> getType(){
        return type;
    }
}
