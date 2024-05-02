package edu.uob;

import com.alexmerz.graphviz.objects.Graph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;

public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;
    private final EntityParser entityParser;
    private final ActionParser actionParser;
    private List<Location> locations;
    private List<Player> players = new ArrayList<>();;


    private HashMap<String,HashSet<GameAction>> actions;


    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Instanciates a new server instance, specifying a game with some configuration files
    *
    * @param entitiesFile The game configuration file containing all game entities to use in your game
    * @param actionsFile The game configuration file containing all game actions to use in your game
    */
    public GameServer(File entitiesFile, File actionsFile) {
        // TODO implement your server logic here

        // Create an instance of the EntityParser and inject the Parser
        entityParser = new EntityParser();
        actionParser = new ActionParser();

        try {
            List<Graph> sections = entityParser.parseEntitiesFromFile(entitiesFile);
            locations = entityParser.parseLocations(sections);
            actions = actionParser.parseActionsFromFile(actionsFile);

        } catch (IOException e) {
            // Handle the exceptions appropriately
            e.printStackTrace();
        } catch (com.alexmerz.graphviz.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * This method handles all incoming game commands and carries out the corresponding actions.</p>
    *
    * @param command The incoming command to be processed
    */


    public String handleCommand(String command) {
        // Preprocess the command to a normalized form
        String processedCommand = preprocessCommand(command);

        String playerName = "DefaultPlayer";  // Default player name

        if (processedCommand.contains(":")) {
            // Split the command at the first colon
            String[] parts = command.split(":", 2);
            playerName = parts[0].trim();  // Use this as the player's name
            processedCommand = preprocessCommand(parts[1]);  // Reprocess to handle remaining part
        }

        // Retrieve or create the player
        Player player = getOrCreatePlayer(playerName);
        Location currentLocation = player.getCurrentLocation();

        // Split the processed command into tokens
        List<String> tokens = Arrays.asList(processedCommand.split("\\s+"));

        if (tokens.isEmpty()) {
            return "Command not recognized.";
        }

        // Remove the first token
        tokens = tokens.subList(1, tokens.size());

        for (String token : tokens) {
            switch (token) {
                /* prints names and descriptions of entities in the current location and lists paths to other locations */
                case "look":
                    return lookCurrentLocation(currentLocation);

                /* inventory (or inv for short) lists all of the artefacts currently being carried by the player */
                case "inventory":
                case "inv":
                    List<Artefact> inventory = player.getInventory();
                    List<String> inventoryNameList = new ArrayList<>();
                    for (Artefact artefact : inventory) {
                        inventoryNameList.add(artefact.getName());
                    }
                    return "Here's your inventory: " + String.join(", ", inventoryNameList);

                case "get":
                    return getArtefact(player, currentLocation, tokens);

                case "drop":
                    return dropArtefact(player, currentLocation, tokens);

                case "goto":
                    return goToNextLocation(player, currentLocation, tokens);


                default:
                    // Assume getMatchingAction is a method to determine the matching action based on tokens
                    GameAction action = getMatchingAction(tokens);
                    if (action == null) {
                        return "I don't understand what you're trying to do.";
                    } else {
                        return performAction(player, action);
                    }
            }
        }
        // Return a default message if no actions or commands matched
        return "Command not recognized.";
    }

    private String lookCurrentLocation(Location currentLocation) {
        StringBuilder description = new StringBuilder("Welcome!\n");
        description.append("This is ").append(currentLocation.getDescription()).append("\n");

        // Append descriptions of artefacts, furniture, characters, and paths
        for (Artefact artefact : currentLocation.getArtefacts()) {
            description.append("In this place, you can see\n").append(artefact.getDescription()).append("\n");
        }

        for (Furniture furniture : currentLocation.getFurnitures()) {
            description.append(furniture.getDescription()).append("\n");
        }

        for (Character character : currentLocation.getCharacters()) {
            description.append(character.getDescription()).append("\n");
        }

        for (LocationPath path : currentLocation.getPaths()) {
            description.append(path.getDescription()).append("\n");
        }
        return description.toString();
    }

    private String getArtefact(Player currentPlayer, Location currentLocation, List<String> tokens) {
        /* get the list of current location's artefacts */
        List<Artefact> currentLocationArtefacts = currentLocation.getArtefacts();
        for(Artefact artefactToGet : currentLocationArtefacts) {
            for(String token : tokens) {
                /* if get xxx, the artefact do exist in the location */
                if(artefactToGet.getName().equals(token)) {
                    /* add the artefact to player's inventory */
                    currentPlayer.addArtefact(artefactToGet);
                    /* remove the artefact from the current location */
                    currentLocation.removeArtefact(artefactToGet);
                    return "You've successfully got the " + artefactToGet.getName();
                }
            }
        }
        return "There's no such artefact can be obtained.";
    }

    private String dropArtefact(Player currentPlayer, Location currentLocation, List<String> tokens) {
        List<Artefact> inventoryList = currentPlayer.getInventory();

        for (String token : tokens) {
            // Find the artefact that matches the token
            Artefact artefactToDrop = inventoryList.stream()
                    .filter(artefact -> artefact.getName().equalsIgnoreCase(token))
                    .findFirst()
                    .orElse(null);

            if (artefactToDrop != null) {
                // Remove the artefact from the player's inventory
                currentPlayer.removeArtefact(artefactToDrop);
                // Add it back to the current location
                currentLocation.addArtefact(artefactToDrop);
                return "You've successfully dropped the " + artefactToDrop.getName();
            }
        }
        // If no matching artefact was found to drop
        return "You can't drop this artefact, because you don't have it.";
    }

    public String goToNextLocation(Player currentPlayer, Location currentLocation, List<String> tokens) {
        List<LocationPath> paths = currentLocation.getPaths(); // Get the list of paths from the current location

        for (String token : tokens) {
            for (LocationPath path : paths) {
                // Check if the path's destination matches the token (case-insensitive comparison)
                if (path.getToLocation().getName().equalsIgnoreCase(token)) {
                    Location nextLocation = path.getToLocation(); // Retrieve the next location

                    // Set the player's current location to this new location
                    currentPlayer.resetLocation(nextLocation);

                    return "You've successfully moved to " + nextLocation.getName();
                }
            }
        }
        // If no valid path is found, return a message indicating failure
        return "You can't go there from here.";
    }





    private Player getOrCreatePlayer(String playerName) {
        // Check if the player already exists
        Player player = players.stream().filter(p -> p.getName().equals(playerName)).findFirst().orElse(null);

        if (player == null) {
            // If the player doesn't exist, create a new player and add to the list
            player = new Player(playerName, locations.get(0));
            players.add(player);
        }

        return player;
    }


    /* Convert to lowercase and Strip out punctuation */
    private String preprocessCommand(String command) {
        // Split the string at the first occurrence of a colon
        String[] parts = command.split(":", 2);

        // If there are at least two parts, handle them
        if (parts.length == 2) {
            String playerName = parts[0].trim();  // The player's name (before the colon)
            String commandPart = parts[1].toLowerCase().replaceAll("[^a-z0-9\\s]", "").trim();  // The command part after the colon

            // Combine them back together
            return playerName + ": " + commandPart;
        }

        // If no colon is found, just sanitize the entire command
        return command.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
    }


    public GameAction getMatchingAction(List<String> tokens) {
        Set<GameAction> potentialActions = new HashSet<>();

        // Convert the token list to a set for quick lookup, avoiding duplicate tokens e.g. lock lock with key
        Set<String> tokenSet = new HashSet<>(tokens);

        // Check each token as a potential trigger to gather all matching actions
        for (String token : tokenSet) {
            HashSet<GameAction> actionsForToken = actions.get(token);
            if (actionsForToken != null) {
                potentialActions.addAll(actionsForToken);
            }
        }
        // Filter out actions that don't match the remaining tokens
        potentialActions.removeIf(action -> !matchAction(tokenSet, action));

        if (potentialActions.size() == 0) {
            return null; // No matching action found
        } else if (potentialActions.size() > 1) {
            handleAmbiguousCommands(new ArrayList<>(potentialActions)); // Handle ambiguity
            return null;
        } else {
            return potentialActions.iterator().next(); // Return the single matching action
        }
    }

    // compare client input "tokens" with GameAction
    private boolean matchAction(Set<String> tokens, GameAction action) {
        List<String> required = action.getSubjects();

        // Check if at least one required subject is present in the tokens
        boolean hasRequiredSubject = false;
        for (String subject : required) {
            if (tokens.contains(subject)) {
                hasRequiredSubject = true;
                break;
            }
        }

        if (!hasRequiredSubject) {
            return false; // No required subject present
        }

        // Check no extraneous entities are present
        for (String token : tokens) {
            if (!required.contains(token) && !action.getTriggers().contains(token) && !action.getConsumed().contains(token) && !action.getProduced().contains(token)) {
                return false;
            }
        }
        return true; // The action matches
    }

    private String handleAmbiguousCommands(List<GameAction> potentialActions) {
        StringBuilder response = new StringBuilder("There is more than one valid action:\n");

        for (GameAction action : potentialActions) {
            response.append("- ").append(action.getNarration()).append("\n");
        }

        response.append("Please clarify your command.");
        return response.toString();
    }

    private String performAction(Player currentPlayer, GameAction action) {
        Set<String> artefactsToConsume = new HashSet<>(action.getConsumed());
        Set<String> artefactsToProduce = new HashSet<>(action.getProduced());

        // Consuming entities
        for (String artefactName : artefactsToConsume) {
            Artefact artefact = isArtefactAvailable(currentPlayer, artefactName);

            if (artefact != null) {
                // Remove item from player's inventory or location
                currentPlayer.removeArtefact(artefact);
                // Optionally: add to storeroom or handle appropriately
            } else {
                return "Artefact '" + artefactName + "' not found in inventory or location.";
            }
        }

        // Producing entities
        for (String artefactName : artefactsToProduce) {
            Artefact artefact = retrieveArtefactFromStoreroom(artefactName);

            if (artefact != null) {
                currentPlayer.addArtefact(artefact);
            } else {
                return "Artefact '" + artefactName + "' not found in storeroom.";
            }
        }

        // Return narration or feedback to the user
        return action.getNarration();
    }

    private Artefact isArtefactAvailable(Player currentPlayer, String artefactName) {
        // Check if the artefact is in the player's inventory
        for (Artefact artefact : currentPlayer.getInventory()) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                return artefact; // Artefact found in inventory
            }
        }

        // If not found, check the current location
        for (Artefact artefact : currentPlayer.getCurrentLocation().getArtefacts()) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                return artefact; // Artefact found in location
            }
        }
        return null; // Artefact not found in inventory or location
    }

    private Artefact retrieveArtefactFromStoreroom(String artefactName) {
        // Locate the storeroom in the list of locations
        Location storeroom = null;

        for (Location location : locations) {
            if (location.getName().equalsIgnoreCase("storeroom")) {
                storeroom = location;
                break;
            }
        }

        // Check for the artefact in the storeroom
        for (Artefact artefact : storeroom.getArtefacts()) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                // Artefact found, remove it from the storeroom and return it
                storeroom.removeArtefact(artefact);
                return artefact;
            }
        }

        return null; // Artefact not found in storeroom
    }

















    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Handles an incoming connection from the socket server.
    *
    * @param serverSocket The client socket to read/write from.
    * @throws IOException If any IO related operation fails.
    */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
        BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
