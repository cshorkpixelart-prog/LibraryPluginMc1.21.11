package com.infinitylibrary.listener;

import com.infinitylibrary.InfinityLibraryPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {
    private final InfinityLibraryPlugin plugin;
    public GUIListener(InfinityLibraryPlugin plugin) { this.plugin = plugin; }
    @EventHandler public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (title.equals(plugin.getGuiManager().statsTitle())) { e.setCancelled(true); return; }
        if (title.equals(plugin.getGuiManager().roomEditorTitle())) { e.setCancelled(true); return; }
        if (!title.equals(plugin.getGuiManager().lecternTitle())) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getRawSlot() == 13) {
            boolean given = plugin.getBookStorageManager().giveDailyWritableBook(player);
            player.sendMessage(given ? ChatColor.LIGHT_PURPLE + "A private writable library book was added to your inventory." : ChatColor.RED + "You already claimed today's writable book.");
            player.closeInventory();
        } else if (e.getRawSlot() == 11) {
            player.closeInventory();
            plugin.getBookStorageManager().beginSearchPrompt(player);
        } else if (e.getRawSlot() == 15) {
            plugin.getGuiManager().openStats(player);
        }
    }
}
