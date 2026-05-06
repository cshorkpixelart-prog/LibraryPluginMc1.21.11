package com.infinitylibrary.listener;

import com.infinitylibrary.InfinityLibraryPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BookListener implements Listener {
    private final InfinityLibraryPlugin plugin;
    public BookListener(InfinityLibraryPlugin plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true) public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (plugin.getSelectionManager().handle(e.getPlayer(), e.getAction(), e.getClickedBlock())) { e.setCancelled(true); return; }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType() == Material.LECTERN) { plugin.getGuiManager().openLectern(e.getPlayer()); e.setCancelled(true); return; }
            if (e.getClickedBlock().getType() == Material.CHISELED_BOOKSHELF) {
                ItemStack item = e.getItem();
                if (item != null && item.getType() == Material.WRITTEN_BOOK) plugin.getBookStorageManager().recordBook(e.getPlayer(), item.clone(), e.getClickedBlock().getLocation());
                return;
            }
        }
        ItemStack item = e.getItem();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && item != null && item.getType() == Material.WRITTEN_BOOK && !plugin.getBookStorageManager().canRead(e.getPlayer(), item)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "This library book is private. You can carry it, but only its linked owner can read it.");
        }
    }

    @EventHandler public void onSearchChat(AsyncPlayerChatEvent e) {
        if (plugin.getBookStorageManager().handleSearchChat(e.getPlayer(), e.getMessage())) e.setCancelled(true);
    }
}
