package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EntityParser {
        public List<Graph> parseEntitiesFromFile(File entitiesFile) throws IOException, ParseException {
                try  {
                        FileReader reader = new FileReader(entitiesFile);
                        Parser parser = new Parser();
                        parser.parse(reader);
                        /* Returns the main Graphs found in the Reader stream */
                        Graph wholeDocument = parser.getGraphs().get(0);
                        /* Returns a list of all sub graphs. */
                        List<Graph> sections = wholeDocument.getSubgraphs();
                        return sections;
                } catch (FileNotFoundException e) {
                        throw new IOException("The file was not found: " + entitiesFile.getAbsolutePath(), e);
                } catch (ParseException e) {
                        throw new ParseException("Error parsing the file: " + entitiesFile.getAbsolutePath());
                }

        }

        public List<Location> parseLocations(List<Graph> sections) {
                Graph locationsGraph = getSectionGraph(sections, 0);
                List<Location> entityLocations = new ArrayList<>();
                if (locationsGraph != null) {
                        /* Returns a list of all sub graphs */
                        List<Graph> locations = locationsGraph.getSubgraphs();

                        for (int i = 0; i < locations.size(); i++) {
                                /* Returns all Nodes of the graph, false -> not include the subgraphs */
                                Node locationDetails = locations.get(i).getNodes(false).get(0);
                                /* System.out.println("These are locationDetails " + locationDetails.toString()); */

                                entityLocations.add(createLocationFromNode(locationDetails));
                        }
                        parsePaths(sections, entityLocations);
                        parseOtherEntities(locations, entityLocations);
                }
                return entityLocations;
        }



        private Location createLocationFromNode(Node node) {
                String locationName = node.getId().getId();
                String locationDescription = node.getAttribute("description");
                /* System.out.println("locationName: " + locationName + " " + "locationDescription: " + locationDescription); */

                return new Location(locationName, locationDescription);
        }

        public Graph getSectionGraph(List<Graph> sections, int index) {
                if (sections != null && !sections.isEmpty() && index >= 0 && index < sections.size()) {
                        return sections.get(index);
                }
                return null;
        }



        private void parseOtherEntities(List<Graph> locations, List<Location> entityLocations) {
                for (Graph location : locations) {
                        List<Graph> subgraphs = location.getSubgraphs();
                        for (Graph subgraph : subgraphs) {
                                String subgraphId = subgraph.getId().getId().toLowerCase();
                                switch (subgraphId) {
                                        case "artefacts":
                                                addArtefactToLocation(subgraph, location, entityLocations);
                                                break;
                                        case "furniture":
                                                addFurnitureToLocation(subgraph, location, entityLocations);
                                                break;
                                        case "characters":
                                                addCharacterToLocation(subgraph, location, entityLocations);
                                                break;

                                }
                        }
                }
        }

        private void addArtefactToLocation(Graph subgraph, Graph location, List<Location> entityLocations) {
                List<Node> artefactNodes = subgraph.getNodes(false);
                for (Node artefactNode : artefactNodes) {
                        String artefactName = artefactNode.getId().getId();
                        String artefactDescription = artefactNode.getAttribute(("description"));


                        Artefact artefact = new Artefact(artefactName, artefactDescription);
                        for (Location entitylocation : entityLocations) {
                                String currentLocation = location.getNodes(false).get(0).getId().getId();
                                if (entitylocation.getName().equals(currentLocation)) {
                                        entitylocation.addArtefact(artefact);
                                }
                        }
                }

        }

        private void addFurnitureToLocation(Graph subgraph, Graph location, List<Location> entityLocations) {
                List<Node> furnitureNodes = subgraph.getNodes(false);
                for (Node furnitureNode : furnitureNodes) {
                        String furnitureName = furnitureNode.getId().getId();
                        String furnitureDescription = furnitureNode.getAttribute(("description"));
                        Furniture furniture = new Furniture(furnitureName, furnitureDescription);
                        for (Location entitylocation : entityLocations) {
                                String currentLocation = location.getNodes(false).get(0).getId().getId();
                                if (entitylocation.getName().equals(currentLocation)) {
                                        entitylocation.addFurniture(furniture);
                                }
                        }
                }
        }

        private void addCharacterToLocation(Graph subgraph, Graph location, List<Location> entityLocations) {
                List<Node> characterNodes = subgraph.getNodes(false);
                for (Node characterNode : characterNodes) {
                        String characterName = characterNode.getId().getId();
                        String characterDescription = characterNode.getAttribute(("description"));

                        Character character = new Character(characterName, characterDescription);
                        for (Location entitylocation : entityLocations) {
                                String currentLocation = location.getNodes(false).get(0).getId().getId();
                                if (entitylocation.getName().equals(currentLocation)) {
                                        entitylocation.addCharacter(character);
                                }
                        }
                }
        }


        private void parsePaths(List<Graph> sections, List<Location> locations) {
                /* Returns all edges of this graph */
                ArrayList<Edge> paths = sections.get(1).getEdges();

                for (Edge path : paths) {
                        Node fromLocationNode = path.getSource().getNode(); // Source node of the edge
                        Node toLocationNode = path.getTarget().getNode(); // Target node of the edge

                        String fromName = fromLocationNode.getId().getId(); // Get source location's name
                        String toName = toLocationNode.getId().getId(); // Get target location's name

                        Location fromLocation = findLocationByName(locations, fromName);
                        Location toLocation = findLocationByName(locations, toName);

                        if (fromLocation != null && toLocation != null) {
                                LocationPath locationPath = new LocationPath(fromLocation, toLocation); // Create LocationPath with actual objects
                                fromLocation.addPath(locationPath); // Add path to source location
                        }
                }
        }

        private Location findLocationByName(List<Location> locations, String name) {
                for (Location location : locations) {
                        if (location.getName().equalsIgnoreCase(name)) {
                                return location;
                        }
                }
                return null; // Return null if not found
        }




}
