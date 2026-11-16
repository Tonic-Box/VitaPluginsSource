package com.tonic.vitatempoross.api;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.CombatAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.WorldsAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitatempoross.TemporossConfig;
import com.tonic.vitatempoross.TemporossPlugin;
import com.tonic.vitatempoross.data.Action;
import com.tonic.vitatempoross.data.Where;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Builds the step handler for one action: click the target by its exact option, then wait for the game to show it worked, with action delays, controlled click boxes and retries.
 */
public final class TemporossBuilder extends AbstractHandlerBuilder<TemporossBuilder>
{
    private static final int ATTEMPTS = 3;
    private static final int RESPONSE_TICKS = 6;
    private static final int LOAD_TICKS = 12;
    private static final int IDLE_TICKS = 3;
    private static final int MOVE_TICKS = 6;
    private static final int POOL_FULL = 97;
    private static final int LOOT_SLOTS = 3;
    private static final int TARGET_DRIFT = 2;
    private static final int CLICKED = 0;
    private static final int GONE = 1;
    private static final int MISSING = 2;
    private static final String[] PUMP_OPTIONS = {"Fill-bucket", "Use", "Fill"};
    private static final String[] BANK_OPTIONS = {"Bank", "Use"};
    private static final Random random = new Random();

    private final TemporossPlugin plugin;
    private final CoveTracker tracker;

    private TemporossBuilder(TemporossPlugin plugin)
    {
        this.plugin = plugin;
        this.tracker = plugin.getTracker();
    }

    /**
     * Builds the handler for an action.
     *
     * @param plugin the plugin
     * @param action the action
     * @return the handler
     */
    public static StepHandler forAction(TemporossPlugin plugin, Action action)
    {
        TemporossBuilder builder = new TemporossBuilder(plugin);
        CoveTracker tracker = builder.tracker;
        Thing target = action.getTarget();
        switch (action.getType())
        {
            case WALK_TO_DOCK:
                return builder.walkTo(CoveTracker.DOCK).build();
            case HOP:
                return builder.append(WorldsAPI.hop(action.getCount())).build();
            case TAKE_NET:
                return builder.act(action, new String[]{"Take-net"}, () -> tracker.getState().isNet(), () -> 0, false, RESPONSE_TICKS).build();
            case BIG_SEARCH:
                return builder.act(action, new String[]{"Big-search"}, () -> tracker.getState().getPermits() <= 0 || tracker.getState().getFreeSlots() < LOOT_SLOTS, () -> tracker.getState().getPermits(), true, RESPONSE_TICKS).build();
            case BANK:
                return builder.bank(action.getReason()).build();
            case CLIMB_LADDER:
                return builder.act(action, new String[]{"Climb", "Quick-climb"}, () -> tracker.getState().getWhere() != Where.UNKAH, () -> 0, false, LOAD_TICKS).build();
            case LEAVE:
                return builder.act(action, new String[]{"Leave"}, () -> !inCove(tracker), () -> 0, false, LOAD_TICKS).build();
            case TETHER:
                return builder.act(action, new String[]{"Tether"}, () -> tracker.getState().isTethered(), () -> 0, true, RESPONSE_TICKS).build();
            case UNTETHER:
                return builder.act(action, new String[]{"Untether"}, () -> !tracker.getState().isTethered(), () -> 0, false, RESPONSE_TICKS).build();
            case REPAIR:
                return builder.act(action, new String[]{"Repair"}, () -> !present(tracker.getState().getTethers(), target), () -> 0, true, RESPONSE_TICKS).build();
            case DOUSE:
            {
                int buckets = tracker.getState().getFullBuckets();
                return builder.act(action, new String[]{"Douse"}, () -> !present(tracker.getState().getFires(), target) || tracker.getState().getFullBuckets() < buckets, () -> 0, false, RESPONSE_TICKS).build();
            }
            case MOVE:
                return builder.step(action.getTile(), action.getReason()).build();
            case TAKE_SUPPLY:
                return builder.act(action, new String[]{"Take"}, () -> supplies(tracker.getState(), action.getItemId()) >= action.getCount(), () -> supplies(tracker.getState(), action.getItemId()), true, RESPONSE_TICKS).build();
            case FILL_BUCKETS:
                return builder.act(action, PUMP_OPTIONS, () -> tracker.getState().getEmptyBuckets() == 0, () -> tracker.getState().getFullBuckets(), true, RESPONSE_TICKS).build();
            case FISH:
                return builder.special(action).act(action, new String[]{"Harpoon"}, () -> fished(tracker.getState(), action), () -> tracker.getState().fish(), true, RESPONSE_TICKS).build();
            case COOK:
                return builder.act(action, new String[]{"Cook-at"}, () -> tracker.getState().getRaw() == 0, () -> tracker.getState().getRaw(), true, RESPONSE_TICKS).build();
            case LOAD:
                return builder.act(action, new String[]{"Fill"}, () -> tracker.getState().fish() == 0, () -> tracker.getState().fish(), true, RESPONSE_TICKS).build();
            case HARPOON_POOL:
                return builder.act(action, new String[]{"Harpoon"}, () -> tracker.getState().getPool() == null || tracker.getState().getEnergy() >= POOL_FULL, () -> tracker.getState().getEssence(), true, RESPONSE_TICKS).build();
            default:
                return builder.waitTick().build();
        }
    }

    private static boolean inCove(CoveTracker tracker)
    {
        Where where = tracker.getState().getWhere();
        return where == Where.GAME || where == Where.GAME_OVER;
    }

    private static boolean fished(CoveState state, Action action)
    {
        return state.fish() >= action.getCount() || state.getFreeSlots() <= 0 || !present(state.getSpots(), action.getTarget());
    }

    private static int supplies(CoveState state, int itemId)
    {
        switch (itemId)
        {
            case ItemID.ROPE:
                return state.isRope() ? 1 : 0;
            case ItemID.HAMMER:
                return state.isHammer() ? 1 : 0;
            default:
                return state.buckets();
        }
    }

    private static boolean present(List<Thing> things, Thing target)
    {
        if (target == null)
        {
            return false;
        }
        for (Thing thing : things)
        {
            if (thing.getId() == target.getId() && thing.getTile().distanceTo(target.getTile()) <= TARGET_DRIFT)
            {
                return true;
            }
        }
        return false;
    }

    private TemporossBuilder waitTick()
    {
        int step = currentStep;
        add(context -> waited(context, 1) ? HandlerBuilder.END_EXECUTION : step);
        return this;
    }

    private TemporossBuilder special(Action action)
    {
        int step = currentStep;
        add(context -> {
            if (action.isSpecial() && CombatAPI.getSpecEnergy() >= 100)
            {
                CombatAPI.toggleSpec();
                plugin.setStatus("Using the harpoon's special attack");
            }
            return step + 1;
        });
        return this;
    }

    private TemporossBuilder act(Action action, String[] options, BooleanSupplier done, IntSupplier progress, boolean repeat, int timeout)
    {
        Thing target = action.getTarget();
        String status = action.getReason();
        int step = currentStep;
        add(context -> {
            if (done.getAsBoolean())
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (paused(context))
            {
                return step;
            }
            if (target == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            int clicked = click(target, options);
            if (clicked == GONE)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (clicked == MISSING)
            {
                return failed("No " + options[0] + " option on " + target.getId());
            }
            context.put("progress", progress.getAsInt());
            context.put("worked", false);
            context.put("idle", 0);
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (done.getAsBoolean())
            {
                return HandlerBuilder.END_EXECUTION;
            }
            int now = progress.getAsInt();
            if (now != context.getOrDefault("progress", now))
            {
                context.put("progress", now);
                context.put("worked", true);
                context.put("attempts", 0);
                context.put("idle", 0);
            }
            CoveState state = tracker.getState();
            if (state.isAnimating() || state.isMoving())
            {
                context.put("worked", true);
                context.put("idle", 0);
                return step + 1;
            }
            if (context.getOrDefault("worked", false))
            {
                int idle = context.getOrDefault("idle", 0) + 1;
                context.put("idle", idle);
                if (idle >= IDLE_TICKS)
                {
                    return repeat ? step : HandlerBuilder.END_EXECUTION;
                }
                return step + 1;
            }
            if (waited(context, timeout))
            {
                return retry(context, step, status + " did not work");
            }
            return step + 1;
        });
        return this;
    }

    private int click(Thing target, String[] options)
    {
        if (target.isNpc())
        {
            NpcEx npc = npc(target);
            if (npc == null)
            {
                return GONE;
            }
            int index = option(npc.getActions(), options);
            if (index < 0)
            {
                return MISSING;
            }
            ClickManagerUtil.queueClickBox(npc);
            NpcAPI.interact(npc, index);
            return CLICKED;
        }
        TileObjectEx object = object(target);
        if (object == null)
        {
            return GONE;
        }
        int index = option(object.getActions(), options);
        if (index < 0)
        {
            return MISSING;
        }
        ClickManagerUtil.queueClickBox(object);
        TileObjectAPI.interact(object, index);
        return CLICKED;
    }

    private static int option(String[] actions, String[] options)
    {
        if (actions == null)
        {
            return -1;
        }
        for (String option : options)
        {
            for (int i = 0; i < actions.length; i++)
            {
                if (actions[i] != null && actions[i].equalsIgnoreCase(option))
                {
                    return i;
                }
            }
        }
        return -1;
    }

    private static NpcEx npc(Thing target)
    {
        NpcEx best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (NpcEx npc : NpcAPI.search().withIds(target.getId()).collect())
        {
            int distance = npc.getWorldPoint().distanceTo(target.getTile());
            if (distance < bestDistance)
            {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static TileObjectEx object(Thing target)
    {
        TileObjectEx best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (TileObjectEx object : TileObjectAPI.search().withId(target.getId()).collect())
        {
            int distance = object.getWorldPoint().distanceTo(target.getTile());
            if (distance < bestDistance)
            {
                best = object;
                bestDistance = distance;
            }
        }
        return best;
    }

    private TemporossBuilder step(WorldPoint tile, String status)
    {
        int step = currentStep;
        add(context -> {
            if (tile.equals(tracker.getState().getPlayer()))
            {
                return HandlerBuilder.END_EXECUTION;
            }
            MovementAPI.walkToWorldPoint(tile);
            context.put("waited", 0);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (tile.equals(tracker.getState().getPlayer()) || waited(context, MOVE_TICKS))
            {
                return HandlerBuilder.END_EXECUTION;
            }
            return step + 1;
        });
        return this;
    }

    private TemporossBuilder bank(String status)
    {
        int step = currentStep;
        add(context -> {
            CoveState state = tracker.getState();
            if (state.getExtras().isEmpty())
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (BankAPI.isOpen())
            {
                return step + 2;
            }
            if (paused(context))
            {
                return step;
            }
            Thing chest = state.getBankChest();
            if (chest == null)
            {
                return failed("Cannot find the bank chest");
            }
            if (click(chest, BANK_OPTIONS) != CLICKED)
            {
                return failed("Cannot open the bank chest");
            }
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (BankAPI.isOpen())
            {
                context.put("waited", 0);
                return step + 2;
            }
            if (!MovementAPI.isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, "The bank did not open");
            }
            return step + 1;
        });
        add(context -> {
            List<Integer> extras = tracker.getState().getExtras();
            if (extras.isEmpty() || !BankAPI.isOpen())
            {
                BankAPI.close();
                return HandlerBuilder.END_EXECUTION;
            }
            if (paused(context))
            {
                return step + 2;
            }
            int first = extras.get(0);
            if (first == context.getOrDefault("deposited", -1))
            {
                if (waited(context, RESPONSE_TICKS))
                {
                    return failed("Depositing item " + first + " did not work");
                }
                return step + 2;
            }
            BankAPI.deposit(first, -1);
            context.put("deposited", first);
            acted(context);
            plugin.setStatus("Depositing " + extras.size() + " kinds of item");
            return step + 2;
        });
        return this;
    }

    private int retry(StepContext context, int step, String reason)
    {
        int attempts = context.getOrDefault("attempts", 0) + 1;
        context.put("attempts", attempts);
        context.put("waited", 0);
        if (attempts >= ATTEMPTS)
        {
            return failed(reason);
        }
        plugin.setStatus(reason + ", trying again");
        return step;
    }

    private int failed(String reason)
    {
        plugin.actionFailed(reason);
        return HandlerBuilder.END_EXECUTION;
    }

    private boolean paused(StepContext context)
    {
        int remaining = context.getOrDefault("pause", 0);
        if (remaining <= 0)
        {
            return false;
        }
        context.put("pause", remaining - 1);
        return true;
    }

    private void acted(StepContext context)
    {
        context.put("pause", rollDelay(plugin.getConfig()));
        context.put("waited", 0);
    }

    private static boolean waited(StepContext context, int ticks)
    {
        int waited = context.getOrDefault("waited", 0) + 1;
        context.put("waited", waited);
        return waited >= ticks;
    }

    private static int rollDelay(TemporossConfig config)
    {
        double value = config.tickDelayWeightedDistribution()
                ? -Math.log(Math.abs(random.nextGaussian())) * config.tickDelayDeviation() + config.tickDelayTarget()
                : Math.round(random.nextGaussian() * config.tickDelayDeviation() + config.tickDelayTarget());
        return (int) Math.max(config.tickDelayMin(), Math.min(config.tickDelayMax(), value));
    }
}
