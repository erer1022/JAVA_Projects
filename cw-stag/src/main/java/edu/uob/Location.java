package edu.uob;

import java.util.List;

public class Location extends GameEntity{
    private List<LocationPath> paths;
    private List<Artefact> artefacts;
    private List<Furniture> furnitures;
    private List<Character> characters;
    public Location(String name, String description) {
        super(name, description);
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
