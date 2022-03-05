package net.kevinfitzgerald.aurora.plugins.test;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.VisibilityManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class Mocks {

    private static final Logger SERVER_LOGGER;
    public static final Server SERVER;
    public static final Plugin PLUGIN;

    static {
        SERVER_LOGGER = mock(Logger.class);
        SERVER = mock(Server.class);
        when(SERVER.getLogger()).thenReturn(SERVER_LOGGER);
        PLUGIN = mock(Plugin.class);
        when(PLUGIN.getName()).thenReturn("aurora-holographic-displays-agent");
    }

    public static void prepareEnvironment() {
        if (Bukkit.getServer() == null) {
            Bukkit.setServer(Mocks.SERVER);
        }
    }

    public static Hologram getHologram() {
        Hologram hologram = mock(Hologram.class);
        VisibilityManager vm = mock(VisibilityManager.class);

        // Default answers
        when(hologram.isDeleted()).thenReturn(false);
        when(hologram.getVisibilityManager()).thenReturn(vm);
        return hologram;
    }

    public static Player getPlayer() {
        return mock(Player.class);
    }

}
