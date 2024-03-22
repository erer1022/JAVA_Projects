package edu.uob;

public class Condition implements Expression {
    String attributeName;
    String operator;
    String value;

    public Condition(String attributeName, String operator, String value) {
        this.attributeName = attributeName;
        this.operator = operator;
        this.value = value.replace("'", "");
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
                return rowValue.contains(value);
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
}
