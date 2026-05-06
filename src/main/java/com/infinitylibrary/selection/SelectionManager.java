package com.infinitylibrary.selection;

import com.infinitylibrary.InfinityLibraryPlugin;
import com.infinitylibrary.model.ConnectionPoint;
import com.infinitylibrary.model.Vector3i;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SelectionManager {
    public enum Mode { POS1, POS2 }

    private final InfinityLibraryPlugin plugin;
    private final NamespacedKey selectionWandKey;
    private final NamespacedKey connectionWandKey;
    private final Map<UUID, Mode> modes = new HashMap<>();
    private final Map<UUID, ConnectionSettings> connectionSettings = new HashMap<>();
    private final Map<UUID, List<ConnectionPoint>> stagedConnections = new HashMap<>();
    private final Map<UUID, Integer> connectionCounters = new HashMap<>();

    public SelectionManager(InfinityLibraryPlugin plugin) {
        this.plugin = plugin;
        this.selectionWandKey = new NamespacedKey(plugin, "selection_wand");
        this.connectionWandKey = new NamespacedKey(plugin, "connection_wand");
    }

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Infinity Library Selection Wand");
        meta.getPersistentDataContainer().set(selectionWandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        return wand;
    }

    public ItemStack createConnectionWand() {
        ItemStack wand = new ItemStack(Material.END_ROD);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Infinity Library Connection Wand");
        meta.setLore(List.of(ChatColor.GRAY + "Click blocks inside a selected room", ChatColor.GRAY + "to stage connection points."));
        meta.getPersistentDataContainer().set(connectionWandKey, PersistentDataType.BYTE, (byte) 1);
        wand.setItemMeta(meta);
        return wand;
    }

    public boolean isSelectionWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(selectionWandKey, PersistentDataType.BYTE);
    }

    public boolean isConnectionWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(connectionWandKey, PersistentDataType.BYTE);
    }

    public Mode mode(Player player) {
        return modes.getOrDefault(player.getUniqueId(), Mode.POS1);
    }

    public void setMode(Player player, String modeName) {
        Mode mode = Mode.valueOf(modeName.toUpperCase(Locale.ROOT));
        modes.put(player.getUniqueId(), mode);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Selection wand mode set to " + mode.name() + ".");
    }

    public void setConnectionSettings(Player player, BlockFace direction, int width, int height, String prefix) {
        if (!isSupportedDirection(direction)) throw new IllegalArgumentException("Direction must be NORTH/EAST/SOUTH/WEST/UP/DOWN");
        if (width < 1 || height < 1) throw new IllegalArgumentException("Width and height must be positive");
        connectionSettings.put(player.getUniqueId(), new ConnectionSettings(direction, width, height, prefix == null || prefix.isBlank() ? "conn" : prefix));
        player.sendMessage(ChatColor.AQUA + "Connection wand set to " + direction.name() + " " + width + "x" + height + " prefix=" + (prefix == null || prefix.isBlank() ? "conn" : prefix) + ".");
    }

    public List<ConnectionPoint> stagedConnections(Player player) {
        return List.copyOf(stagedConnections.getOrDefault(player.getUniqueId(), List.of()));
    }

    public void addStagedConnection(Player player, ConnectionPoint connectionPoint) {
        stagedConnections.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(connectionPoint);
    }

    public void clearStagedConnections(Player player) {
        stagedConnections.remove(player.getUniqueId());
        connectionCounters.remove(player.getUniqueId());
    }

    public boolean handle(Player player, Action action, Block clickedBlock) {
        if (clickedBlock == null) return false;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (isConnectionWand(held)) return handleConnectionWand(player, action, clickedBlock);
        if (!isSelectionWand(held)) return false;
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return false;
        Mode mode = mode(player);
        if (mode == Mode.POS1 || action == Action.LEFT_CLICK_BLOCK) {
            plugin.getRoomManager().setPos1(player, clickedBlock.getLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Selection position 1 set to " + format(clickedBlock));
        } else {
            plugin.getRoomManager().setPos2(player, clickedBlock.getLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Selection position 2 set to " + format(clickedBlock));
        }
        return true;
    }

    private boolean handleConnectionWand(Player player, Action action, Block clickedBlock) {
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return false;
        try {
            ConnectionSettings settings = connectionSettings.getOrDefault(player.getUniqueId(), new ConnectionSettings(BlockFace.NORTH, 3, 3, "conn"));
            Vector3i relative = plugin.getRoomManager().relativeToSelection(player, clickedBlock.getLocation());
            int number = connectionCounters.merge(player.getUniqueId(), 1, Integer::sum);
            ConnectionPoint connectionPoint = new ConnectionPoint(settings.prefix() + "_" + number, relative, settings.direction(), settings.width(), settings.height());
            addStagedConnection(player, connectionPoint);
            player.sendMessage(ChatColor.AQUA + "Staged connection " + connectionPoint.id() + " at " + relative + " facing " + settings.direction().name() + ".");
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ChatColor.RED + ex.getMessage());
        }
        return true;
    }

    private boolean isSupportedDirection(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.EAST || face == BlockFace.SOUTH || face == BlockFace.WEST || face == BlockFace.UP || face == BlockFace.DOWN;
    }

    private String format(Block block) {
        return block.getX() + ", " + block.getY() + ", " + block.getZ();
    }

    private record ConnectionSettings(BlockFace direction, int width, int height, String prefix) {}
}
