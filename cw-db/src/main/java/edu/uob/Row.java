package edu.uob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Row {
    private int id;
    private Map<String, String> valuesMap = new HashMap<>();

    public Row(int id, List<String> attributeNames, List<String> values) {
        this.id = id;
        valuesMap.put("id", Integer.toString(id));

        for (int i = 1; i <attributeNames.size(); i++) {
            if (i <= values.size()) {
                valuesMap.put(attributeNames.get(i), values.get(i-1));
            } else {
                valuesMap.put(attributeNames.get(i), null);
            }
        }
    }

    public String getId() {
        return Integer.toString(id);
    }

    public String getValue(String attributeName) {
        for (Map.Entry<String, String> entry : valuesMap.entrySet()) {
            // Check if the entry's key matches attributeName, ignoring case differences
            if (entry.getKey().equalsIgnoreCase(attributeName)) {
                return entry.getValue();
            }
        }
        // If no matching key is found, return an empty string
        return "";
    }


    public void updateValue(String attributeName, String value) {
        valuesMap.put(attributeName, value);
    }

    public void dropValue(String attributeName) {
        valuesMap.remove(attributeName);
    }
}
