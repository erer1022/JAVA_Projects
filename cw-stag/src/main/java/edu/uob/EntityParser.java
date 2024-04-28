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
        public List<Graph> parseEntitiesFromFile(File entityFile) throws IOException, ParseException {
                try  {
                        FileReader reader = new FileReader("config" + File.separator + "basic-entities.dot");
                        Parser parser = new Parser();
                        parser.parse(reader);
                        /* Returns the main Graphs found in the Reader stream */
                        Graph wholeDocument = parser.getGraphs().get(0);
                        /* Returns a list of all sub graphs. */
                        List<Graph> sections = wholeDocument.getSubgraphs();
                        return sections;
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
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
                                entityLocations.add(createLocationFromNode(locationDetails));
                        }
                        parsePaths(sections, entityLocations);
                        parseOtherEntities(locations, entityLocations);
                }
                return entityLocations;
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
                                                addCharactersToLocation(subgraph, location, entityLocations);
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






        private Location createLocationFromNode(Node node) {
                String locationName = node.getId().getId();
                String locationDescription = node.getAttribute(locationName);
                return new Location(locationName, locationDescription);
        }

        public Graph getSectionGraph(List<Graph> sections, int index) {
                if (sections != null && !sections.isEmpty() && index >= 0 && index < sections.size()) {
                        return sections.get(index);
                }
                return null;
        }

        private void parsePaths(List<Graph> sections, List<Location> locations) {
                /* Returns all edges of this graph */
                ArrayList<Edge> paths = sections.get(1).getEdges();

                for (int i = 0; i < paths.size(); i++) {
                        Edge path = paths.get(i);
                        Node fromLocation = path.getSource().getNode(); /* Returns the source node of the edge */
                        String fromName = fromLocation.getId().getId();
                        Node toLocation = path.getTarget().getNode(); /* Returns the target node of the edge */
                        String toName = toLocation.getId().getId();

                        for (Location location : locations) {
                                if (location.getName().equalsIgnoreCase(fromName)) {
                                        LocationPath locationPath = new LocationPath(fromName, toName);
                                        location.addPath(locationPath);
                                }
                        }
                }
        }


}
