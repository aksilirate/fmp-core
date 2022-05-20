package me.axilirate.buildcooldown;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


public final class BuildCooldown extends JavaPlugin implements Listener {


    NamespacedKey buildCooldownKey = new NamespacedKey(this, "buildcooldown");

    @Override
    public void onEnable() {


        getServer().getPluginManager().registerEvents(this, this);


        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getServer().getOnlinePlayers()){
                    PersistentDataContainer playerData = player.getPersistentDataContainer();
                    int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);


                    if (playerBuildCooldown > 0){
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                new TextComponent("Build Cooldown: " + Integer.toString(playerBuildCooldown)));

                        playerData.set(buildCooldownKey, PersistentDataType.INTEGER, playerBuildCooldown - 1);
                    }


                }




                }
        }.runTaskTimer(this, 0, 20);





    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }



    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer playerData = player.getPersistentDataContainer();


        if (!playerData.has(buildCooldownKey, PersistentDataType.INTEGER)){
            playerData.set(buildCooldownKey, PersistentDataType.INTEGER, 0);
        }
    }



    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer playerData = player.getPersistentDataContainer();
        int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);



        if (playerBuildCooldown > 0 && !player.isOp()){
            event.setCancelled(true);
            return;
        }

        playerData.set(buildCooldownKey, PersistentDataType.INTEGER, 300);

    }



    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PersistentDataContainer playerData = player.getPersistentDataContainer();
        int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);



        if (playerBuildCooldown > 0 && !player.isOp()){
            event.setCancelled(true);
            return;
        }

        playerData.set(buildCooldownKey, PersistentDataType.INTEGER, 300);

    }

}


