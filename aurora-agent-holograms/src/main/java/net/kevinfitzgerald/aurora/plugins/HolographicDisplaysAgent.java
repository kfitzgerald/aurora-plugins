package net.kevinfitzgerald.aurora.plugins;

import digital.murl.aurora.Result;
import digital.murl.aurora.agents.AgentAction;
import digital.murl.aurora.agents.Agent;
import digital.murl.aurora.points.Point;
import digital.murl.aurora.points.PointManager;
import digital.murl.aurora.points.Points;
import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import digital.murl.aurora.utils.shortid.ShortId;
import digital.murl.aurora.utils.AuroraMapUtils;
import net.kevinfitzgerald.aurora.plugins.payloads.HideHologramRequest;
import net.kevinfitzgerald.aurora.plugins.payloads.ShowHologramRequest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Aurora agent for displaying Holograms via Holographic Displays
 */
public final class HolographicDisplaysAgent implements Agent {

    // Agent name
    protected static final String AGENT_NAME = "holographic-displays";

    // Groups
    protected static final String ADMIN_POINT_GROUP = "admin-points";
    protected static final String CLIENT_POINT_GROUP = "client-points";

    // Point visibility permission
    protected static final String ADMIN_PERMISSION = "aurora.admin"; // fixme should this be core?

    // Actions
    protected static final String POINTS_SHOW_ACTION = "points.show";
    protected static final String POINTS_HIDE_ACTION = "points.hide";
    protected static final String START_ACTION = "start";
    protected static final String STOP_ACTION = "stop";
    protected static final String UPDATE_ACTION = "update";
    protected static final String ALL_STOP_ACTION = "all.stop";
    protected static final String CLEANUP_ACTION = "cleanup";

    // Hax
    public static Logger logger;
    public static HolographicDisplaysAgentPlugin plugin;

    // There can be only one, highlander
    private static final ConcurrentHashMap<String, HologramState> holograms = new ConcurrentHashMap<>();

    //region Action Handling

    /**
     * Gets the name of the agent
     * @return Agent name
     */
    public String getName() {
        return AGENT_NAME;
    }

    /**
     * Gets the actions to register
     * @return Agent actions
     */
    public HashMap<String, AgentAction> getActions() {
        HashMap<String, AgentAction> actions = new HashMap<>();
        actions.put(POINTS_SHOW_ACTION, (agent, params) -> showPointHologramsHandler());
        actions.put(POINTS_HIDE_ACTION, (agent, params) -> hidePointHologramsHandler());
        actions.put(START_ACTION, (agent, params) -> showHologramHandler(params));
        actions.put(STOP_ACTION, (agent, params) -> hideHologramHandler(params));
        actions.put(UPDATE_ACTION, (agent, params) -> updateHologramHandler(params));
        actions.put(ALL_STOP_ACTION, (agent, params) -> stopAllHologramsHandler());
        actions.put(CLEANUP_ACTION, (agent, params) -> clearAllHologramsHandler());
        return actions;
    }

    /**
     * Gets the action schemas
     * @return Agent action schemas
     */
    public HashMap<String, String> getActionSchemas() {
        HashMap<String, String> schemas = new HashMap<>();
        schemas.put(POINTS_SHOW_ACTION, getSchemaPayload(POINTS_SHOW_ACTION));
        schemas.put(POINTS_HIDE_ACTION, getSchemaPayload(POINTS_HIDE_ACTION));
        schemas.put(START_ACTION, getSchemaPayload(START_ACTION));
        schemas.put(STOP_ACTION, getSchemaPayload(STOP_ACTION));
        schemas.put(UPDATE_ACTION, getSchemaPayload(UPDATE_ACTION));
        schemas.put(ALL_STOP_ACTION, getSchemaPayload(ALL_STOP_ACTION));
        schemas.put(CLEANUP_ACTION, getSchemaPayload(CLEANUP_ACTION));
        return schemas;
    }

    /**
     * Fetches the schema JSON blob from the jar resources
     * @param action Action name
     * @return JSON payload
     */
    private static String getSchemaPayload(String action) {
        String filename = "/schemas/"+action+".json";
        Scanner scanner = new Scanner(HolographicDisplaysAgent.class.getResourceAsStream(filename), "UTF-8").useDelimiter("\\A");
        if (scanner.hasNext()) {
            return scanner.next();
        } else {
            logger.warning("Failed to load schema for action, filename: " + filename);
            return null;
        }
    }

    /**
     * Adds a hologram at every registered
     * @return Action result
     */
    private static Result showPointHologramsHandler() {
        // Do not dup point holograms...
        hidePointHologramsHandler();

        logger.info("Showing admin point holograms...");
        for (Point point : Points.getPoints()) {
            addHologram(ShortId.generate(), ADMIN_POINT_GROUP, point, new Object[]{ ""+point.id }, false, ADMIN_PERMISSION);
        }

        return new Result(Result.Outcome.SUCCESS, null);
    }

    /**
     * Removes all point holograms
     * @return Action result
     */
    private static Result hidePointHologramsHandler() {
        logger.info("Deleting admin point holograms...");
        Iterator<Map.Entry<String, HologramState>> i = holograms.entrySet().iterator();
        while (i.hasNext()) {
            HologramState state = i.next().getValue();
            if (state.getEffectGroup().equals(ADMIN_POINT_GROUP)) {
                state.destroy();
                i.remove();
            }
        }

        return new Result(Result.Outcome.SUCCESS, null);
    }

    /**
     * Adds a new hologram or shows an existing hologram in the world
     * @param params Request args
     * @return Response
     */
    private static Result showHologramHandler(Map<String, Object> params) {
        /*
            CREATE: {
                id: "string", (optional)
                permission: "string", (optional)
                point: 42, (number)
                lines: [ (required)
                    "text", (string)
                    ItemStack (map<string,object>)
                ]
            }

            SHOW: {
                id: "string" (required)
            }
        */

        // /agents holographic-displays start {"id": "h1", "permission": null, "point": 0, "lines": [ "§6YOOOOOO", "§lThis§r is {rainbow}COOL" ]}
        // /agents holographic-displays stop {"id":"h1"}

        ShowHologramRequest request = AuroraMapUtils.mapToObject(params, ShowHologramRequest.class);
        // logger.info("Parsed request: " + request.toString());

        // Use the given id or generate one if not given
        if (request.id == null || request.id.trim().length() == 0) request.id = ShortId.generate();

        // Check if hologram already and show it (as it could be hidden)
        if (holograms.containsKey(request.id)) {
            holograms.get(request.id).show();
            return new Result(Result.Outcome.SUCCESS, null);
        }

        // Create the hologram
        return createOrUpdateState(request, true);
    }

    /**
     * Adds a new hologram or shows an existing hologram in the world
     * @param params Request args
     * @return Response
     */
    private static Result updateHologramHandler(Map<String, Object> params) {
        /*
            UPDATE: {
                id: "string", (optional)
                permission: "string", (optional)
                point: 42, (number, required)
                lines: [ (required)
                    "text", (string)
                    ItemStack (map<string,object>)
                ]
            }
        */

        ShowHologramRequest request = AuroraMapUtils.mapToObject(params, ShowHologramRequest.class);

        // Make sure the hologram exists
        if (request.id == null || !holograms.containsKey(request.id)) {
            return new Result(Result.Outcome.NOT_FOUND, "Hologram not found");
        }

        // Update the existing state
        return createOrUpdateState(request, false);
    }

    /**
     * Internal helper that is common to both show and update
     * @param request Request params
     * @param doCreate Whether to create the hologram or update the existing one
     * @return Response
     */
    private static Result createOrUpdateState(ShowHologramRequest request, boolean doCreate) {

        // Get point
        if (request.point == null) {
            return new Result(Result.Outcome.INVALID_ARGS, "point is a required parameter");
        }

        // Get point from Aurora
        Point point = PointManager.getPoint((request.point));
        if (point == null) {
            return new Result(Result.Outcome.INVALID_ARGS, "point "+request.point+" is not registered");
        }

        // Make sure we received at least one line
        if (request.lines.length == 0) {
            return new Result(Result.Outcome.INVALID_ARGS, "lines is a required parameter");
        }

        // Convert raw lines into an array of Strings and ItemStacks
        Object[] lines = new Object[request.lines.length];
        for (int i = 0; i < request.lines.length; i++) {
            if (request.lines[i] instanceof String) {
                lines[i] = request.lines[i];
            } else if (request.lines[i] instanceof Map) {
                try {
                    lines[i] = ItemStack.deserialize((Map<String,Object>)request.lines[i]);
                } catch (Exception e) {
                    logger.log(Level.SEVERE,"Failed to parse hologram line into ItemStack", e);
                    return new Result(Result.Outcome.INVALID_ARGS, "lines["+i+"] could not be deserialized as an ItemStack");
                }
            } else {
                return new Result(Result.Outcome.INVALID_ARGS, "lines["+i+"] must be a String or ItemStack Map");
            }
        }

        // Get the permission required to view, if necessary
        boolean visibleToAll = request.permission == null || request.permission.trim().length() == 0;

        // Create or update the hologram state tracker
        HologramState state;
        if (doCreate) {
            state = addHologram(request.id, CLIENT_POINT_GROUP, point, lines, visibleToAll, request.permission);
            if (state == null) {
                return new Result(Result.Outcome.INVALID_ARGS, "Failed to create hologram");
            }
        } else {
            state = updateHologram(request.id, CLIENT_POINT_GROUP, point, lines, visibleToAll, request.permission);
            if (state == null) {
                return new Result(Result.Outcome.INVALID_ARGS, "Failed to update hologram");
            }
        }

        // FIXME: need to return a payload to the client
        return new Result(Result.Outcome.SUCCESS, state.getId()); // for now use the message slot for sticking the id in there
    }

    /**
     * Hides a particular hologram
     * @param params Request
     * @return Response
     */
    private static Result hideHologramHandler(Map<String, Object> params) {
        /*
            HIDE: {
                id: "string" (required)
            }
        */

        HideHologramRequest request = AuroraMapUtils.mapToObject(params, HideHologramRequest.class);

        // Verify the hologram exists
        if (request.id == null || !holograms.containsKey(request.id)) {
            return new Result(Result.Outcome.NOT_FOUND, "Hologram not found");
        }

        // Hide it
        HologramState state = holograms.get(request.id);
        state.hide();

        return new Result(Result.Outcome.SUCCESS, null);
    }

    /**
     * Hides all holograms (can be shown again)
     * @return Response
     */
    private static Result stopAllHologramsHandler() {
        // hide, do not delete holograms
        for (Map.Entry<String, HologramState> stringHologramStateEntry : holograms.entrySet()) {
            HologramState state = stringHologramStateEntry.getValue();
            state.hide();
        }

        return new Result(Result.Outcome.SUCCESS, null);
    }

    /**
     * Deletes all holograms (gone for good)
     * @return Response
     */
    private static Result clearAllHologramsHandler() {
        removeAllHolograms();

        return new Result(Result.Outcome.SUCCESS, null);
    }

    //endregion

    /**
     * Show existing holograms to the player if they're late to the party
     * @param player Player who joined
     */
    public static void handlePlayerJoin(Player player) {
        // FIXME: if a permission-hologram is hidden, it will become visible to the player when they join
        // since the HologramState doesn't keep track of whether it _should_ be visible or not
        // In reality, the probability of this being a problem is slim and not worth the effort to fix
        holograms.values().forEach(hologramState -> hologramState.showToPlayerIfAble(player));
    }

    /**
     * Removes all existing holograms from existence
     */
    public static void removeAllHolograms() {
        Iterator<Map.Entry<String, HologramState>> i = holograms.entrySet().iterator();
        while (i.hasNext()) {
            HologramState state = i.next().getValue();
            state.destroy();
            i.remove();
        }
    }

    /**
     * Adds a new hologram into existence
     * @param id The id of the hologram
     * @param effectGroup The effect group/tag/id this hologram belongs to
     * @param point The location where the hologram should appear
     * @param lines The content of the hologram
     * @param visibleToAll Whether the hologram should show to everyone by default
     * @param visibleToPermission The permission players must have in order to see the hologram when visibleToAll is false
     * @return The wrapped hologram state or null if the lines are poop
     */
    public static HologramState addHologram(String id, String effectGroup, Point point, Object[] lines, Boolean visibleToAll, String visibleToPermission) {
        // If the hologram was already added, ignore the request to create a new one and return the old one
        if (holograms.containsKey(id)) return holograms.get(id);

        // Create the hologram and set the content
        Hologram hologram = HologramsAPI.createHologram(plugin, point.location().add(new Vector(0, 0.25, 0)));
        hologram.setAllowPlaceholders(true);
        if (!setHologramLines(hologram, lines)) {
            return null;
        }

        // Create the state wrapper so the hologram can be refreshed as server/client events happen
        HologramState state = new HologramState(id, hologram, effectGroup, visibleToAll, visibleToPermission);
        holograms.put(id, state);

        // Show it to players with the state permission
        state.show();

        return state;
    }

    /**
     * Updates an existing hologram
     * @param id The id of the hologram to update
     * @param effectGroup The effect group/tag/id this hologram belongs to
     * @param point The location where the hologram should appear
     * @param lines The content of the hologram
     * @param visibleToAll Whether the hologram should show to everyone by default
     * @param visibleToPermission The permission players must have in order to see the hologram when visibleToAll is false
     * @return Updated state wrapper or null if not found
     */
    public static HologramState updateHologram(String id, String effectGroup, Point point, Object[] lines, Boolean visibleToAll, String visibleToPermission) {
        HologramState state = holograms.get(id);
        if (state == null) return null;

        Hologram hologram = state.getHologram();
        state.setEffectGroup(effectGroup);
        hologram.teleport(point.location().add(new Vector(0, 0.25, 0)));
        setHologramLines(hologram, lines);
        state.setVisibility(visibleToAll, visibleToPermission);

        return state;
    }

    /**
     * Sets the content of a hologram
     * @param hologram Hologram to update
     * @param lines Array of String or ItemStack objects, one per line
     * @return True if successful, False when a crap object was given
     */
    protected static boolean setHologramLines(Hologram hologram, Object[] lines) {
        hologram.clearLines();
        for (Object line : lines) {
            if (line instanceof String) {
                hologram.appendTextLine((String)line);
            } else if (line instanceof ItemStack) {
                hologram.appendItemLine((ItemStack)line);
            } else {
                logger.warning("Rejecting hologram (line not String or ItemStack), got: " + line.getClass());
                hologram.delete();
                return false;
            }
        }
        return true;
    }

}
