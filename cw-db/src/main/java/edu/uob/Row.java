package edu.uob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Row {
    private int id;
    private Map<String, String> valuesMap = new HashMap<>();

    public Row(int id, List<String> attributeNames, List<String> values) {
        this.id = id;
        for (int i = 0; i <attributeNames.size(); i++) {
            if (i < values.size()) {
                valuesMap.put(attributeNames.get(i), values.get(i));
            } else {
                valuesMap.put(attributeNames.get(i), null);
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getValue(String attributeName) {
        return valuesMap.get(attributeName);
    }

    public void updateValue(String attributeName, String value) {
        valuesMap.put(attributeName, value);
    }

}
