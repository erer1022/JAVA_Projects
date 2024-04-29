package edu.uob;

public class LocationPath {
    private String fromName, toName;
    public LocationPath(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }

    public String getDescription() {
        return "Here's the path: " + fromName + " -> " + toName;
    }

}
