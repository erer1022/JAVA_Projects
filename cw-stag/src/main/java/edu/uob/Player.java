package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private Location currentLocation;
    private List<Artefact> inventory;
    private int health;

    // Constructor to initialize the player with a name and starting location
    public Player(String name, Location startLocation) {
        this.name = name;
        this.currentLocation = startLocation;
        this.inventory = new ArrayList<>();
        this.health = 100;  // Assuming a max health of 100
    }

    public String getName() {
        return this.name;
    }
    // Methods to handle the player's location
    public void moveTo(Location newLocation) {
        this.currentLocation = newLocation;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    // Methods to handle the player's inventory
    public void addArtefact(Artefact artefact) {
        inventory.add(artefact);
    }

    public boolean removeArtefact(Artefact artefact) {
        return inventory.remove(artefact);
    }

    public List<Artefact> getInventory() {
        return inventory;
    }

    // Methods for health management
    public void takeDamage(int amount) {
        health -= amount;
        if (health < 0) {
            health = 0;
        }
    }

    public void heal(int amount) {
        health += amount;
        if (health > 100) {
            health = 100;
        }
    }

    public int getHealth() {
        return health;
    }

    // Utility method for displaying player's status
    public String getStatus() {
        return "Name: " + name + ", Health: " + health + ", Location: " + currentLocation.getName();
    }
}

