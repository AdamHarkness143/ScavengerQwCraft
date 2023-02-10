package com.cnaude.scavenger;

import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.cnaude.scavenger.Commands.ScavengerDisable;
import com.cnaude.scavenger.Commands.ScavengerEnable;
import com.cnaude.scavenger.Commands.ScavengerList;
import com.cnaude.scavenger.Commands.ScavengerOff;
import com.cnaude.scavenger.Commands.ScavengerOn;
import com.cnaude.scavenger.Commands.ScavengerReload;
import com.cnaude.scavenger.Commands.ScavengerRestoreInv;
import com.cnaude.scavenger.Commands.ScavengerSaveInv;
import com.cnaude.scavenger.Hooks.ScavengerAuthMeReloaded;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
public class Scavenger extends JavaPlugin {

    public static final String PLUGIN_NAME = "Scavenger";
    public static final String LOG_HEADER = "[" + PLUGIN_NAME + "]";
    private Economy economy = null;
    public RestorationManager rm;
    public boolean battleArena = false;
    public ScavengerIgnoreList ignoreList;
    public boolean configLoaded = false;
    static final Logger LOG = Logger.getLogger("Minecraft");
    public ScavengerConfig config;
    protected ScavengerEventListenerOffline eventListenerOffline;
    private ScavengerEventListenerOnline eventListenerOnline;

    private ScavengerAuthMeReloaded scavengerAuthMeReloaded = null;

    public AuthMeListener authMeListener = null;

    public boolean hasPermission(CommandSender sender, String perm) {
        return sender.hasPermission(perm) || (sender.isOp() && config.opsAllPerms());
    }

    @Override
    public void onEnable() {
        loadConfig();

        for (String s : config.blacklistedWorlds()) {
            logDebug("BlackListedWorld: " + s);
        }

        for (String s : config.whitelistedWorlds()) {
            logDebug("WhiteListedWorld: " + s);
        }

        setupResidence();

        rm = new RestorationManager(this);

        eventListenerOnline = new ScavengerEventListenerOnline(this, rm);
        ignoreList = new ScavengerIgnoreList(this);

        if (config.offlineMode()) {
         if (Bukkit.getServer().getPluginManager().getPlugin("AuthMe") != null) {
                eventListenerOffline = new ScavengerEventListenerOffline(this, rm);
                getServer().getPluginManager().registerEvents(eventListenerOffline, this);

                authMeListener = new AuthMeListener(this);
                getServer().getPluginManager().registerEvents(authMeListener, this);

                scavengerAuthMeReloaded = new ScavengerAuthMeReloaded(this);
                logInfo("Hooked into AuthMe.");
            } else {
                logInfo("Authenticator not detected. Set offline-mode to false or add Authenticator or AuthMe.");
                getServer().getPluginManager().registerEvents(eventListenerOnline, this);
            }
        } else {
            getServer().getPluginManager().registerEvents(eventListenerOnline, this);
            logInfo("Offline-mode is set to false, no Authenticator Hook");
        }

        getCommand("scvrdisable").setExecutor(new ScavengerDisable(this));
        getCommand("scvrenable").setExecutor(new ScavengerEnable(this));
        getCommand("scvrlist").setExecutor(new ScavengerList(this));
        getCommand("scvroff").setExecutor(new ScavengerOff(this));
        getCommand("scvron").setExecutor(new ScavengerOn(this));
        getCommand("scavengerreload").setExecutor(new ScavengerReload(this));
        getCommand("saveinv").setExecutor(new ScavengerSaveInv(this));
        getCommand("restoreinv").setExecutor(new ScavengerRestoreInv(this));

    }

    private void checkForWorldGuard() {
        if (getWorldGuard() != null && config.wgPVPIgnore()) {
            logInfo("WorldGuard detected. Scavenger will not recover items in PVP regions.");
        }
    }

    private void checkForBattleArena() {
        Plugin baPlugin = getServer().getPluginManager().getPlugin("BattleArena");

        if (baPlugin != null) {
            battleArena = true;
            logInfo("BattleArena detected. Scavenger will not recover items in an arena.");
        }
    }



    @Override
    public void onDisable() {
        if (checkForProtocolLib()) {
            rm.save();
            ignoreList.save();
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    private Boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }



    public boolean isFactionsLoaded() {
        return (getServer().getPluginManager().getPlugin("Factions") != null);
    }

    public boolean isDungeonMazeLoaded() {
        return (getServer().getPluginManager().getPlugin("DungeonMaze") != null);
    }

    public boolean checkForProtocolLib() {
        Plugin plugin = getServer().getPluginManager().getPlugin("ProtocolLib");
        return plugin != null;
    }


    public void setupResidence() {
        if (config.residence()) {
            PluginManager pm = getServer().getPluginManager();
            Plugin p = pm.getPlugin("Residence");
            if (p != null) {
                if (!p.isEnabled()) {
                    logInfo("Manually enabling Residence!");
                    pm.enablePlugin(p);
                }
                logInfo("Adding '" + config.resFlag() + "' flag to Residence.");
                FlagPermissions.addResidenceOnlyFlag(config.resFlag());
                FlagPermissions.addFlag(config.resFlag());
            } else {
                logInfo("Residence NOT Installed!");
            }
        } else {
            logDebug("Residence support disabled via config.");
        }
    }

    public WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (plugin != null) {
            if (!plugin.getDescription().getVersion().startsWith("7.")) {
                logInfo("Invalid version of WorldGuard detected. Please use 7.0.0 or newer.");
                plugin = null;
            }
        }

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }
        return (WorldGuardPlugin) plugin;
    }

    public void logInfo(String _message) {
        LOG.log(Level.INFO, String.format("%s %s", LOG_HEADER, _message));
    }

    public void logDebug(String _message) {
        if (config.debugEnabled()) {
            LOG.log(Level.INFO, String.format("%s [DEBUG] %s", LOG_HEADER, _message));
        }
    }

    public void logError(String _message) {
        LOG.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, _message));
    }

    public void loadConfig() {
        if (!this.configLoaded) {
            getConfig().options().copyDefaults(true);
            saveConfig();
            logInfo("Configuration loaded.");
            config = new ScavengerConfig(this, getConfig());
        } else {
            reloadConfig();
            getConfig().options().copyDefaults(false);
            config = new ScavengerConfig(this, getConfig());
            logInfo("Configuration reloaded.");
        }

        if (config.economyEnabled()) {
            Plugin x = getServer().getPluginManager().getPlugin("Vault");
            if (x != null && x instanceof Vault) {
                if (setupEconomy()) {
                    logInfo("Scavenger has linked to " + economy.getName() + " through Vault");
                    if (config.percent()) {
                        if (config.addMin()) {
                            logInfo("Item recovery fee: " + config.percentCost()
                                    + "% + " + config.minCost());
                        } else {
                            logInfo("Item recovery fee: " + config.percentCost()
                                    + "% (Min: " + config.minCost()
                                    + ") (Max: " + config.maxCost() + ")");
                        }
                    } else {
                        logInfo("Item recovery fee: " + config.restoreCost());
                    }
                } else {
                    logError("Vault could not find an Economy plugin installed!");
                }
            } else {
                logInfo("Scavenger relies on Vault for economy support and Vault isn't installed!");
                logInfo("See http://dev.bukkit.org/server-mods/vault/");
                logInfo("If you don't want economy support, set 'Economy: Enabled' to false in Scavenger config.");
            }
        } else {
            logInfo("Economy disabled. Item recovery will be free.");
        }
        configLoaded = true;
    }



    private String headerStr() {
        return ChatColor.translateAlternateColorCodes('&', config.msgHeader());
    }

    public void message(Player player, String message) {
        if (player instanceof Player) {
            message = message.replace("%PLAYER%", player.getName());
            message = message.replace("%DPLAYER%", player.getDisplayName());
            if (config.shouldNotify()) {
                String[] splitMessage = (headerStr() + message).split("%NL%");
                player.sendMessage(splitMessage);
            } else {
                for (String s : message.split("%NL%")) {
                    logInfo(s);
                }
            }
        }
    }

    public void message(CommandSender sender, String message) {
        String colorizedMessage = ChatColor.translateAlternateColorCodes('&', message);
        if (config.shouldNotify()) {
            sender.sendMessage(headerStr() + colorizedMessage);
        } else {
            logInfo(colorizedMessage);
        }
    }

    public void error(Player player, String message) {
        if (player != null && config.shouldNotify()) {
            player.sendMessage(headerStr() + ChatColor.RED + "Error: " + message);
        } else {
            logError(message);
        }
    }

    public boolean isAuthenticated(Player player) {
       if (scavengerAuthMeReloaded != null) {
            return scavengerAuthMeReloaded.isAuthenticated(player);
        } else {
            return false;
        }
    }

}
