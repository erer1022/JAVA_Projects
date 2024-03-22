package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Duration;

public class ExampleDBTests {

    private DBServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    // Random name generator - useful for testing "bare earth" queries (i.e. where tables don't previously exist)
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }

    private String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // A basic test that creates a database, creates a table, inserts some test data, then queries it.
    // It then checks the response to see that a couple of the entries in the table are returned as expected
    @Test
    public void testBasicCreateAndQuery() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        assertTrue(response.contains("[OK]"), "A valid query was made, however an [OK] tag was not returned");
        assertFalse(response.contains("[ERROR]"), "A valid query was made, however an [ERROR] tag was returned");
        assertTrue(response.contains("Simon"), "An attempt was made to add Simon to the table, but they were not returned by SELECT *");
        assertTrue(response.contains("Chris"), "An attempt was made to add Chris to the table, but they were not returned by SELECT *");
    }

    // A test to make sure that querying returns a valid ID (this test also implicitly checks the "==" condition)
    // (these IDs are used to create relations between tables, so it is essential that suitable IDs are being generated and returned !)
    @Test
    public void testQueryID() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT id FROM marks WHERE name == 'Simon';");
        // Convert multi-lined responses into just a single line
        String singleLine = response.replace("\n"," ").trim();

        // Split the line on the space character
        String[] tokens = singleLine.split(" ");
        // Check that the very last token is a number (which should be the ID of the entry)
        String lastToken = tokens[tokens.length-1];
        try {
            Integer.parseInt(lastToken);
        } catch (NumberFormatException nfe) {
            fail("The last token returned by `SELECT id FROM marks WHERE name == 'Simon';` should have been an integer ID, but was " + lastToken);
        }
    }

    // A test to make sure that databases can be reopened after server restart
    @Test
    public void testTablePersistsAfterRestart() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        // Create a new server object
        server = new DBServer();
        sendCommandToServer("USE " + randomName + ";");
        String response = sendCommandToServer("SELECT * FROM marks;");
        System.out.println(response);
        assertTrue(response.contains("Simon"), "Simon was added to a table and the server restarted - but Simon was not returned by SELECT *");
    }

    // Test to make sure that the [ERROR] tag is returned in the case of an error (and NOT the [OK] tag)
    @Test
    public void testForErrorTag() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        String response = sendCommandToServer("SELECT * FROM libraryfines;");
        System.out.println(response);
        assertTrue(response.contains("[ERROR]"), "An attempt was made to access a non-existent table, however an [ERROR] tag was not returned");
        assertFalse(response.contains("[OK]"), "An attempt was made to access a non-existent table, however an [OK] tag was returned");
    }

    @Test
    public void testFromScript() {
        sendCommandToServer("CREATE DATABASE markbook"  + ";");
        sendCommandToServer("USE " + "markbook" + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");

        sendCommandToServer("DELETE FROM marks WHERE name == 'Sion';");

        String response2 = sendCommandToServer("SELECT * FROM marks;");
        System.out.println(response2);

    }

    @Test
    public void testDROP() {
        String response = sendCommandToServer("CREATE DATABASE database1;");
        System.out.println(response);
        sendCommandToServer("USE database1;");
        sendCommandToServer("CREATE TABLE data1 (name, mark, pass);");
        sendCommandToServer("CREATE TABLE data2 (name, mark, pass);");

        String response2 = sendCommandToServer("DROP TABLE data2;");
        System.out.println(response2);
        String response3 = sendCommandToServer("DROP DATABASE database1;");
        System.out.println(response3);

        String response4 = sendCommandToServer("DROP DATABASE database1;");
        System.out.println(response4);

        String response5 = sendCommandToServer("DROP TABLE data1;");
        System.out.println(response5);

    }

    @Test
    public void testUPDATE() {
        sendCommandToServer("CREATE DATABASE darkbook"  + ";");
        sendCommandToServer("USE " + "darkbook" + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        //multiple set clause, case insensitive
        String response3 = sendCommandToServer("update marks set mark = 35, pass = FALSE where name == 'Simon';");
        System.out.println(response3);
        String response1 = sendCommandToServer("SELECT * FROM marks;");
        System.out.println(response1);

        //Wrong table
        String response2 = sendCommandToServer("UPDATE test SET mark = 35, pass = FALSE WHERE name == 'Simon';");
        System.out.println(response2);

        //Missing setclause
        String response4 = sendCommandToServer("UPDATE marks mark = 35 WHERE name == 'Simon';");
        System.out.println(response4);
        String response5 = sendCommandToServer("UPDATE marks set WHERE name == 'Simon';");
        System.out.println(response5);

        //Missing where clause
        String response6 = sendCommandToServer("UPDATE marks SET mark = 35, pass = FALSE;");
        System.out.println(response6);

        //changing (updating) the ID of a record
        String response8 = sendCommandToServer("UPDATE marks SET id = 2 WHERE name == 'Simon';");
        System.out.println(response8);
    }

    @Test
    public void testALTER() {
        sendCommandToServer("CREATE DATABASE markbook;");
        sendCommandToServer("USE " + "markbook" + ";");
        sendCommandToServer("CREATE TABLE marks (Name, Mark, Pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        String response = sendCommandToServer("ALTER TABLE marks ADD age;");
        System.out.println(response);
        String response1 = sendCommandToServer("SELECT * FROM marks;");
        System.out.println(response1);
        /* treat column names as case-insensitive for querying, but preserve the case when storing them */
        String response4 = sendCommandToServer("ALTER TABLE marks DROP pass;");
        System.out.println(response4);
        String response5 = sendCommandToServer("SELECT * FROM marks;");
        System.out.println(response5);

        /* attempting to remove the ID column from a table */
        String response2 = sendCommandToServer("ALTER TABLE marks DROP id;");
        System.out.println(response2);

        /* attribute names containing . characters are not permitted by the BNF */
        String response3 = sendCommandToServer("ALTER TABLE marks ADD hi.j;");
        System.out.println(response3);

        /* attribute names containing . characters are not permitted by the BNF */
        String response6 = sendCommandToServer("ALTER TABLE marks ADD id;");
        System.out.println(response6);
    }

    @Test
    public void testJOIN() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks;");
        System.out.println(response);

        sendCommandToServer("CREATE TABLE coursework (task, submission);");
        sendCommandToServer("INSERT INTO coursework VALUES (OXO, 3);");
        sendCommandToServer("INSERT INTO coursework VALUES (DB, 1);");
        sendCommandToServer("INSERT INTO coursework VALUES (OXO, 4);");
        sendCommandToServer("INSERT INTO coursework VALUES (STAG, 2);");
        String response2 = sendCommandToServer("SELECT * FROM coursework;");
        System.out.println(response2);

        String response3 = sendCommandToServer("JOIN coursework AND marks ON submission AND id;");
        System.out.println(response3);
        String response4 = sendCommandToServer("JOIN marks AND coursework ON id AND submission;");
        System.out.println(response4);
        //Invalid command, missing "AND"
        String response5 = sendCommandToServer("JOIN marks coursework ON id AND submission;");
        System.out.println(response5);

        //Check order
        String response6 = sendCommandToServer("JOIN marks AND coursework ON submission AND id;");
        System.out.println(response6);
    }



    @Test
    public void testSELECT(){
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        String response = sendCommandToServer("SELECT * FROM marks WHERE (pass == TRUE) AND (mark >= 55);");
        System.out.println(response);

        //Missing attribute
        String response1 = sendCommandToServer("SELECT  FROM marks;");
        System.out.println(response1);
        //Wrong table name
        String response2 = sendCommandToServer("SELECT * FROM test WHERE (pass == TRUE) AND (mark >= 55);");
        System.out.println(response2);
        //Wrong attribute name
        String response4 = sendCommandToServer("select shidjs from MarKs where (pass == TRUE) and (mark >= 55);");
        System.out.println(response4);
        //multiple attributes
        String response5 = sendCommandToServer("select name, mark from MarKs where (pass == TRUE) and (mark >= 55);");
        System.out.println(response5);
        //whitespace
        String response6 = sendCommandToServer("select name, mark from MarKs where Name=='Chris';");
        System.out.println(response6);

    }

    @Test
    public void testCREATE() {
        String response = sendCommandToServer("create database " + "markbook" + ";");
        System.out.println(response);
        String response2 = sendCommandToServer("create database " + "MARKBOOK" + ";");
        System.out.println(response2);

        sendCommandToServer("use " + "markbook" + ";");
        //case-insensitive
        String response3 = sendCommandToServer("create table MARKS;");
        System.out.println(response3);
        String response4 = sendCommandToServer("CREATE TABLE marks;");
        System.out.println(response4);
        //Missing database name
        String response1 = sendCommandToServer("CREATE DATABASE;");
        System.out.println(response1);
        //creating a table with duplicate column names
        String response5 = sendCommandToServer("create table test (name, name, marks);");
        System.out.println(response5);
        //Invalid name
        String response6 = sendCommandToServer("CREATE DATABASE hh.hh;");
        System.out.println(response6);
        /* treat column names as case-insensitive for querying,
           but preserve the case when storing them */
        String response7 = sendCommandToServer("create table column (Name, Gender, marks);");
        System.out.println(response7);
        String response8 = sendCommandToServer("select name from COLUMN;");
        System.out.println(response8);
    }

    @Test
    public void testINSERT() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("insert into marks values ('Simon', 65, TRUE);");
        sendCommandToServer("insert into MARKS values ('Sion', 55, TRUE);");
        //Insert too much value
        String response7 = sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE, xx);");
        System.out.println("Insert too much value: " + response7);
        //Insert too few value
        String response12 = sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35);");
        System.out.println("Insert too few value: " + response12);

        String response8 = sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        System.out.println(response8);
        String response9 = sendCommandToServer("INSERT INTO marks VALUES ();");
        System.out.println(response9);
        //Invalid attribute name, using reserved words
        String response10 = sendCommandToServer("INSERT INTO marks VALUES ('AND', 35, TRUE);");
        System.out.println(response10);
    }

    @Test
    public void testDELETE() {
        String randomName = generateRandomName();
        sendCommandToServer("CREATE DATABASE " + randomName + ";");
        sendCommandToServer("USE " + randomName + ";");
        sendCommandToServer("CREATE TABLE marks (name, mark, pass);");
        sendCommandToServer("INSERT INTO marks VALUES ('Simon', 65, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Sion', 55, TRUE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Rob', 35, FALSE);");
        sendCommandToServer("INSERT INTO marks VALUES ('Chris', 20, FALSE);");
        //Wrong table name
        String response = sendCommandToServer("delete from test where name == simon;");
        System.out.println(response);
        //case-insensitive
        String response1 = sendCommandToServer("delete from marks where name=='Simon';");
        System.out.println(response1);
        //missing where clause
        String response2 = sendCommandToServer("delete from marks name == Rob;");
        System.out.println(response2);
        String response3 = sendCommandToServer("delete from marks where;");
        System.out.println(response3);
    }
}
