package com.tonic.vitatithefarm.api;

import com.tonic.api.game.reach.SceneCollision;
import com.tonic.util.WorldPointUtil;
import com.tonic.vitatithefarm.data.FarmMap;
import com.tonic.vitatithefarm.data.PlantState;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates between Tithe Farm's template coordinates and the live instance, and reads each patch straight from its scene tile.
 */
public final class FarmCoords
{
    private final Client client;
    private final Map<WorldPoint, WorldPoint> patchTiles = new HashMap<>();
    private int baseX = Integer.MIN_VALUE;
    private int baseY = Integer.MIN_VALUE;

    @Getter
    private String problem;

    /**
     * @param client the client
     */
    public FarmCoords(Client client)
    {
        this.client = client;
    }

    /**
     * @param local a local point
     * @return the tile in template coordinates, or null when the point is null
     */
    public WorldPoint template(LocalPoint local)
    {
        return local == null ? null : WorldPoint.fromLocalInstance(client, local);
    }

    /**
     * Finds the live tile of a template tile and checks that it translates back to the same template tile.
     *
     * @param template a tile in template coordinates
     * @return the tile in the current scene, or null when the scene does not hold it
     */
    public WorldPoint instance(WorldPoint template)
    {
        WorldView view = client.getTopLevelWorldView();
        List<WorldPoint> candidates = WorldPointUtil.toInstance(template);
        for (WorldPoint candidate : candidates)
        {
            LocalPoint local = LocalPoint.fromWorld(view, candidate);
            if (local != null && template.equals(template(local)))
            {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Maps every patch to its live tile when the scene moved; runs each tick in the farm.
     *
     * @return whether every patch was found
     */
    public boolean refresh()
    {
        WorldView view = client.getTopLevelWorldView();
        if (view.getBaseX() == baseX && view.getBaseY() == baseY && problem == null && !patchTiles.isEmpty())
        {
            return true;
        }
        baseX = view.getBaseX();
        baseY = view.getBaseY();
        patchTiles.clear();
        problem = null;
        for (WorldPoint key : FarmMap.PATCHES)
        {
            WorldPoint tile = instance(key);
            if (tile == null)
            {
                problem = "The farm layout was not found: patch " + key + " is not in the scene";
                patchTiles.clear();
                return false;
            }
            patchTiles.put(key, tile);
        }
        for (WorldPoint key : FarmMap.PATCHES)
        {
            GameObject patch = patch(key);
            if (patch != null && !key.equals(template(patch.getLocalLocation())))
            {
                problem = "The farm layout does not match: patch " + key + " is at " + template(patch.getLocalLocation());
                patchTiles.clear();
                return false;
            }
        }
        return true;
    }

    /**
     * @return how far the instance is shifted from the template when every patch is shifted alike, or null when it is not a plain shift
     */
    public WorldPoint shift()
    {
        WorldPoint shift = null;
        for (Map.Entry<WorldPoint, WorldPoint> entry : patchTiles.entrySet())
        {
            WorldPoint key = entry.getKey();
            WorldPoint tile = entry.getValue();
            WorldPoint delta = new WorldPoint(tile.getX() - key.getX(), tile.getY() - key.getY(), tile.getPlane());
            if (shift == null)
            {
                shift = delta;
            }
            else if (!shift.equals(delta))
            {
                return null;
            }
        }
        return shift;
    }

    /**
     * Reads the farm's collision into a grid in template coordinates, or makes one from the layout when the instance is not a plain shift.
     *
     * @return the grid
     */
    public FarmGrid grid()
    {
        WorldPoint shift = shift();
        if (shift == null)
        {
            return FarmGrid.fromLayout();
        }
        SceneCollision collision = SceneCollision.of(client.getTopLevelWorldView(), shift.getPlane());
        if (collision == null)
        {
            return FarmGrid.fromLayout();
        }
        int[][] flags = new int[FarmGrid.WIDTH][FarmGrid.HEIGHT];
        for (int x = 0; x < FarmGrid.WIDTH; x++)
        {
            for (int y = 0; y < FarmGrid.HEIGHT; y++)
            {
                WorldPoint tile = new WorldPoint(FarmGrid.MIN_X + x + shift.getX(), FarmGrid.MIN_Y + y + shift.getY(), shift.getPlane());
                int sceneX = collision.sceneX(tile);
                int sceneY = collision.sceneY(tile);
                flags[x][y] = collision.inside(sceneX, sceneY) ? collision.flags(sceneX, sceneY) : SceneCollision.BLOCK_TILE;
            }
        }
        return new FarmGrid(flags, true);
    }

    /**
     * Forgets the mapping, so the next refresh rebuilds it.
     */
    public void clear()
    {
        patchTiles.clear();
        baseX = Integer.MIN_VALUE;
        baseY = Integer.MIN_VALUE;
        problem = null;
    }

    /**
     * @param key a patch centre in template coordinates
     * @return the patch object on that tile, or null when the scene has none
     */
    public GameObject patch(WorldPoint key)
    {
        WorldPoint tile = patchTiles.get(key);
        if (tile == null)
        {
            return null;
        }
        WorldView view = client.getTopLevelWorldView();
        LocalPoint local = LocalPoint.fromWorld(view, tile);
        if (local == null)
        {
            return null;
        }
        Scene scene = view.getScene();
        Tile sceneTile = scene.getTiles()[tile.getPlane()][local.getSceneX()][local.getSceneY()];
        if (sceneTile == null)
        {
            return null;
        }
        for (GameObject object : sceneTile.getGameObjects())
        {
            if (object != null && PlantState.isPatch(object.getId()))
            {
                return object;
            }
        }
        return null;
    }
}
