package com.tonic.vitatithefarm.api;

import com.tonic.vitatithefarm.data.Condition;
import com.tonic.vitatithefarm.data.Reward;
import com.tonic.vitatithefarm.data.Seed;
import com.tonic.vitatithefarm.data.Where;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the planner needs from one tick, with positions in template coordinates.
 */
@Getter
@Setter
public class FarmState
{
    private int tick;
    private Where where = Where.ELSEWHERE;
    private WorldPoint player;
    private boolean animating;
    private boolean moving;
    private boolean runEnabled;
    private int energy;
    private boolean staminaActive;
    private int potion = -1;
    private int farmingLevel;
    private int herbloreLevel;
    private Seed heldSeed;
    private int seeds;
    private int fruit;
    private int freeSlots;
    private boolean spade;
    private boolean dibber;
    private boolean barehandedPlanting;
    private Water water = new Water(0, 0, 0, false, false, -1);
    private boolean humidify;
    private int score;
    private int points;
    private boolean layoutReady;
    private FarmGrid grid = FarmGrid.fromLayout();
    private String layoutProblem;
    private final List<Plot> plots = new ArrayList<>();
    private final Set<Reward> owned = EnumSet.noneOf(Reward.class);
    private int outfitToWear = -1;

    /**
     * @param key a patch centre in template coordinates
     * @return the patch, or null when it was not read this tick
     */
    public Plot plot(WorldPoint key)
    {
        for (Plot plot : plots)
        {
            if (plot.getKey().equals(key))
            {
                return plot;
            }
        }
        return null;
    }

    /**
     * @return the plants alive in the farm, grown ones included
     */
    public int growing()
    {
        int growing = 0;
        for (Plot plot : plots)
        {
            if (plot.getState().isGrowing())
            {
                growing++;
            }
        }
        return growing;
    }

    /**
     * @param condition the condition to count
     * @return the patches in that condition
     */
    public int count(Condition condition)
    {
        int count = 0;
        for (Plot plot : plots)
        {
            if (plot.getState().getCondition() == condition)
            {
                count++;
            }
        }
        return count;
    }
}
