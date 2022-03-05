package net.kevinfitzgerald.aurora.plugins.test;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.VisibilityManager;
import net.kevinfitzgerald.aurora.plugins.HologramState;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class HologramStateTest {

    @Test
    void visibleToAll() {
        Player player = Mocks.getPlayer();
        Hologram hologram = mock(Hologram.class);
        HologramState state = new HologramState("h1", hologram, "test", true, null);

        // nothing should happen cuz visibleToAll is false
        state.showToPlayerIfAble(player);
        verify(hologram, never()).getVisibilityManager(); // shouldn't have touched it
    }

    @Test
    void visibleToPlayerWithPermission() {
        Player player = Mocks.getPlayer();

        // Works for players with permissions
        Hologram hologram = mock(Hologram.class);
        VisibilityManager vm = mock(VisibilityManager.class);
        when(hologram.isDeleted()).thenReturn(false);
        when(hologram.getVisibilityManager()).thenReturn(vm);
        when(player.hasPermission("perm")).thenReturn(true);

        HologramState state = new HologramState("h1", hologram, "test", false, "perm");
        state.showToPlayerIfAble(player);

        // They should have been shown
        verify(vm).showTo(player);
    }

    @Test
    void notVisibleToPlayerWithoutPermission() {
        Player player = Mocks.getPlayer();

        // Works for players with permissions
        Hologram hologram = mock(Hologram.class);
        VisibilityManager vm = mock(VisibilityManager.class);
        when(hologram.isDeleted()).thenReturn(false);
        when(hologram.getVisibilityManager()).thenReturn(vm);
        when(player.hasPermission("perm")).thenReturn(false);

        HologramState state = new HologramState("h1", hologram, "test", false, "perm");
        state.showToPlayerIfAble(player);

        // They should have been shown
        verify(vm, never()).showTo(player);
    }



}
