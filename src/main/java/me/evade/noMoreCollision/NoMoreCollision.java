package me.evade.noMoreCollision;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NoMoreCollision extends JavaPlugin implements Listener {
    private static final String TEAM_NAME = "nocollision";
    private static final String GUI_TITLE = "§6World Collision Settings";
    private Map<UUID, Boolean> playerCollisionStates = new HashMap<>();
    private Map<String, Boolean> worldCollisionStates = new HashMap<>();
    private boolean globalCollisionState = true;
    private Scoreboard scoreboard;
    private Team noCollisionTeam;
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("collision").setExecutor(new CollisionCommand(this));
        setupTeam();
        setupDataFile();
        loadData();
        for (World world : Bukkit.getWorlds()) {
            if (!worldCollisionStates.containsKey(world.getName())) {
                worldCollisionStates.put(world.getName(), true);
            }
        }
        getLogger().info("NoMoreCollision has been enabled!");
    }

    @Override
    public void onDisable() {
        saveData();
        resetAllPlayersCollision();
        getLogger().info("NoMoreCollision has been disabled!");
    }

    private void setupDataFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                dataConfig = new YamlConfiguration();
                dataConfig.set("global-collision", true);
                dataConfig.set("worlds", new HashMap<>());
                dataConfig.set("players", new HashMap<>());
                dataConfig.save(dataFile);
            } catch (IOException e) {
                getLogger().severe("Could not create data.yml file!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        globalCollisionState = dataConfig.getBoolean("global-collision", true);

        if (dataConfig.isConfigurationSection("worlds")) {
            for (String world : dataConfig.getConfigurationSection("worlds").getKeys(false)) {
                boolean state = dataConfig.getBoolean("worlds." + world, true);
                worldCollisionStates.put(world, state);
            }
        }

        if (dataConfig.isConfigurationSection("players")) {
            for (String playerUUID : dataConfig.getConfigurationSection("players").getKeys(false)) {
                boolean state = dataConfig.getBoolean("players." + playerUUID, true);
                playerCollisionStates.put(UUID.fromString(playerUUID), state);
            }
        }
    }

    public void saveData() {
        dataConfig.set("global-collision", globalCollisionState);

        for (Map.Entry<String, Boolean> entry : worldCollisionStates.entrySet()) {
            dataConfig.set("worlds." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<UUID, Boolean> entry : playerCollisionStates.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Could not save data to " + dataFile);
            e.printStackTrace();
        }
    }

    private void setupTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getMainScoreboard();

        if (scoreboard.getTeam(TEAM_NAME) != null) {
            scoreboard.getTeam(TEAM_NAME).unregister();
        }

        noCollisionTeam = scoreboard.registerNewTeam(TEAM_NAME);
        noCollisionTeam.setAllowFriendlyFire(true);
        noCollisionTeam.setCanSeeFriendlyInvisibles(false);
        noCollisionTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    public void toggleGlobalCollision() {
        globalCollisionState = !globalCollisionState;

        for (Player player : Bukkit.getOnlinePlayers()) {
            setPlayerCollision(player, globalCollisionState);
        }

        saveData();
    }

    public void toggleWorldCollision(Player player, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        boolean currentState = getWorldCollisionState(worldName);
        boolean newState = !currentState;
        setWorldCollisionState(worldName, newState);

        for (Player worldPlayer : world.getPlayers()) {
            setPlayerCollision(worldPlayer, newState);
        }

        String status = newState ? "enabled" : "disabled";
        player.sendMessage("§6[NoMoreCollision] §eCollision has been " + status + " for all players in world '" + worldName + "'.");

        saveData();
    }

    public void togglePlayerCollision(Player player) {
        UUID playerUUID = player.getUniqueId();
        boolean currentState = playerCollisionStates.getOrDefault(playerUUID, true);
        boolean newState = !currentState;

        setPlayerCollision(player, newState);
        playerCollisionStates.put(playerUUID, newState);

        String status = newState ? "enabled" : "disabled";
        player.sendMessage("§6[NoMoreCollision] §eCollision has been " + status + " for you.");

        saveData();
    }

    public void setPlayerCollision(Player player, boolean enabled) {
        if (enabled) {
            noCollisionTeam.removeEntry(player.getName());
        } else {
            noCollisionTeam.addEntry(player.getName());
        }
    }

    private void resetAllPlayersCollision() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (noCollisionTeam.hasEntry(player.getName())) {
                noCollisionTeam.removeEntry(player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (playerCollisionStates.containsKey(playerUUID)) {
            setPlayerCollision(player, playerCollisionStates.get(playerUUID));
        } else {
            String worldName = player.getWorld().getName();
            boolean worldState = getWorldCollisionState(worldName);
            setPlayerCollision(player, worldState);

            if (!globalCollisionState) {
                setPlayerCollision(player, false);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        String worldName = player.getWorld().getName();
        boolean worldState = getWorldCollisionState(worldName);

        if (!playerCollisionStates.containsKey(playerUUID)) {
            setPlayerCollision(player, worldState);

            String status = worldState ? "enabled" : "disabled";
            player.sendMessage("§6[NoMoreCollision] §eCollision is " + status + " in this world.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (playerCollisionStates.containsKey(playerUUID)) {
            saveData();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE)) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (clickedItem.getType() == Material.LIME_WOOL || clickedItem.getType() == Material.RED_WOOL) {
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasDisplayName()) {
                String worldName = clickedItem.getItemMeta().getDisplayName().substring(2);
                toggleWorldCollision(player, worldName);

                int currentPage = 0;
                if (title.contains("(Page ")) {
                    String pageStr = title.substring(title.indexOf("(Page ") + 6, title.indexOf("/")).trim();
                    try {
                        currentPage = Integer.parseInt(pageStr) - 1;
                    } catch (NumberFormatException e) {
                    }
                }

                openCollisionGUI(player, currentPage);
            }
        }
        else if (clickedItem.getType() == Material.ARROW) {
            String arrowName = clickedItem.getItemMeta().getDisplayName();

            int currentPage = 0;
            if (title.contains("(Page ")) {
                String pageStr = title.substring(title.indexOf("(Page ") + 6, title.indexOf("/")).trim();
                try {
                    currentPage = Integer.parseInt(pageStr) - 1;
                } catch (NumberFormatException e) {
                }
            }

            if (arrowName.contains("Previous")) {
                openCollisionGUI(player, currentPage - 1);
            } else if (arrowName.contains("Next")) {
                openCollisionGUI(player, currentPage + 1);
            }
        }
    }

    public void openCollisionGUI(Player player) {
        openCollisionGUI(player, 0);
    }

    public void openCollisionGUI(Player player, int page) {
        List<World> worlds = Bukkit.getWorlds();
        int totalPages = (int) Math.ceil(worlds.size() / 7.0);

        if (page >= totalPages) {
            page = 0;
        }

        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE + " (Page " + (page + 1) + "/" + Math.max(1, totalPages) + ")");

        int start = page * 7;
        int worldsPerPage = (page == 0) ? 8 : 7;
        int end = Math.min(start + worldsPerPage, worlds.size());

        for (int i = start; i < end; i++) {
            int adjustedIndex = i - start;
            int slot;

            if (page == 0) {
                slot = adjustedIndex;
            } else {
                slot = adjustedIndex + 1;
            }

            World world = worlds.get(i);
            boolean isEnabled = getWorldCollisionState(world.getName());

            Material material = isEnabled ? Material.LIME_WOOL : Material.RED_WOOL;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§f" + world.getName());

            List<String> lore = new ArrayList<>();
            lore.add(isEnabled ? "§aCollision Enabled" : "§cCollision Disabled");
            lore.add("§7Click to toggle");
            meta.setLore(lore);

            item.setItemMeta(meta);
            gui.setItem(slot, item);
        }

        if (totalPages > 1) {
            if (page > 0) {
                ItemStack prevArrow = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevArrow.getItemMeta();
                prevMeta.setDisplayName("§e← Previous Page");
                prevArrow.setItemMeta(prevMeta);
                gui.setItem(0, prevArrow);
            }

            if (page < totalPages - 1) {
                ItemStack nextArrow = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextArrow.getItemMeta();
                nextMeta.setDisplayName("§e→ Next Page");
                nextArrow.setItemMeta(nextMeta);
                gui.setItem(8, nextArrow);
            }
        }

        player.openInventory(gui);
    }

    public boolean getGlobalCollisionState() {
        return globalCollisionState;
    }

    public boolean getPlayerCollisionState(Player player) {
        return playerCollisionStates.getOrDefault(player.getUniqueId(),
                getWorldCollisionState(player.getWorld().getName()));
    }

    public boolean getWorldCollisionState(String worldName) {
        return worldCollisionStates.getOrDefault(worldName, true);
    }

    public void setWorldCollisionState(String worldName, boolean enabled) {
        worldCollisionStates.put(worldName, enabled);
    }

    public List<String> getCommandSuggestions(String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("self");
            suggestions.add("global");
            suggestions.add("status");
            suggestions.add("help");

            return filterSuggestions(args[0], suggestions);
        }

        return suggestions;
    }

    private List<String> filterSuggestions(String current, List<String> options) {
        if (current.isEmpty()) {
            return options;
        }

        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(current.toLowerCase())) {
                filtered.add(option);
            }
        }

        return filtered;
    }
}