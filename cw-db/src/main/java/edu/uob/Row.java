package edu.uob;

import java.util.HashMap;
import java.util.Map;

public class Row {
    private Map<String, Object> values;

    public Row() {
        this.values = new HashMap<>();
    }

    public void setValues(String columnName, Object value) {
        values.put(columnName, value);
    }

}
