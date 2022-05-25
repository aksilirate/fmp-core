package me.axilirate.fmpcore;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.*;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.UUID;


public final class FMPCore extends JavaPlugin implements Listener {


    NamespacedKey buildCooldownKey = new NamespacedKey(this, "buildcooldown");
    NamespacedKey breakCooldownKey = new NamespacedKey(this, "breakcooldown");


    float hungerMultiplayer = 0.00036f;



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


        Color color = null;
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (discordId != null){
            Guild guild = DiscordSRV.getPlugin().getMainGuild();
            Member member = guild.getMemberById(discordId);
            color = member.getColor();
        }



        if (color == null){
            color = Color.WHITE;
        }




        player.setDisplayName(ChatColor.of(color) + player.getName());
        player.setPlayerListName(ChatColor.of(color) + player.getName());


        PersistentDataContainer playerData = player.getPersistentDataContainer();



        player.setBedSpawnLocation(null);


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

            if (event.getEntity().getWorld().getName().equals("world_the_end")){
                return;
            }

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


        int x = Math.abs(location.getBlockX());
        int z = Math.abs(location.getBlockZ());

        float rate = getHungerRate(player);



        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        String formattedRate = decimalFormat.format(rate);

        int blocksLeft = Math.max(Math.abs(x), Math.abs(z));

        if (Math.abs(location.getBlockX()) > 2500 || Math.abs(location.getBlockZ()) > 2500){

            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("Protection: Off | Slow Mode: Off | Hunger Rate: " + formattedRate));

            objective.setDisplayName(ChatColor.BOLD + "2nd Layer In " + Integer.toString(blocksLeft - 2500) + " Blocks");

            return;
        }





        if (Math.abs(location.getBlockX()) > 1000 || Math.abs(location.getBlockZ()) > 1000){

            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("Protection: Off | Slow Mode: On | Hunger Rate: " + formattedRate));



            objective.setDisplayName(ChatColor.BOLD + "3rd Layer In " + Integer.toString(2500 - blocksLeft) + " Blocks");
            return;
        }




        objective.setDisplayName(ChatColor.BOLD + "2nd Layer In " + Integer.toString(1000 - blocksLeft) + " Blocks");


        if (player.getWorld().getName().equals("world_the_end")){
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    new TextComponent("Protection: Off | Slow Mode: On | Hunger Rate: " + formattedRate));


            return;
        }


        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                new TextComponent("Protection: On | Slow Mode: On | Hunger Rate: " + formattedRate));




    }



    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event){
        Player player = event.getPlayer();

        UUID uuid = player.getUniqueId();
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        Member member = guild.getMemberById(discordId);
        Color color = member.getColor();


        if (color == null){
            color = Color.WHITE;
        }


        event.setFormat(ChatColor.of(color) + player.getDisplayName() + ChatColor.WHITE + " » " + event.getMessage());
    }





    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event){
        event.setRespawnLocation(null);
    }


    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event){
        event.setCancelled(true);
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){

        if (event.getClickedBlock() == null){
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK){
            return;
        }


        if (event.getClickedBlock().getBlockData() instanceof org.bukkit.block.data.type.Bed) {
            event.setCancelled(true);
        }
    }



    @EventHandler
    public void onEntityExhaustion(EntityExhaustionEvent event){

        if (event.getEntity() instanceof Player){
            Player player = (((Player) event.getEntity()).getPlayer());


            float rate = getHungerRate(player);


            event.setExhaustion(event.getExhaustion() * rate);





        }


    }





    public float getHungerRate(Player player){
        Location location = player.getLocation();
        int x = Math.abs(location.getBlockX());
        int z = Math.abs(location.getBlockZ());



        if (player.getWorld().getName().equals("world_the_end")){
            return 1.0f + (Math.max((float) x, (float) z) * hungerMultiplayer * 0.1f);
        }


        return Math.max((float) x, (float) z) * hungerMultiplayer;

    }








}


