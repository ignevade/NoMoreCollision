# NoMoreCollision

A Minecraft Paper plugin that allows players to control entity collision on a per-world, global, or player basis with an easy-to-use GUI.

## Features

- **World-specific Collision**: Enable or disable collision separately for each world
- **Player-specific Settings**: Allow players to toggle their own collision on/off
- **Global Collision Control**: One command to toggle collision for the entire server
- **User-friendly GUI**: Visual interface for toggling collision in different worlds
- **Permission-Based Access**: Granular control over who can modify collision settings

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/collision` | `nomorecollision.toggle` | Open GUI to toggle collision for worlds |
| `/collision self` | `nomorecollision.self` | Toggle collision just for yourself |
| `/collision global` | `nomorecollision.global` | Toggle collision for all players on the server |
| `/collision status` | `nomorecollision.status` | Check collision status |
| `/collision help` | `nomorecollision.toggle` | Show help menu |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `nomorecollision.toggle` | Access to base command and GUI | op |
| `nomorecollision.self` | Ability to toggle personal collision | op |
| `nomorecollision.global` | Ability to toggle global collision | op |
| `nomorecollision.status` | Ability to check collision status | true |

## Data Storage

All collision settings are stored in a `data.yml` file with three main sections:
- `global-collision`: Server-wide collision setting
- `worlds`: World-specific collision settings
- `players`: Player-specific collision settings

### GUI Navigation

The GUI shows:
- Lime wool blocks for worlds with collision enabled
- Red wool blocks for worlds with collision disabled
- Navigation arrows for browsing multiple pages of worlds
![](https://files.catbox.moe/8e978b.png) 
![](https://files.catbox.moe/iuct66.png)
![](https://files.catbox.moe/2x55f5.png)
## Installation

1. Download the NoMoreCollision.jar file
2. Place it in your server's plugins folder
3. Restart your server
4. Data file will be automatically generated at `plugins/NoMoreCollision/data.yml`
