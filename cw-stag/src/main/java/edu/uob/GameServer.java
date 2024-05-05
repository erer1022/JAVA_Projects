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
import java.util.stream.Collectors;

public final class GameServer {

    private static final char END_OF_TRANSMISSION = 4;
    private final EntityParser entityParser;
    private final ActionParser actionParser;
    private List<Location> locations;
    private Location startLocation;
    private Location storeroom = null;
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
            this.startLocation = locations.get(0);
            actions = actionParser.parseActionsFromFile(actionsFile);

            // Locate the storeroom in the list of locations
            if (storeroom == null) {
                for (Location location : locations) {
                    if (location.getName().equalsIgnoreCase("storeroom")) {
                        storeroom = location;
                        break;
                    }
                }
            }

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

        // Remove the first token
        tokens = tokens.subList(1, tokens.size());

        if (tokens.contains("and")) {
            return "A single command can only be used to perform a single built-in command or single game action";
        }

        // Check no extraneous entities are present
        if(areThereExtraneousEntities(player, tokens)) {
            return "The action can't be performed, you may use extraneous entity.";
        }

        // List of basic commands
        List<String> basicCommands = Arrays.asList("look", "inventory", "inv", "get", "drop", "goto", "health");

        // Check for any basic command in tokens
        List<String> foundBasicCommands = tokens.stream()
                .filter(basicCommands::contains)
                .collect(Collectors.toList());

        // If tokens contains basic command && only one basic command  --> perform basic commands
        if (foundBasicCommands.size() == 1) {
            // Execute the basic command if exactly one is found
            return performBasicCommand(player, tokens, foundBasicCommands.get(0));
        } else if (foundBasicCommands.size() > 1) {
            // More than one basic command found
            return "Ambiguous command: multiple basic commands detected.";
        }

        // else, try to find match action && detect if more than one action in the incoming command

        Set<GameAction> potentialActions = getMatchingAction(tokens);
        if (tokens.size() == 1 && potentialActions.size() > 1) {
            return "there is more than one valid action possible - which one do you want to perform ?"; // Handle ambiguity
        }
        Set<String> tokenSet = new HashSet<>(tokens);
        // Filter out actions that don't match the remaining tokens
        potentialActions.removeIf(action -> !matchAction(player, tokenSet, action));

        if(potentialActions.size() == 0){
            return "The action can't be performed, you may act upon a wrong subject.";
        } else {
             return performAction(player, potentialActions.iterator().next());
        }
    }

    private String performBasicCommand(Player player, List<String> tokens, String basicCommand) {
            switch (basicCommand) {
                /* prints names and descriptions of entities in the current location and lists paths to other locations */
                case "look":
                    return lookCurrentLocation(player.getCurrentLocation());

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
                    return getArtefact(player, player.getCurrentLocation(), tokens);

                case "drop":
                    return dropArtefact(player, player.getCurrentLocation(), tokens);

                case "goto":
                    return goToNextLocation(player, player.getCurrentLocation(), tokens);

                case "health":
                    return player.getStatus();

                default:
                    // Return a default message if no actions or commands matched
                    return "Command not recognized.";

            }
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

    private String lookCurrentLocation(Location currentLocation) {
        StringBuilder description = new StringBuilder("Welcome!\n");
        description.append("This is ").append(currentLocation.getDescription()).append("\n");

        description.append("In this place, you can see: ");
        // Append descriptions of artefacts, furniture, characters, and paths
        for (Artefact artefact : currentLocation.getArtefacts()) {
            description.append(artefact.getDescription()).append("\n");
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


        // Only when players are at the same location, and there are more than one, they will receive a description about other players.
        if (players.size() > 1) {
            boolean allPlayersAtSameLocation = true;

            // First, check if all players are at the same location
            for (Player player : players) {
                if (!player.getCurrentLocation().equals(currentLocation)) {
                    allPlayersAtSameLocation = false;
                    break;
                }
            }

            // If all players are at the same location, build the description
            if (allPlayersAtSameLocation) {
                description.append("There are multiple players in ").append(currentLocation.getName()).append(": ");
                for (Player player : players) {
                    description.append(player.getName()).append(" ");
                }
            }
        }
        return description.toString();
    }

    private String getArtefact(Player currentPlayer, Location currentLocation, List<String> tokens) {
        List<Artefact> currentLocationArtefacts = currentLocation.getArtefacts();

        // Counter to track the number of artifacts found
        int artifactsFound = 0;

        // Artefact to be obtained
        Artefact artefactToGet = null;

        // Find the artefact to get
        for (Artefact artefact : currentLocationArtefacts) {
            for (String token : tokens) {
                // If the artefact exists in the location
                if (artefact.getName().equals(token)) {
                    artifactsFound++;
                    artefactToGet = artefact;
                }
            }
        }

        if (artifactsFound == 1 && artefactToGet != null) {
            // Add the artefact to the player's inventory
            currentPlayer.addArtefact(artefactToGet);
            // Remove the artefact from the current location
            currentLocation.removeArtefact(artefactToGet);
            return "You've successfully got the " + artefactToGet.getName();
        } else if (artifactsFound > 1) {
            return "Multiple artifacts match the given command. Please be more specific.";
        } else {
            return "There's no such artifact that can be obtained.";
        }
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
                    Location toLocation = path.getToLocation(); // Retrieve the next location

                    // Set the player's current location to this new location
                    currentPlayer.moveToLocation(toLocation);

                    return "You've successfully moved to " + toLocation.getName();
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


    public Set<GameAction> getMatchingAction(List<String> tokens) {
        Set<GameAction> potentialActions = new HashSet<>();
        // Convert the token list to a set for quick lookup, avoiding duplicate tokens e.g. lock lock with key
        Set<String> tokenSet = new HashSet<>(tokens);

        // Check each token as a potential trigger to gather all matching actions
        for (String token : tokenSet) {
            for (Map.Entry<String, HashSet<GameAction>> entry : actions.entrySet()) {
                String actionToken = entry.getKey();

                // Check if the token matches the action token (key)
                if (actionToken.contains(token)) {
                    HashSet<GameAction> actionsForToken = entry.getValue();
                    // Check if the token matches any trigger of the action
                    for (GameAction action : actionsForToken) {
                        if (action.getTriggers().contains(token)) {
                            potentialActions.add(action);
                        }
                    }
                }
            }
        }
        return potentialActions;
    }

    // compare client input "tokens" with GameAction
    private boolean matchAction(Player player, Set<String> tokens, GameAction action) {
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

        return true; // The action matches
    }

    private boolean areThereExtraneousEntities(Player player, List<String> tokens) {
        // Clone the original list to avoid modifying it directly.
        List<Location> otherLocations = new ArrayList<>(locations);
        // Remove the currentLocation from the new list.
        otherLocations.remove(player.getCurrentLocation());
        Set<String> extraneousEntities = new HashSet<>();

        for (Location otherLocation : otherLocations) {
            // Get lists of entity names in the current location
            List<String> otherLocationEntities = getCurrentLocationEntities(otherLocation);
            extraneousEntities.addAll(otherLocationEntities);
        }

        for (String token : tokens) {
            if (extraneousEntities.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String performAction(Player currentPlayer, GameAction action) {
        List<String> entitiesToConsume = action.getConsumed();
        List<String> entitiesToProduce = action.getProduced();

        if (areAllSubjectsAvailable(currentPlayer, action)) {
            // Consume entities from the player's inventory or current location and move to storeroom
            consumeEntity(currentPlayer, entitiesToConsume);
            // move the produced entity from storeroom to current location
            produceEntity(currentPlayer, entitiesToProduce);
            if (currentPlayer.getHealth() == 0) {
                resetPlayer(currentPlayer);
            // Return narration or feedback to the user
                return action.getNarration() + "\nyou died and lost all of your items, you must return to the start of the game";
            } else {
                return action.getNarration();
            }
        } else {
            return "You need to find something before this action.";
        }
    }

    private void resetPlayer(Player currentPlayer) {
        /* When a player's health runs out (i.e. when it becomes zero)
           1. they should lose all of the items in their inventory (which are dropped in the location where they ran out of health).
           2. The player should then be transported to the start location of the game
           3. and their health level restored to full (i.e. 3). */

        List<Artefact> itemsToLose = currentPlayer.getInventory();
        for (Artefact item : itemsToLose) {
            currentPlayer.getCurrentLocation().addArtefact(item);
        }
        currentPlayer.moveToLocation(startLocation);
        currentPlayer.heal(3);
    }

    // Subjects of an action can be locations, characters or furniture
    private boolean areAllSubjectsAvailable(Player currentPlayer, GameAction action) {
        // Convert player's inventory to a list of entity names
        List<String> playerInventory = getPlayerInventoryNames(currentPlayer);
        Location currentLocation = currentPlayer.getCurrentLocation();

        // Get lists of entity names in the current location
        List<String> currentLocationEntities = getCurrentLocationEntities(currentLocation);

        // Combine player's inventory and current location entities
        Set<String> allEntities = new HashSet<>(playerInventory);
        allEntities.addAll(currentLocationEntities);

        // Get a copy of required subjects to avoid modifying the original list
        List<String> requiredSubjects = new ArrayList<>(action.getSubjects());

        // Using an iterator to safely remove items while iterating
        Iterator<String> it = requiredSubjects.iterator();
        while (it.hasNext()) {
            String subject = it.next();
            // Check if the subject is a location name
            for (Location location : locations) {
                if (location.getName().equals(subject) && location.equals(currentLocation)) {
                    // If the subject is the current location's name and player is at this location, remove it from list
                    it.remove();
                    break;  // Exit the loop after removal to prevent unnecessary checks
                }
            }
        }

        // Check if all remaining required subjects are available
        return requiredSubjects.stream().allMatch(allEntities::contains);
    }


    private List<String> getPlayerInventoryNames(Player currentPlayer) {
        return currentPlayer.getInventory().stream()
                .map(GameEntity::getName)
                .collect(Collectors.toList());
    }

    private List<String> getCurrentLocationEntities(Location currentLocation) {
        List<String> currentLocationEntities = new ArrayList<>();
        // Add all of the artefacts of the current location
        currentLocationEntities.addAll(currentLocation.getArtefacts().stream()
                .map(GameEntity::getName)
                .collect(Collectors.toList()));
        // Add all of the furnitures of the current location
        currentLocationEntities.addAll(currentLocation.getFurnitures().stream()
                .map(GameEntity::getName)
                .collect(Collectors.toList()));
        // Add all of the characters of the current location
        currentLocationEntities.addAll(currentLocation.getCharacters().stream()
                .map(GameEntity::getName)
                .collect(Collectors.toList()));
        return currentLocationEntities;
    }


    private void consumeEntity(Player currentPlayer, List<String> entitiesToConsume) {
        Location currentLocation = currentPlayer.getCurrentLocation();
        List<String> playerInventory = getPlayerInventoryNames(currentPlayer);

        for (String entity : entitiesToConsume) {
            // consume entity is in player's inventory
            if (playerInventory.contains(entity)) {
                consumeFromPlayerInventory(currentPlayer, entity);
            } else {
                // consume entity is in the current location or consumed entity is a location
                consumeFromLocation(currentPlayer, currentLocation, entity);
            }
        }
    }

    private void consumeFromPlayerInventory(Player currentPlayer, String entity) {
        for (Artefact inventory : currentPlayer.getInventory()) {
            if (inventory.getName().equals(entity)) {
                // move from player's inventory
                currentPlayer.removeArtefact(inventory);
                // move the artefact into storeroom
                storeroom.addArtefact(inventory);
                break;
            }
        }
    }

    private void consumeFromLocation(Player currentPlayer, Location currentLocation, String entity) {
        // entity consumed is an artefact
        for (Artefact artefact : currentLocation.getArtefacts()) {
            if (artefact.getName().equals(entity)) {
                currentLocation.removeArtefact(artefact);
                storeroom.addArtefact(artefact);
                return;
            }
        }
        // entity consumed is a furniture
        for (Furniture furniture : currentLocation.getFurnitures()) {
            if (furniture.getName().equals(entity)) {
                currentLocation.removeFurniture(furniture);
                storeroom.addFurniture(furniture);
                return;
            }
        }
        // entity consumed is a location
        for (Location location : locations) {
            if (location.getName().equals(entity)) {
                LocationPath pathToRemove = new LocationPath(currentLocation, location);
                currentLocation.removePath(pathToRemove);
                return;
            }
        }
        // entity consumed is player's health
        if (entity.equals("health")) {
            currentPlayer.takeDamage(1);
        }
    }


    private void produceEntity(Player currentPlayer, List<String> entitiesToProduce) {
        Location currentLocation = currentPlayer.getCurrentLocation();

        // produced entitiy is player's health
        for (String entity : entitiesToProduce) {
            if (entity.equals("health")) {
                currentPlayer.heal(1);
            }
            // produced entity is a location
            producedLocation(currentLocation, entity);
            // or produced entity will move from storeroom to the current location
            moveEntityFromStoreroom(currentLocation, entity);
        }
    }

    private void producedLocation(Location currentLocation, String entity) {
        for (Location location : locations) {
            if (location.getName().equals(entity)) {
                LocationPath pathToAdd = new LocationPath(currentLocation, location);
                currentLocation.addPath(pathToAdd);
                return;
            }
        }
    }

    private void moveEntityFromStoreroom(Location currentLocation, String entity) {
        for (Artefact artefact : storeroom.getArtefacts()) {
            if (artefact.getName().equals(entity)) {
                storeroom.removeArtefact(artefact);
                currentLocation.addArtefact(artefact);
                return;
            }
        }

        for (Furniture furniture : storeroom.getFurnitures()) {
            if (furniture.getName().equals(entity)) {
                storeroom.removeFurniture(furniture);
                currentLocation.addFurniture(furniture);
                return;
            }
        }

        for (Character character : storeroom.getCharacters()) {
            if (character.getName().equals(entity)) {
                storeroom.removeCharacter(character);
                currentLocation.addCharacter(character);
                return;
            }
        }
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
