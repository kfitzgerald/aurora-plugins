package net.kevinfitzgerald.aurora.plugins;

import digital.murl.aurora.Aurora;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;


public final class HolographicDisplaysAgentPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        HolographicDisplaysAgent.plugin = this;
        HolographicDisplaysAgent.logger = getLogger();

        // Do we care if aurora is enabled? or just roll as-is
        if (!Bukkit.getPluginManager().isPluginEnabled("aurora_core")) {
            getLogger().severe("Looks like Aurora is not enabled :(");
            return;
        }

        // Verify that HD is enabled as well
        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("Looks like Holographic Displays is not enabled :(");
            return;
        }

        // Register the agent with aurora
        try {
            HolographicDisplaysAgent agent = new HolographicDisplaysAgent();
            Aurora.registerAgent(agent.getName(), agent, agent.getActions(), agent.getActionSchemas());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to register Holographic Displays agent with Aurora!", e);
            return;
        }

        // Register server events
        getServer().getPluginManager().registerEvents(this, this);

        // Woot
        getLogger().info("Aurora HD agent enabled !");
    }

    @Override
    public void onDisable() {
        // Remove all holograms
        HolographicDisplaysAgent.removeAllHolograms();
        getLogger().info("Aurora HD agent disabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Show the player existing holograms if they're late to the party
        HolographicDisplaysAgent.handlePlayerJoin(event.getPlayer());
    }
}
