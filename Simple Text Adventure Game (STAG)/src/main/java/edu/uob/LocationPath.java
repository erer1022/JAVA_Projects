package edu.uob;

public class LocationPath {
    private Location fromLocation, toLocation;

    public LocationPath(Location fromLocation, Location toLocation) {
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
    }

    public String getDescription() {
        return "Here's the path to the next Location: " + fromLocation.getName() + " -> " + toLocation.getName();
    }

    public Location getToLocation() {
        return toLocation;
    }

    public Location getFromLocation() {
        return fromLocation;
    }
}
