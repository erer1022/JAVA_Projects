package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;

class ExampleSTAGTests {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testLook() {
    String response = sendCommandToServer("simon: look");
    response = response.toLowerCase();
    System.out.println(response);
    assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
    assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
    assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
    assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
    assertTrue(response.contains("forest"), "Did not see available paths in response to look");
  }

  // Test that we can pick something up and that it appears in our inventory
  @Test
  void testGet()
  {
      String response;
      response = sendCommandToServer("simon: get potion");
      System.out.println(response);
      response = sendCommandToServer("simon: inv");
      System.out.println(response);
      response = response.toLowerCase();

      assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: look");
      System.out.println(response);
      response = response.toLowerCase();
      assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
  }

  // Test that we can goto a different location (we won't get very far if we can't move around the game !)
  @Test
  void testGoto()
  {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      System.out.println(response);
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
  }

  // Add more unit tests or integration tests here.
  @Test
  void testTrigger()
  {
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      //System.out.println(response);
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: get key");
      sendCommandToServer("simon: goto cabin");

      // Composite Commands
      String response4 = sendCommandToServer("simon: get axe open trapdoor");
      System.out.println("response4: " + response4);

      String response1 = sendCommandToServer("simon: axe get");
      //System.out.println("response1: " + response1);
      String response2 = sendCommandToServer("simon: open");
      System.out.println("response2: " + response2);

      //TODO
      String response3 = sendCommandToServer("simon: drop axe key");
      System.out.println("response3: " + response3);

      // Extraneous entity and composite command
      String response5 = sendCommandToServer("simon: get potion, cut down the tree");
      System.out.println("response5: " + response5);

      // Decorated Commands
      //String response5 = sendCommandToServer("simon: please chop the tree using the axe");

      sendCommandToServer("simon: get key and potion");
      String response6 = sendCommandToServer("simon: goto cabin");

      // Partial Commands
      //String response1 = sendCommandToServer("simon: open trapdoor");
      //System.out.println("response1: " + response1);

      /*String response0 = sendCommandToServer("simon: get potion and open trapdoor");
      //System.out.println("response0: " + response0);


      sendCommandToServer("simon: look");
      String response2 = sendCommandToServer("simon: chop tree with axe");
      //System.out.println("response2: " + response2);
      String response3 = sendCommandToServer("simon: look");
      //System.out.println("response3: " + response3);*/

  }

}
