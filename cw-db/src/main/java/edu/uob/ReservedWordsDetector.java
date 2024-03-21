package edu.uob;

import java.util.HashSet;
import java.util.Set;

public class ReservedWordsDetector {

    private final Set<String> reservedWords;

    public ReservedWordsDetector() {
        reservedWords = new HashSet<>();
        reservedWords.add("use");
        reservedWords.add("USE");
        reservedWords.add("create");
        reservedWords.add("CREATE");
        reservedWords.add("drop");
        reservedWords.add("DROP");
        reservedWords.add("alter");
        reservedWords.add("ALTER");
        reservedWords.add("insert");
        reservedWords.add("INSERT");
        reservedWords.add("into");
        reservedWords.add("INTO");
        reservedWords.add("select");
        reservedWords.add("SELECT");
        reservedWords.add("update");
        reservedWords.add("UPDATE");
        reservedWords.add("delete");
        reservedWords.add("DELETE");
        reservedWords.add("join");
        reservedWords.add("JOIN");
        reservedWords.add("database");
        reservedWords.add("DATABASE");
        reservedWords.add("table");
        reservedWords.add("TABLE");
        reservedWords.add("*");
        reservedWords.add("where");
        reservedWords.add("WHERE");
        reservedWords.add("on");
        reservedWords.add("ON");
        reservedWords.add("like");
        reservedWords.add("LIKE");
        reservedWords.add("set");
        reservedWords.add("SET");
        reservedWords.add("add");
        reservedWords.add("ADD");
        reservedWords.add("values");
        reservedWords.add("VALUES");
        reservedWords.add("from");
        reservedWords.add("FROM");
    }

    public boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }
}
