package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class Condition implements Expression {
    String attributeName;
    String operator;
    String value;

    public Condition(String attributeName, String operator, String value) {
        this.attributeName = attributeName;
        this.operator = operator;
        this.value = value;
    }


    @Override
    public boolean evaluate(Row row) {
        String rowValue = row.getValue(attributeName);

        switch (operator) {
            case "==":
                return rowValue.equals(value);
            case ">":
                return Double.compare(Double.parseDouble(rowValue), Double.parseDouble(value)) > 0;
            case "<":
                return Double.compare(Double.parseDouble(rowValue), Double.parseDouble(value)) < 0;
            case ">=":
                return Double.compare(Double.parseDouble(rowValue), Double.parseDouble(value)) >= 0;
            case "<=":
                return Double.compare(Double.parseDouble(rowValue), Double.parseDouble(value)) <= 0;
            case "!=":
                return !rowValue.equals(value);
            case "LIKE":
                return rowValue.contains(value); // Simplified version of LIKE, matches if rowValue contains the condition's value
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
}