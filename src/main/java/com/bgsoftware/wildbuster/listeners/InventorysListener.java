package com.bgsoftware.wildbuster.listeners;

import com.bgsoftware.wildbuster.Locale;
import com.bgsoftware.wildbuster.WildBusterPlugin;
import com.bgsoftware.wildbuster.api.objects.PlayerBuster;
import com.bgsoftware.wildbuster.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class InventorysListener implements Listener {

    private WildBusterPlugin plugin;

    public InventorysListener(WildBusterPlugin plugin){
        this.plugin = plugin;
    }

    /**
     * The following two events are here for patching a dupe glitch caused
     * by shift clicking and closing the inventory in the same time.
     */

    private Map<UUID, ItemStack> latestClickedItem = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClickMonitor(InventoryClickEvent e){
        if(e.getCurrentItem() != null) {
            latestClickedItem.put(e.getWhoClicked().getUniqueId(), e.getCurrentItem());
            Bukkit.getScheduler().runTaskLater(plugin, () -> latestClickedItem.remove(e.getWhoClicked().getUniqueId()), 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCloseMonitor(InventoryCloseEvent e){
        if(latestClickedItem.containsKey(e.getPlayer().getUniqueId())){
            ItemStack clickedItem = latestClickedItem.get(e.getPlayer().getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                e.getPlayer().getInventory().removeItem(clickedItem);
                //noinspection deprecation
                ((Player) e.getPlayer()).updateInventory();
            }, 1L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if(e.getClickedInventory() == null || e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR || !(e.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) e.getWhoClicked();

        if(e.getInventory().getName().equals(ChatColor.BOLD + "Cancelling Menu")){
            e.setCancelled(true);
            //Player head
            if(e.getRawSlot() < 36){
                UUID playerUUID = UUID.fromString(ChatColor.stripColor(e.getCurrentItem().getItemMeta().getLore().get(1)));
                player.openInventory(ItemUtil.getPlayerMenu(Bukkit.getOfflinePlayer(playerUUID), 0));
            }
            //Previous or Next page
            else if(e.getRawSlot() == 38 || e.getRawSlot() == 42){
                String firstLoreLine = ChatColor.stripColor(e.getInventory().getItem(e.getRawSlot()).getItemMeta().getLore().get(0));
                Matcher matcher;
                if((matcher = Pattern.compile("Page (\\d)").matcher(firstLoreLine)).matches()){
                    int newPage = Integer.valueOf(matcher.group(1));
                    e.getWhoClicked().openInventory(ItemUtil.getCancelMenu(newPage - 1));
                }
            }
        }

        else if(e.getInventory().getName().equals(ChatColor.BOLD + "Player's Active Busters")){
            e.setCancelled(true);
            OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(e.getInventory().getItem(40).getItemMeta().getLore().get(1).split(" ")[1]));
            //Buster item
            if(e.getRawSlot() < 36){
                //"Running at " chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ()
                String chunkString = e.getCurrentItem().getItemMeta().getLore().get(0).split(" ")[2];
                String[] chunkSections = chunkString.split(",");
                Chunk chunk = Bukkit.getWorld(chunkSections[0]).getChunkAt(Integer.valueOf(chunkSections[1]), Integer.valueOf(chunkSections[2]));
                PlayerBuster playerBuster = plugin.getBustersManager().getPlayerBuster(chunk);

                if(playerBuster != null) {
                    playerBuster.performCancel(player);
                }
                else{
                    Locale.INVALID_BUSTER_LOCATION.send(player, chunkString);
                }
            }
            //Previous or Next page
            else if(e.getRawSlot() == 38 || e.getRawSlot() == 42){
                String firstLoreLine = ChatColor.stripColor(e.getInventory().getItem(e.getRawSlot()).getItemMeta().getLore().get(0));
                Matcher matcher;
                if((matcher = Pattern.compile("Page (\\d)").matcher(firstLoreLine)).matches()){
                    int newPage = Integer.valueOf(matcher.group(1));
                    e.getWhoClicked().openInventory(ItemUtil.getPlayerMenu(target, newPage - 1));
                }
            }
        }

    }

}
