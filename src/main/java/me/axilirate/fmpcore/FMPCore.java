package me.axilirate.fmpcore;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.*;

import java.util.UUID;


public final class FMPCore extends JavaPlugin implements Listener {


    NamespacedKey buildCooldownKey = new NamespacedKey(this, "buildcooldown");
    NamespacedKey breakCooldownKey = new NamespacedKey(this, "breakcooldown");



    @Override
    public void onEnable() {



        getServer().getPluginManager().registerEvents(this, this);


        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getServer().getOnlinePlayers()){


                    PersistentDataContainer playerData = player.getPersistentDataContainer();
                    int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);
                    int playerBreakCooldown = playerData.get(breakCooldownKey, PersistentDataType.INTEGER);

                    Objective objective = player.getScoreboard().getObjective("buildbreakcooldowns");

                    Score buildScore = objective.getScore("build cooldown: ");
                    buildScore.setScore(playerBuildCooldown);

                    Score breakScore = objective.getScore("break cooldown: ");
                    breakScore.setScore(playerBreakCooldown);




                    playerData.set(buildCooldownKey, PersistentDataType.INTEGER, playerBuildCooldown - 1);

                    playerData.set(breakCooldownKey, PersistentDataType.INTEGER, playerBreakCooldown - 1);

                    if (playerBuildCooldown < 1){
                        playerData.set(buildCooldownKey, PersistentDataType.INTEGER, 0);
                    }

                    if (playerBreakCooldown < 1){
                        playerData.set(breakCooldownKey, PersistentDataType.INTEGER, 0);
                    }


                    updateDisplayTitle(player);



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

        Boolean newKey = false;

        if (!playerData.has(buildCooldownKey, PersistentDataType.INTEGER)){
            playerData.set(buildCooldownKey, PersistentDataType.INTEGER, 0);
            newKey = true;
        }

        if (!playerData.has(breakCooldownKey, PersistentDataType.INTEGER)){
            playerData.set(breakCooldownKey, PersistentDataType.INTEGER, 0);
            newKey = true;
        }


        if (newKey){
            createScoreboard(player);
            return;
        }




        int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);
        int playerBreakCooldown = playerData.get(breakCooldownKey, PersistentDataType.INTEGER);


        int offlineTime = (int) ((System.currentTimeMillis() - player.getLastPlayed()) * 0.001);


        int totalBuildCooldown = playerBuildCooldown - offlineTime;
        if (totalBuildCooldown < 0){
            totalBuildCooldown = 0;
        }


        int totalBreakCooldown = playerBreakCooldown - offlineTime;
        if (totalBreakCooldown < 0){
            totalBreakCooldown = 0;
        }


        playerData.set(buildCooldownKey, PersistentDataType.INTEGER, totalBuildCooldown);
        playerData.set(breakCooldownKey, PersistentDataType.INTEGER, totalBreakCooldown);



        createScoreboard(player);


    }





    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        PersistentDataContainer playerData = player.getPersistentDataContainer();
        int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);
        int playerBreakCooldown = playerData.get(breakCooldownKey, PersistentDataType.INTEGER);

        UUID uuid = player.getUniqueId();

        if (playerBuildCooldown > 0){
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTaskLater(this, () -> {
                if (!player.isOnline()) {
                    String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
                    User user = DiscordUtil.getUserById(discordId);

                    DiscordUtil.privateMessage(user, "Your build cooldown is over.");
                }
            }, 20L * playerBuildCooldown);
        }


        if (playerBreakCooldown > 0){
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTaskLater(this, () -> {
                if (!player.isOnline()) {
                    String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
                    User user = DiscordUtil.getUserById(discordId);

                    DiscordUtil.privateMessage(user, "Your break cooldown is over.");
                }
            }, 20L * playerBreakCooldown);
        }








    }





    @EventHandler
    public void onBlockPlaceEvent(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        Location location = player.getLocation();

        if (Math.abs(location.getBlockX()) > 2500 || Math.abs(location.getBlockZ()) > 2500){
            return;
        }



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
        Location location = player.getLocation();


        if (Math.abs(location.getBlockX()) > 2500 || Math.abs(location.getBlockZ()) > 2500){
            return;
        }


        PersistentDataContainer playerData = player.getPersistentDataContainer();
        int playerBreakCooldown = playerData.get(breakCooldownKey, PersistentDataType.INTEGER);



        if (playerBreakCooldown > 0 && !player.isOp()){
            event.setCancelled(true);
            return;
        }

        playerData.set(breakCooldownKey, PersistentDataType.INTEGER, 300);

    }



    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();

        Location location = player.getLocation();

        if (Math.abs(location.getBlockX()) > 2500 || Math.abs(location.getBlockZ()) > 2500){
            return;
        }


        PersistentDataContainer playerData = player.getPersistentDataContainer();
        int playerBreakCooldown = playerData.get(breakCooldownKey, PersistentDataType.INTEGER);



        if (playerBreakCooldown > 0 && !player.isOp()){
            event.setCancelled(true);
            return;
        }

        playerData.set(breakCooldownKey, PersistentDataType.INTEGER, 300);

    }






    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player){
            Location location = event.getEntity().getLocation();
            if (Math.abs(location.getBlockX()) < 1000 && Math.abs(location.getBlockZ()) < 1000){
                event.setCancelled(true);
            }
        }

    }





    public void createScoreboard(Player player){

        PersistentDataContainer playerData = player.getPersistentDataContainer();
        int playerBuildCooldown = playerData.get(buildCooldownKey, PersistentDataType.INTEGER);
        int playerBreakCooldown = playerData.get(breakCooldownKey, PersistentDataType.INTEGER);


        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("buildbreakcooldowns", "dummy", "");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);


        Score buildScore = objective.getScore("build cooldown: ");
        Score breakScore = objective.getScore("break cooldown: ");

        buildScore.setScore(playerBuildCooldown);
        breakScore.setScore(playerBreakCooldown);


        player.setScoreboard(scoreboard);
        updateDisplayTitle(player);
    }




    public void updateDisplayTitle(Player player){

        Objective objective = player.getScoreboard().getObjective("buildbreakcooldowns");

        Location location = player.getLocation();

        int blocksLeft = Math.max(Math.abs(location.getBlockX()), Math.abs(location.getBlockZ()));

        if (Math.abs(location.getBlockX()) > 2500 || Math.abs(location.getBlockZ()) > 2500){

            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("Protection: Off | Slow Mode: Off"));

            objective.setDisplayName(ChatColor.BOLD + "Last Zone In " + Integer.toString(blocksLeft - 2500) + " Blocks");

            return;
        }





        if (Math.abs(location.getBlockX()) > 1000 || Math.abs(location.getBlockZ()) > 1000){

            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("Protection: Off | Slow Mode: On"));



            objective.setDisplayName(ChatColor.BOLD + "Next Zone In " + Integer.toString(2500 - blocksLeft) + " Blocks");
            return;
        }


        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent("Protection: ON | Slow Mode: ON"));

        objective.setDisplayName(ChatColor.BOLD + "Next Zone In " + Integer.toString(1000 - blocksLeft) + " Blocks");


    }






}


