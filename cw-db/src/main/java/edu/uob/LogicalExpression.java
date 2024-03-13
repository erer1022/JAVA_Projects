package edu.uob;

import com.beust.ah.A;

import java.util.List;
import java.util.ArrayList;
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
            if (expr instanceof Condition) {
                exprResult = expr.evaluate(row);
            } else { // LogicalExpression
                exprResult = expr.evaluate(row);
            }

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
        Stack<Expression> stack = new Stack<>();
        // Create an initial LogicalExpression with a neutral operator like "AND".
        LogicalExpression currentExpression = new LogicalExpression("AND");
        stack.push(currentExpression);

        ArrayList<String> conditionParts = new ArrayList<>();

        for (String token : tokens) {
            switch (token.toUpperCase()) {
                case "AND":
                case "OR":
                    LogicalExpression newExpr = new LogicalExpression(token);
                    // Add newExpr as a component to the expression before the top expression on the stack
                    if (stack.size() > 1) {
                        ((LogicalExpression) stack.get(stack.size() - 2)).addExpression(newExpr);
                    }
                    // Push newExpr onto the stack to start a new clause
                    stack.push(newExpr);
                    currentExpression = newExpr;
                    break;
                case "(":
                    // Start a new logical expression with the default "AND" which will be changed if needed
                    currentExpression = new LogicalExpression("AND");
                    stack.push(currentExpression);
                    break;
                case ")":
                    // Pops the current logical expression as it's completed
                    currentExpression = (LogicalExpression) stack.pop();
                    // If there's another logical expression underneath, add the completed one to it
                    if (!stack.isEmpty()) {
                        ((LogicalExpression) stack.peek()).addExpression(currentExpression);
                    }
                    break;
                default:
                    // Non-operator tokens should be part of a condition
                    conditionParts.add(token);
                    // When we have three parts, we have a full condition
                    if (conditionParts.size() == 3) {
                        Condition condition = new Condition(conditionParts.get(0), conditionParts.get(1), conditionParts.get(2));
                        currentExpression.addExpression(condition);
                        conditionParts.clear();
                    }
                    break;
            }
        }
        // At the end, the stack should only contain the root of the expression tree
        return (LogicalExpression) stack.pop();
    }

}
