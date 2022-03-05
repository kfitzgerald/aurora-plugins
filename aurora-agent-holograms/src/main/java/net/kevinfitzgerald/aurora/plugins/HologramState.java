package net.kevinfitzgerald.aurora.plugins;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


/**
 * Wrapped hologram state
 */
public class HologramState {

    private final String id;
    private final Hologram hologram;
    private String effectGroup;
    private boolean visibleToAll;
    private String visibleToPermission;

    /**
     * Creates a new wrapped hologram
     * @param hologram Hologram instance
     * @param id Hologram instance id
     * @param effectGroup The effect group/tag/id the hologram belongs to
     * @param visibleToAll Whether the hologram should show to everyone by default
     * @param visibleToPermission The permission players must have in order to see the hologram when visibleToAll is false
     */
    public HologramState(String id, Hologram hologram, String effectGroup, boolean visibleToAll, String visibleToPermission) {
        this.id = id;
        this.effectGroup = effectGroup;
        this.hologram = hologram;
        this.visibleToAll = visibleToAll;
        this.visibleToPermission = visibleToPermission;
    }

    /**
     * Show the hologram to the given player if applicable
     * For example, when a player joins the server and has an applicable permission
     * @param player Player in question
     */
    public void showToPlayerIfAble(Player player) {
        if (!isVisibleToAll() && !getHologram().isDeleted()) {
            if (player.hasPermission(getVisibleToPermission())) {
                getHologram().getVisibilityManager().showTo(player);
            }
        }
    }

    /**
     * Show the hologram to all players who have permission
     */
    public void show() {
        if (getHologram().isDeleted()) return;

        // Set whether all players can see this thing
        getHologram().getVisibilityManager().setVisibleByDefault(isVisibleToAll());

        // If not, then show to the online players with permission to see it
        if (!isVisibleToAll()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                showToPlayerIfAble(player);
            }
        }
    }

    /**
     * Hides the hologram from everyone regardless of permission set
     */
    public void hide() {
        if (getHologram().isDeleted()) return;

        getHologram().getVisibilityManager().setVisibleByDefault(false);
        getHologram().getVisibilityManager().resetVisibilityAll();
    }

    /**
     * Removes the hologram from the world FOREVER
     */
    public void destroy() {
        getHologram().delete();
    }

    /**
     * Gets the hologram instance
     */
    public Hologram getHologram() {
        return hologram;
    }

    /**
     * Gets the state identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Used to track HDs by an effect id/tag/group etc
     */
    public String getEffectGroup() {
        return effectGroup;
    }

    /**
     * Sets the hologram group tag
     * @param effectGroup Group tag
     */
    public void setEffectGroup(String effectGroup) {
        this.effectGroup = effectGroup;
    }

    /**
     * Whether the hologram should be visible to everyone by default
     */
    public boolean isVisibleToAll() {
        return visibleToAll;
    }

    /**
     * Whether the hologram should only be visible to players with the given permission
     */
    public String getVisibleToPermission() {
        return visibleToPermission;
    }

    /**
     * Sets the hologram visibility
     * @param visibleToAll Whether the hologram should be visible to everyone by default
     * @param visibleToPermission When not visible by default, show only to players with the given permission
     */
    public void setVisibility(boolean visibleToAll, String visibleToPermission) {
        this.visibleToAll = visibleToAll;
        this.visibleToPermission = visibleToPermission;

        // Update visibility
        hide();
        show();
    }
}
