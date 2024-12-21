# Combined Assignment Documentation

This repository contains two Java-based projects: a relational database server and a text adventure game engine. Both projects utilize Java and Maven, demonstrating concepts such as server-client communication, dynamic configurations, and robust data handling.

---


## Database Server Assignment

This project involves building a relational database server from scratch as part of a coursework assignment. The server supports a simplified SQL-like query language and ensures data persistence by storing tables as files in the file system. This README provides an overview of the project, its structure, and key features.

### Project Overview

- **Purpose:** Develop a robust and flexible database server to process and manage data using a simplified query language.
- **Technologies:** Java, Maven.
- **Functionality:**
  - Receive and process client commands over a provided network interface.
  - Support essential database operations such as `CREATE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`, and `JOIN`.
  - Ensure data persistence using tab-separated text files for storage.
  - Handle errors gracefully without crashing the server.

### Key Features

1. **Server Architecture:**
   - Main server class: `DBServer`.
   - Handles incoming commands via the `handleCommand(String)` method.
   - Implements a simplified query language for database interactions.

2. **Query Language:**
   - Supports commands including:
     - `CREATE`: Create databases or tables.
     - `INSERT`: Add rows to a table.
     - `SELECT`: Retrieve records with optional conditions.
     - `UPDATE`: Modify existing records.
     - `DELETE`: Remove records.
     - `JOIN`: Perform inner joins between tables.
   - Case-insensitive keywords and flexible whitespace formatting.

3. **Data Storage:**
   - Tables are stored as tab-separated files in a designated directory.
   - Data integrity ensured through unique and non-recyclable primary keys.
   - Case-insensitive handling of database and table names.

4. **Error Handling:**
   - Differentiates between successful (`[OK]`) and erroneous (`[ERROR]`) responses.
   - Provides meaningful error messages for malformed or invalid queries.

5. **Agile Development Process:**
   - Version control with GitHub.
   - Continuous integration with frequent commits to maintain a functional master branch.

### Setup and Execution

#### Prerequisites
- Java Development Kit (JDK) 17 or higher.
- Maven build tool.

#### Build and Run
Navigate to the project directory and use Maven to compile and execute the server:

```bash
mvnw clean compile exec:java@server
```

Example Commands
Create a database:
```bash
CREATE DATABASE my_database;
```

Create a table:
```bash
CREATE TABLE my_table (id, name, age);
```

Insert data:
```bash
INSERT INTO my_table VALUES (1, 'Alice', 30);
```

Query data:
```bash
SELECT * FROM my_table;
```

## Simple Text Adventure Game (STAG) Assignment

This project involves building a versatile, general-purpose socket-server game engine for text adventure games. The server communicates with clients to process commands, manage game state, and provide responses. The game engine is configurable through external files, allowing for various game scenarios.

### Project Overview

- **Purpose:** Develop a robust text adventure game engine capable of handling diverse game configurations.
- **Technologies:** Java, Maven, JPGD library for DOT file parsing, JAXP for XML processing.
- **Functionality:**
  - Process client commands for game interaction.
  - Support multiple players in a shared game environment.
  - Load game configurations dynamically from provided entity and action files.
  - Ensure gameplay flexibility and natural command interpretation.

### Key Features

1. **Game Engine Server:**
   - Main server class: `GameServer`.
   - Processes commands via the `handleCommand(String)` method.
   - Communicates with clients over a network connection.

2. **Game Entities:**
   - Represented in a DOT file, parsed using the JPGD library.
   - Types of entities include:
     - Locations
     - Artefacts
     - Furniture
     - Characters
     - Players
   - Supports unique identifiers for entities and special locations like the `storeroom`.

3. **Game Actions:**
   - Configured using XML files, parsed with JAXP.
   - Actions include:
     - Trigger phrases
     - Subject, consumed, and produced entities
     - Narrations for feedback
   - Supports dynamic changes in the game world based on player actions.

4. **Standard Commands:**
   - Built-in commands:
     - `inventory` (`inv`): View carried artefacts.
     - `get`: Pick up artefacts.
     - `drop`: Drop artefacts.
     - `goto`: Move to a specified location.
     - `look`: View the current location's details.
   - Custom actions defined in the actions file.

5. **Flexible Command Interpretation:**
   - Case-insensitive.
   - Handles decorated, reordered, and partial commands.
   - Detects and avoids ambiguous or invalid commands.

6. **Multi-Player Support:**
   - Supports multiple players in the same game instance.
   - Tracks individual player states (location, inventory, health).

### Setup and Execution

#### Prerequisites
- Java Development Kit (JDK) 17 or higher.
- Maven build tool.

Build and Run
Navigate to the project directory and use Maven to compile and execute the server:
```bash
mvnw clean compile exec:java@server
```
To connect a client:
```bash
mvnw exec:java@client -Dexec.args="player_name"
```

Example Commands
View inventory:
```bash
inventory
```

Pick up an item:
```bash
get key
```

Move to a location:
```bash
goto forest
```

View the current location:
```bash
look
```