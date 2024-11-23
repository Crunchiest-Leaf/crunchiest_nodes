# CrunchiestNodes

CrunchiestNodes is a Minecraft plugin designed to manage and interact with custom mining nodes in the game. It provides commands for registering, deleting, and highlighting nodes, making it easier for staff members to manage nodes within the game.

## Features

- **Node Registration Mode**: Allows players to register blocks as nodes.
- **Node Deletion Mode**: Enables players to delete registered nodes.
- **Node Highlighting Mode**: Highlights nodes within 30 blocks of the player for 10 seconds.

## Commands

### `/nodebuilder <cooldown> <global|player> <tool|better>`

Enables node registration mode. Click a block to register it as a node. Run the command again to disable.

- **cooldown**: The cooldown time in minutes.
- **global|player**: Specifies whether the node is global or player-specific.
- **tool|better**: Specifies whether a generic form of the tool, or the tool level used.
- **Main Hand**: Hold the tool required to mine the node.
- **Off Hand**: Hold the item stack that the node will drop.

**Usage**: `/nodebuilder 5 global tool`

### `/nodedeleter`

Enables node deletion mode. Left-click nodes to delete them. Run the command again to disable.

**Usage**: `/nodedeleter`

### `/nodehighlighter`

Highlights nodes within 30 blocks of the player for 10 seconds. Nodes will flash between their base block type and white concrete.

**Usage**: `/nodehighlighter`

## Installation

1. Download the plugin jar file.
2. Place the jar file in the `plugins` folder of your Minecraft server.
3. Start or restart the server to load the plugin.

## Configuration

No additional configuration is required. The plugin uses default settings.

## Permissions

- `crunchiestnodes.nodebuilder`: Permission to use the `/nodebuilder` command.
- `crunchiestnodes.nodedeleter`: Permission to use the `/nodedeleter` command.
- `crunchiestnodes.nodehighlighter`: Permission to use the `/nodehighlighter` command.

## Developer Information

### Main Class

The main class for the plugin is `CrunchiestNodes`.

```java