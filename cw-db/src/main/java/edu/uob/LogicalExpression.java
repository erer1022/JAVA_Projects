package edu.uob;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class LogicalExpression implements Expression{
    List<Expression> expressions; // Can contain Condition or LogicalExpression objects
    String operator; // "AND" or "OR"

    LogicalExpression(String operator) {
        this.expressions = new ArrayList<>();
        this.operator = operator;
    }

    public void addExpression(Expression expression) {
        expressions.add(expression);
    }

    @Override
    public boolean evaluate(Row row) {
        boolean result = "OR".equals(operator) ? false : true;

        for (Expression expr : expressions) {
            boolean exprResult;
            exprResult = expr.evaluate(row);

            if ("AND".equals(operator)) {
                result = result && exprResult;
            } else { //"OR"
                result = result || exprResult;
            }
        }
        return result;
    }

    /* <Condition>       ::=  "(" <Condition> <BoolOperator> <Condition> ")" | <Condition> <BoolOperator> <Condition> | "(" [AttributeName] <Comparator> [Value] ")" | [AttributeName] <Comparator> [Value]
       <BoolOperator>    ::= "AND" | "OR"
       <Comparator>      ::=  "==" | ">" | "<" | ">=" | "<=" | "!=" | " LIKE " */

    /*  (pass == FALSE) AND (mark > 35); */

    public static LogicalExpression parseConditions(ArrayList<String> tokens) {
        Stack<LogicalExpression> stack = new Stack<>();
        // Initialize the root expression with a neutral operator like "AND".
        LogicalExpression rootExpression = new LogicalExpression("AND");
        stack.push(rootExpression);

        ArrayList<String> conditionParts = new ArrayList<>();

        for (String token : tokens) {
            switch (token.toUpperCase()) {
                case "AND":
                case "OR":
                    LogicalExpression newExpr = new LogicalExpression(token);
                    // Always add newExpr as a component to the current top expression on the stack.
                    stack.peek().addExpression(newExpr);
                    // Push newExpr onto the stack to start a new clause.
                    stack.push(newExpr);
                    break;
                case "(":
                    // Start a new logical expression with the default "AND" which will be changed if needed.
                    LogicalExpression newLogicalExpr = new LogicalExpression("AND");
                    stack.push(newLogicalExpr);
                    break;
                case ")":
                    // Pops the current logical expression as it's completed.
                    LogicalExpression finishedExpr = stack.pop();
                    // If there's another logical expression underneath, add the completed one to it.
                    if (!stack.isEmpty()) {
                        stack.peek().addExpression(finishedExpr);
                    }
                    break;
                default:
                    // Non-operator tokens should be part of a condition.
                    conditionParts.add(token);
                    // When we have three parts, we have a full condition.
                    if (conditionParts.size() == 3) {
                        Condition condition = new Condition(conditionParts.get(0), conditionParts.get(1), conditionParts.get(2));
                        stack.peek().addExpression(condition);
                        conditionParts.clear();
                    }
                    break;
            }
        }
        // The rootExpression is already at the base of the stack and thus does not need to be popped again.
        return rootExpression;
    }


}
