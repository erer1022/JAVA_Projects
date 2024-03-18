package edu.uob;

import java.util.HashSet;
import java.util.Set;

public class ReservedWordsDetector {

    private final Set<String> reservedWords;

    public ReservedWordsDetector() {
        reservedWords = new HashSet<>();
        reservedWords.add("use");
        reservedWords.add("create");
        reservedWords.add("drop");
        reservedWords.add("alter");
        reservedWords.add("insert");
        reservedWords.add("into");
        reservedWords.add("select");
        reservedWords.add("update");
        reservedWords.add("delete");
        reservedWords.add("join");
        reservedWords.add("database");
        reservedWords.add("table");
        reservedWords.add("*");
        reservedWords.add("where");
        reservedWords.add("on");
        reservedWords.add("like");
        reservedWords.add("set");
        reservedWords.add("add");
    }

    public boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }
}

