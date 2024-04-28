package edu.uob;

import java.util.List;
import java.util.Objects;

public class GameAction {
    private List<String> triggers;   // List of triggers keyphrases
    private List<String> subjects;   // List of subject entities
    private List<String> consumed;   // List of consumed entities
    private List<String> produced;   // List of produced entities
    private String narration;        // Action description

    public GameAction(List<String> triggers, List<String> subjects, List<String> consumed, List<String> produced, String narration) {
        this.triggers = triggers;
        this.subjects = subjects;
        this.consumed = consumed;
        this.produced = produced;
        this.narration = narration;
    }

    public List<String> getTriggers() {
        return triggers;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public List<String> getConsumed() {
        return consumed;
    }

    public List<String> getProduced() {
        return produced;
    }

    public String getNarration() {
        return narration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameAction that = (GameAction) o;
        return Objects.equals(triggers, that.triggers) &&
                Objects.equals(subjects, that.subjects) &&
                Objects.equals(consumed, that.consumed) &&
                Objects.equals(produced, that.produced) &&
                Objects.equals(narration, that.narration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggers, subjects, consumed, produced, narration);
    }

    /* Use to print the GameAction Data structure
    @Override
    public String toString() {
        return "GameAction{" +
                "triggers=" + triggers +
                ", subjects=" + subjects +
                ", consumed=" + consumed +
                ", produced=" + produced +
                ", narration='" + narration + '\'' +
                '}';
    }
    */


}
