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

        // Split the processed command into tokens
        List<String> tokens = Arrays.asList(processedCommand.split("\\s+"));

        if (tokens.isEmpty()) {
            return "Command not recognized.";
        }

        // Remove the first token
        tokens = tokens.subList(1, tokens.size());

        for (String token : tokens) {
            switch (token) {
                case "look":
                    Location currentLocation = player.getCurrentLocation();

                    StringBuilder description = new StringBuilder("Welcome!\n");
                    description.append("This is ").append(currentLocation.getDescription()).append("\n");

                    // Append descriptions of artefacts, furniture, characters, and paths
                    for (Artefact artefact : currentLocation.getArtefacts()) {
                    description.append("There is ").append(artefact.getDescription()).append("\n");
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

                default:
                    // Assume getMatchingAction is a method to determine the matching action based on tokens
                    GameAction action = getMatchingAction(tokens);
                    if (action == null) {
                        return "I don't understand what you're trying to do.";
                    } else {
                        return performActionOrHandleAmbiguity(action, tokens);
                    }
            }
        }

        // Return a default message if no actions or commands matched
        return "Command not recognized.";
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

        // Convert the token list to a set for quick lookup
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

    private boolean matchAction(Set<String> tokens, GameAction action) {
        List<String> required = action.getSubjects();

        // Check all required subjects are present in the tokens
        for (String subject : required) {
            if (!tokens.contains(subject)) {
                return false;
            }
        }

        // Check no extraneous entities are present
        for (String token : tokens) {
            if (!required.contains(token) && !action.getTriggers().contains(token) && !action.getConsumed().contains(token) && !action.getProduced().contains(token)) {
                return false;
            }
        }

        return true;
    }

    private String performActionOrHandleAmbiguity(GameAction action, List<String> tokens) {
        // Check for ambiguity: Are there multiple valid actions matching the command?
        Set<GameAction> validActions = new HashSet<>(actions.get(action.getTriggers().get(0))); // Assuming first trigger is the primary key
        validActions.removeIf(a -> !matchAction(new HashSet<>(tokens), a));

        /*if (validActions.size() > 1) {
            return handleAmbiguousCommands(new ArrayList<>(validActions));
        } else {
            return performAction(action);
        }*/
        return "";
    }

    private String handleAmbiguousCommands(List<GameAction> potentialActions) {
        StringBuilder response = new StringBuilder("There is more than one valid action:\n");

        for (GameAction action : potentialActions) {
            response.append("- ").append(action.getNarration()).append("\n");
        }

        response.append("Please clarify your command.");
        return response.toString();
    }

     /*private String performAction(GameAction action) {
        // Consuming entities
        for (String artefact : action.getConsumed()) {
            // Remove item from player's inventory or location
            player.getInventory().removeArtefact(artefact);
        }

        // Producing entities
        for (String artefact : action.getProduced()) {
            // Add item to player's inventory or location
            player.getInventory().addArtefact(artefact);
        }

        // Changing game state
        for (String entity : action.getSubjects()) {
            // Apply necessary game state changes, such as updating world state or unlocking locations
        }

        // Return narration or feedback to the user
        return action.getNarration();
    }*/














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
