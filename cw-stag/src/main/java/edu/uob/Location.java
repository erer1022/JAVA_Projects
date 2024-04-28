package edu.uob;

import java.util.ArrayList;
import java.util.List;

public class Location extends GameEntity{
    private List<LocationPath> paths;
    private List<Artefact> artefacts;
    private List<Furniture> furnitures;
    private List<Character> characters;
    public Location(String name, String description) {

        super(name, description);
        this.paths = new ArrayList<>(); // Initialize paths list
        this.artefacts = new ArrayList<>(); // Initialize artefacts list
        this.furnitures = new ArrayList<>(); // Initialize furnitures list
        this.characters = new ArrayList<>(); // Initialize characters list
    }

    public void addPath(LocationPath path) {
        paths.add(path);
    }
    public void addArtefact(Artefact artefact) {
        artefacts.add(artefact);
    }
    public void addFurniture(Furniture furniture) {
        furnitures.add(furniture);
    }

    public void addCharacter(Character character) {
        characters.add(character);
    }

}
