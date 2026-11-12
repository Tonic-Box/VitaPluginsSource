package com.tonic.vitamta.rooms;

import com.tonic.vitamta.api.ArenaState;
import com.tonic.vitamta.data.Action;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;

/**
 * Plays the Telekinetic Theatre: stand on the side of the maze the next pull goes towards, cast once the guardian has stopped, and start a new maze when it is solved.
 */
public final class TelekineticPlanner
{
    /**
     * Chooses the next action in the room.
     *
     * @param state the game state
     * @return the action
     */
    public Action next(ArenaState state)
    {
        if (!state.isGuardianPresent())
        {
            return Action.waitFor("Waiting for the maze guardian");
        }
        if (state.isGuardianComplete())
        {
            return Action.newMaze();
        }
        TelekineticSolver maze = state.getMaze();
        if (maze == null)
        {
            return Action.waitFor("Waiting for the maze to load");
        }
        if (state.getMoves().isEmpty())
        {
            return Action.waitFor("Waiting for the guardian to reach the finish");
        }
        Direction next = state.getMoves().get(0);
        Direction after = state.getMoves().size() > 1 ? state.getMoves().get(1) : null;
        WorldPoint player = state.getPlayer();
        if (maze.side(player) == next)
        {
            if (state.isGuardianMoving() || state.isTelegrabInFlight() || state.isMoving())
            {
                return Action.waitFor("Waiting for the guardian to stop");
            }
            return Action.telegrab();
        }
        WorldPoint stand = maze.standTile(player, next, after);
        if (stand == null)
        {
            return Action.waitFor("No tile to cast from");
        }
        return Action.move(stand, "Walking to the " + next.name().toLowerCase() + " side of the maze");
    }
}
