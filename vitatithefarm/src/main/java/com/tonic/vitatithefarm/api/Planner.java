package com.tonic.vitatithefarm.api;

import com.tonic.vitatithefarm.data.Action;
import com.tonic.vitatithefarm.data.FarmMap;
import com.tonic.vitatithefarm.data.Reward;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * Plays runs the way players do: prepare, then one lap over the route to plant and water each patch, two laps to water each plant again as it dries, and one lap to harvest, always in route order.
 */
public final class Planner
{
    private static final int SACK_SIZE = 100;
    private static final int RUN_ON_ENERGY = 10;
    private static final int PLANTING_TICKS = 140;

    /**
     * The step of a run.
     */
    public enum Phase
    {
        PREPARE("Preparing"),
        PLANT("Planting"),
        WATER_2("Watering, stage 2"),
        WATER_3("Watering, stage 3"),
        HARVEST("Harvesting"),
        FINISH("Finishing plants found growing");

        private final String name;

        Phase(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    @Getter
    private Phase phase = Phase.PREPARE;

    @Getter
    private int index;

    private List<WorldPoint> run = Collections.emptyList();
    private int firstPlanted = -1;

    @Getter
    private String capReason;

    /**
     * Starts again from preparing a run.
     */
    public void reset()
    {
        phase = Phase.PREPARE;
        index = 0;
        run = Collections.emptyList();
        firstPlanted = -1;
        capReason = null;
    }

    /**
     * @return the patches the current run uses
     */
    public int getRunSize()
    {
        return run.size();
    }

    /**
     * Chooses the next action.
     *
     * @param state the tick's state
     * @param settings the settings
     * @return the action
     */
    public Action next(FarmState state, Settings settings)
    {
        switch (state.getWhere())
        {
            case FARM:
                return farm(state, settings);
            case LOBBY:
                reset();
                return settings.isDraining() ? stop(settings) : lobby(state, settings);
            default:
                reset();
                return settings.isDraining() ? stop(settings) : Action.of(Action.Type.WALK_TO_FARM, Action.TRAVEL, "Walking to Tithe Farm");
        }
    }

    private static Action stop(Settings settings)
    {
        return Action.of(Action.Type.STOP, Action.STOP, "Stopped: " + settings.getStopReason());
    }

    private static Action lobby(FarmState state, Settings settings)
    {
        Reward reward = nextReward(state, settings);
        if (reward != null)
        {
            return Action.buy(reward);
        }
        Reward wear = Reward.of(state.getOutfitToWear());
        if (wear != null && settings.getRewards().contains(wear))
        {
            return Action.withItem(Action.Type.WEAR, Action.TRAVEL, state.getOutfitToWear(), "Wearing the farmer's outfit");
        }
        if (state.getHeldSeed() != null && state.getHeldSeed() != settings.getSeed())
        {
            return Action.withItem(Action.Type.DROP_SEEDS, Action.TRAVEL, state.getHeldSeed().getSeedId(), "Dropping " + state.getHeldSeed() + " seeds, only one kind can be held");
        }
        if (state.getSeeds() == 0)
        {
            return Action.of(Action.Type.TAKE_SEEDS, Action.TRAVEL, "Taking " + settings.getSeed() + " seeds from the table");
        }
        if (state.isHumidify() && state.getWater().isEmptyVessel())
        {
            return Action.of(Action.Type.HUMIDIFY, Action.TRAVEL, "Casting Humidify to fill the cans");
        }
        return Action.of(Action.Type.ENTER, Action.TRAVEL, "Entering the farm");
    }

    private static Reward nextReward(FarmState state, Settings settings)
    {
        for (Reward reward : Reward.values())
        {
            if (!settings.getRewards().contains(reward) || state.getOwned().contains(reward))
            {
                continue;
            }
            if (reward == Reward.HERB_SACK && state.getHerbloreLevel() < Reward.HERB_SACK_HERBLORE)
            {
                continue;
            }
            if (state.getPoints() - reward.getCost() >= settings.getPointReserve())
            {
                return reward;
            }
        }
        return null;
    }

    private Action farm(FarmState state, Settings settings)
    {
        if (!state.isLayoutReady())
        {
            String problem = state.getLayoutProblem();
            return problem == null ? Action.waiting("Reading the farm") : Action.of(Action.Type.STOP, Action.STOP, "Stopped: " + problem);
        }
        if (state.getEnergy() < settings.getDrinkBelow() && state.getPotion() != -1)
        {
            return Action.withItem(Action.Type.DRINK, Action.UPKEEP, state.getPotion(), "Drinking for run energy");
        }
        if (!state.isRunEnabled() && state.getEnergy() >= RUN_ON_ENERGY)
        {
            return Action.of(Action.Type.RUN, Action.UPKEEP, "Switching run on");
        }
        if (phase == Phase.PREPARE && state.growing() > 0)
        {
            phase = Phase.FINISH;
        }
        switch (phase)
        {
            case PLANT:
                return plantLap(state);
            case WATER_2:
                return waterLap(state, 2, Phase.WATER_3);
            case WATER_3:
                return waterLap(state, 3, Phase.HARVEST);
            case HARVEST:
                return harvestLap(state);
            case FINISH:
                return finish(state);
            default:
                return prepare(state, settings);
        }
    }

    private Action prepare(FarmState state, Settings settings)
    {
        if (state.getFruit() > 0)
        {
            return Action.of(Action.Type.DEPOSIT, Action.WORK, "Depositing " + state.getFruit() + " fruit in the sack");
        }
        for (Plot plot : state.getPlots())
        {
            if (plot.isDead())
            {
                return Action.onPlot(Action.Type.CLEAR, Action.WORK, plot.getKey(), "Clearing a dead plant");
            }
        }
        int plants = runSize(state, settings);
        if (plants == 0)
        {
            String reason = settings.isDraining() ? settings.getStopReason() + ", with the sack full" : "the cans hold no water";
            return Action.of(Action.Type.STOP, Action.STOP, "Stopped: " + reason);
        }
        Water water = state.getWater();
        if (water.getCharges() < plants * Water.PER_PLANT)
        {
            if (state.isHumidify() && water.isEmptyVessel())
            {
                return Action.of(Action.Type.HUMIDIFY, Action.UPKEEP, "Casting Humidify to fill the cans");
            }
            if (water.getRefillItem() != -1)
            {
                return Action.withItem(Action.Type.REFILL, Action.UPKEEP, water.getRefillItem(), "Filling the cans at the water barrel");
            }
        }
        if (state.getSeeds() < plants || state.getHeldSeed() != settings.getSeed())
        {
            String loss = state.getScore() % SACK_SIZE == 0 ? "" : ", the part-filled sack is lost";
            return Action.of(Action.Type.LEAVE, Action.TRAVEL, "Leaving to take more seeds" + loss);
        }
        run = RouteGenerator.generate(plants);
        index = 0;
        firstPlanted = -1;
        phase = Phase.PLANT;
        return plantLap(state);
    }

    private int runSize(FarmState state, Settings settings)
    {
        int plants = Math.max(RouteGenerator.MIN_PLANTS, Math.min(RouteGenerator.MAX_PLANTS, settings.getPlantsPerRun()));
        capReason = null;
        int byWater = state.getWater().getCapacity() / Water.PER_PLANT;
        if (byWater < plants)
        {
            plants = byWater;
            capReason = "the cans hold water for " + byWater + " plants";
        }
        if (settings.isDraining())
        {
            int room = (SACK_SIZE - state.getScore() % SACK_SIZE) % SACK_SIZE;
            if (room < plants)
            {
                plants = room;
                capReason = "finishing the sack before stopping";
            }
        }
        return plants;
    }

    private Action plantLap(FarmState state)
    {
        while (index < run.size())
        {
            Plot plot = state.plot(run.get(index));
            if (plot == null)
            {
                return Action.waiting("Reading patch " + (index + 1));
            }
            if (plot.isEmpty())
            {
                if (firstPlanted >= 0 && state.getTick() - firstPlanted > PLANTING_TICKS)
                {
                    capReason = "planting took over " + PLANTING_TICKS + " ticks, so this run has " + index + " plants";
                    run = run.subList(0, index);
                    break;
                }
                return Action.onPlot(Action.Type.PLANT, Action.WORK, plot.getKey(), "Planting " + position());
            }
            if (plot.isDry() && plot.getState().getStage() == 1)
            {
                return Action.onPlot(Action.Type.WATER, Action.WORK, plot.getKey(), "Watering " + position());
            }
            if (plot.isDead())
            {
                return Action.onPlot(Action.Type.CLEAR, Action.WORK, plot.getKey(), "Clearing " + position());
            }
            if (firstPlanted < 0)
            {
                firstPlanted = state.getTick();
            }
            index++;
        }
        return nextLap(Phase.WATER_2);
    }

    private Action waterLap(FarmState state, int stage, Phase after)
    {
        while (index < run.size())
        {
            Plot plot = state.plot(run.get(index));
            if (plot == null)
            {
                return Action.waiting("Reading patch " + (index + 1));
            }
            int plantStage = plot.getState().getStage();
            if (plot.isDry() && plantStage <= stage)
            {
                return Action.onPlot(Action.Type.WATER, Action.WORK, plot.getKey(), "Watering " + position() + ", stage " + stage);
            }
            if (plot.getState().isGrowing() && !plot.isGrown() && plantStage < stage)
            {
                return waitAt(state, plot, "Waiting for " + position() + " to dry");
            }
            index++;
        }
        return nextLap(after);
    }

    private Action harvestLap(FarmState state)
    {
        while (index < run.size())
        {
            Plot plot = state.plot(run.get(index));
            if (plot == null)
            {
                return Action.waiting("Reading patch " + (index + 1));
            }
            if (plot.isGrown())
            {
                if (state.getFreeSlots() == 0)
                {
                    return Action.of(Action.Type.DEPOSIT, Action.WORK, "Depositing " + state.getFruit() + " fruit to make room");
                }
                return Action.onPlot(Action.Type.HARVEST, Action.WORK, plot.getKey(), "Harvesting " + position());
            }
            if (plot.isDead())
            {
                return Action.onPlot(Action.Type.CLEAR, Action.WORK, plot.getKey(), "Clearing " + position() + ", it died");
            }
            if (plot.isDry())
            {
                return Action.onPlot(Action.Type.WATER, Action.WORK, plot.getKey(), "Watering " + position());
            }
            if (plot.getState().isGrowing())
            {
                return waitAt(state, plot, "Waiting for " + position() + " to grow");
            }
            index++;
        }
        return nextLap(Phase.PREPARE);
    }

    private Action finish(FarmState state)
    {
        Plot best = null;
        int bestSteps = Integer.MAX_VALUE;
        for (Plot plot : state.getPlots())
        {
            boolean work = plot.isDry() || plot.isDead() || plot.isGrown() && state.getFreeSlots() > 0;
            if (!work)
            {
                continue;
            }
            int steps = state.getGrid().reach(state.getPlayer(), FarmMap.approach(plot.getKey())).getSteps();
            if (steps < bestSteps)
            {
                best = plot;
                bestSteps = steps;
            }
        }
        if (best != null)
        {
            if (best.isDry())
            {
                return Action.onPlot(Action.Type.WATER, Action.WORK, best.getKey(), "Watering a plant found growing");
            }
            if (best.isDead())
            {
                return Action.onPlot(Action.Type.CLEAR, Action.WORK, best.getKey(), "Clearing a dead plant");
            }
            return Action.onPlot(Action.Type.HARVEST, Action.WORK, best.getKey(), "Harvesting a plant found growing");
        }
        if (state.growing() == 0)
        {
            return nextLap(Phase.PREPARE);
        }
        if (state.getFruit() > 0 && state.getFreeSlots() == 0)
        {
            return Action.of(Action.Type.DEPOSIT, Action.WORK, "Depositing " + state.getFruit() + " fruit to make room");
        }
        return Action.waiting("Waiting for the plants found growing");
    }

    private Action nextLap(Phase next)
    {
        phase = next;
        index = 0;
        return Action.waiting(next.toString());
    }

    private static Action waitAt(FarmState state, Plot plot, String reason)
    {
        if (FarmMap.distanceToPatch(state.getPlayer(), plot.getKey()) > 0)
        {
            return Action.step(state.getGrid().reach(state.getPlayer(), FarmMap.approach(plot.getKey())).getTile(), reason);
        }
        return Action.onPlot(Action.Type.WAIT, Action.IDLE, plot.getKey(), reason);
    }

    private String position()
    {
        return "patch " + (index + 1) + " of " + run.size();
    }
}
