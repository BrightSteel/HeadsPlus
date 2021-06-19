package io.github.thatsmusic99.headsplus.listeners;

import io.github.thatsmusic99.headsplus.HeadsPlus;
import io.github.thatsmusic99.headsplus.api.events.HeadCraftEvent;
import io.github.thatsmusic99.headsplus.config.MainConfig;
import io.github.thatsmusic99.headsplus.managers.PersistenceManager;
import io.github.thatsmusic99.headsplus.util.FlagHandler;
import io.github.thatsmusic99.headsplus.util.events.HeadsPlusEventExecutor;
import io.github.thatsmusic99.headsplus.util.events.HeadsPlusListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.meta.SkullMeta;

public class HPPlayerCraftEvent extends HeadsPlusListener<InventoryClickEvent> {

    @Override
    public void init() {
        Bukkit.getPluginManager().registerEvent(InventoryClickEvent.class, this, EventPriority.NORMAL,
                new HeadsPlusEventExecutor(InventoryClickEvent.class, "RecipeHandlingEvent", this), HeadsPlus.getInstance());
    }

    @Override
    public boolean shouldEnable() {
        return MainConfig.get().getMainFeatures().ENABLE_CRAFTING;
    }

    public void onEvent(InventoryClickEvent e) {
        addData("player", e.getWhoClicked().getName());
        addData("inventory-type", e.getInventory().getType().name());
        addData("slot", e.getRawSlot());

        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;
        if (!isCorrectSlot(e)) return;
        if (!(e.getCurrentItem().getItemMeta() instanceof SkullMeta)) return;
        String type = PersistenceManager.get().getSellType(e.getCurrentItem());
        if (type == null || type.isEmpty()) return;
        if (!player.hasPermission("headsplus.craft")) {
            e.getWhoClicked().sendMessage(ChatColor.RED + "You cannot craft heads!");
            e.setCancelled(true);
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            if (!FlagHandler.canCraft(e.getWhoClicked().getLocation(), EntityType.valueOf(type))) {
                e.getWhoClicked().sendMessage(ChatColor.RED + "You cannot craft heads!");
                e.setCancelled(true);
                return;
            }
        }
        fireEvent(e);
	}

	private int shift(InventoryClickEvent e) {
	    int amount;
	    if (!e.isShiftClick()) return 1;
	    int a = 0;
	    if (e.getInventory().getType().equals(InventoryType.WORKBENCH)) {
	        for (int i = 1; i <= 9; i++) {
	            if (e.getInventory().getItem(i) != null) {
	                a += e.getInventory().getItem(i).getAmount();
	            }
	        }
	    } else {
	        for (int i = 80; i <= 83; i++) {
	            if (e.getInventory().getItem(i) != null) {
	                a += e.getInventory().getItem(i).getAmount();
	            }
	        }
	    }
	    if (a % 2 == 0) {
	        amount = a / 2;
	    } else {
	        amount = (a - 1) / 2;
	    }
        return amount;
    }

    private void fireEvent(InventoryClickEvent e) {
        HeadCraftEvent event;
        int amount = shift(e);
        String type = PersistenceManager.get().getSellType(e.getCurrentItem());
        event = new HeadCraftEvent((Player) e.getWhoClicked(), e.getCurrentItem(), e.getWhoClicked().getWorld(), e.getWhoClicked().getLocation(), amount, type);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            e.setCancelled(true);
        }
    }

    private boolean isCorrectSlot(InventoryClickEvent e) {
        switch (e.getInventory().getType()) {
            case CRAFTING:
                if (e.getWhoClicked().getGameMode() != GameMode.SURVIVAL) return false;
            case WORKBENCH:
                return e.getRawSlot() == 0;
            case FURNACE:
            case SMITHING:
                return e.getRawSlot() == 2;

        }
        return false;
    }
}
