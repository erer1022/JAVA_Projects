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
        if (valuesMap.get(attributeName) != null) {
            return valuesMap.get(attributeName);
        } else {
            return "";
        }

    }

    public void updateValue(String attributeName, String value) {
        valuesMap.put(attributeName, value);
    }

    public void dropValue(String attributeName) {
        valuesMap.remove(attributeName);
    }

}
