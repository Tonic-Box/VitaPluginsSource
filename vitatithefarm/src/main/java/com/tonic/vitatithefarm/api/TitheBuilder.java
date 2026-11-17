package com.tonic.vitatithefarm.api;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.api.widgets.ShopAPI;
import com.tonic.data.magic.spellbooks.Lunar;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitatithefarm.TitheFarmConfig;
import com.tonic.vitatithefarm.TitheFarmPlugin;
import com.tonic.vitatithefarm.data.Action;
import com.tonic.vitatithefarm.data.FarmMap;
import com.tonic.vitatithefarm.data.PlantState;
import com.tonic.vitatithefarm.data.Reward;
import com.tonic.vitatithefarm.data.Where;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarClientID;

import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds the step handler for one action: click the target by its exact option, then wait for the farm to show it worked, with action delays, controlled click boxes and retries.
 */
public final class TitheBuilder extends AbstractHandlerBuilder<TitheBuilder>
{
    private static final int ATTEMPTS = 3;
    private static final int RESPONSE_TICKS = 5;
    private static final int LOAD_TICKS = 12;
    private static final int IDLE_TICKS = 3;
    private static final int MOVE_TICKS = 8;
    private static final int MIN_SEEDS = 2000;
    private static final int MAX_SEEDS = 10000;
    private static final String[] SHOP_OPTIONS = {"Rewards", "Trade", "Shop"};
    private static final String[] ENTER_ANSWERS = {"don't ask", "No"};
    private static final String[] LEAVE_ANSWERS = {"Yes"};
    private static final Random random = new Random();

    private final TitheFarmPlugin plugin;
    private final FarmTracker tracker;

    private TitheBuilder(TitheFarmPlugin plugin)
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
    public static StepHandler forAction(TitheFarmPlugin plugin, Action action)
    {
        TitheBuilder builder = new TitheBuilder(plugin);
        FarmTracker tracker = builder.tracker;
        WorldPoint plot = action.getPlot();
        String reason = action.getReason();
        switch (action.getType())
        {
            case WALK_TO_FARM:
                return builder.walkTo(FarmMap.LOBBY_TILE).build();
            case STEP:
                return builder.step(action.getTile(), reason).build();
            case BUY_REWARD:
                return builder.buy(action.getReward(), reason).build();
            case WEAR:
            {
                int itemId = action.getItemId();
                return builder.useItem(itemId, "Wear", () -> InventoryAPI.count(itemId) == 0, reason).build();
            }
            case DROP_SEEDS:
            {
                int itemId = action.getItemId();
                return builder.useItem(itemId, "Drop", () -> InventoryAPI.count(itemId) == 0, reason).build();
            }
            case DRINK:
            {
                int itemId = action.getItemId();
                int count = InventoryAPI.count(itemId);
                return builder.useItem(itemId, "Drink", () -> InventoryAPI.count(itemId) < count, reason).build();
            }
            case RUN:
                return builder.run(reason).build();
            case TAKE_SEEDS:
                return builder.takeSeeds(reason).build();
            case HUMIDIFY:
                return builder.humidify(reason).build();
            case ENTER:
                return builder.door(ENTER_ANSWERS, () -> tracker.getState().getWhere() == Where.FARM, reason).build();
            case LEAVE:
                return builder.door(LEAVE_ANSWERS, () -> tracker.getState().getWhere() != Where.FARM, reason).build();
            case REFILL:
            {
                int itemId = action.getItemId();
                return builder.act(() -> nearest(FarmMap.BARREL_IDS), null, itemId, () -> tracker.getState().getWater().isFull(), true, reason).build();
            }
            case PLANT:
            {
                int seedId = plugin.settings().getSeed().getSeedId();
                return builder.act(() -> builder.plot(plot), null, seedId, () -> builder.changedFrom(plot, PlantState.EMPTY), false, reason).build();
            }
            case WATER:
            case HARVEST:
            case CLEAR:
            {
                PlantState before = builder.plotState(plot);
                String option = option(action.getType());
                return builder.act(() -> builder.plot(plot), option, -1, () -> builder.changedFrom(plot, before), false, reason).build();
            }
            case DEPOSIT:
                return builder.act(() -> nearest(FarmMap.SACK_IDS), "Deposit", -1, () -> tracker.getState().getFruit() == 0, false, reason).build();
            default:
                return builder.waitTick().build();
        }
    }

    private static String option(Action.Type type)
    {
        switch (type)
        {
            case WATER:
                return "Water";
            case HARVEST:
                return "Harvest";
            default:
                return "Clear";
        }
    }

    private static TileObjectEx nearest(int[] ids)
    {
        return TileObjectAPI.search().withId(ids).sortNearest().first();
    }

    private TileObjectEx plot(WorldPoint key)
    {
        GameObject object = tracker.getCoords().patch(key);
        return object == null ? null : TileObjectEx.of(object);
    }

    private PlantState plotState(WorldPoint key)
    {
        Plot plot = tracker.getState().plot(key);
        return plot == null ? null : plot.getState();
    }

    private boolean changedFrom(WorldPoint key, PlantState before)
    {
        PlantState now = plotState(key);
        return now != null && !now.equals(before);
    }

    private int tick()
    {
        return plugin.getClient().getTickCount();
    }

    private TitheBuilder waitTick()
    {
        int step = currentStep;
        add(context -> waited(context, 1) ? end() : step);
        return this;
    }

    private TitheBuilder act(Supplier<TileObjectEx> target, String option, int itemId, BooleanSupplier done, boolean repeat, String status)
    {
        int step = currentStep;
        add(context -> {
            if (done.getAsBoolean())
            {
                return end();
            }
            if (paused(context))
            {
                return step;
            }
            TileObjectEx object = target.get();
            if (object == null)
            {
                return failed("Cannot find the target for: " + status);
            }
            if (option != null)
            {
                int index = optionIndex(object.getActions(), option);
                if (index < 0)
                {
                    return failed("No " + option + " option on object " + object.getId());
                }
                ClickManagerUtil.queueClickBox(object);
                TileObjectAPI.interact(object, index);
            }
            else
            {
                ItemEx item = InventoryAPI.getItem(itemId);
                if (item == null)
                {
                    return failed("Item " + itemId + " is not in the inventory");
                }
                ClickManagerUtil.queueClickBox(object);
                InventoryAPI.useOn(item, object);
            }
            context.put("progress", tracker.getState().getWater().getCharges());
            context.put("worked", false);
            context.put("idle", 0);
            context.put("clicked", tick());
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (done.getAsBoolean())
            {
                return end();
            }
            FarmState state = tracker.getState();
            if (repeat)
            {
                int now = state.getWater().getCharges();
                if (now != context.getOrDefault("progress", now))
                {
                    context.put("progress", now);
                    context.put("worked", true);
                    context.put("idle", 0);
                }
                else if (context.getOrDefault("worked", false) && !state.isAnimating())
                {
                    int idle = context.getOrDefault("idle", 0) + 1;
                    context.put("idle", idle);
                    if (idle >= IDLE_TICKS)
                    {
                        return end();
                    }
                }
            }
            if (state.isMoving() || state.isAnimating())
            {
                context.put("waited", 0);
                return step + 1;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, status + " did not work" + message(context));
            }
            return step + 1;
        });
        return this;
    }

    private TitheBuilder useItem(int itemId, String option, BooleanSupplier done, String status)
    {
        int step = currentStep;
        add(context -> {
            if (done.getAsBoolean())
            {
                return end();
            }
            if (paused(context))
            {
                return step;
            }
            ItemEx item = InventoryAPI.getItem(itemId);
            if (item == null)
            {
                return end();
            }
            ClickManagerUtil.queueClickBox(item);
            InventoryAPI.interact(item, option);
            context.put("clicked", tick());
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (done.getAsBoolean())
            {
                return end();
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, status + " did not work" + message(context));
            }
            return step + 1;
        });
        return this;
    }

    private TitheBuilder run(String status)
    {
        int step = currentStep;
        add(context -> {
            if (tracker.getState().isRunEnabled())
            {
                return end();
            }
            if (paused(context))
            {
                return step;
            }
            MovementAPI.toggleRun();
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (MovementAPI.isRunEnabled())
            {
                return end();
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, "Run did not switch on");
            }
            return step + 1;
        });
        return this;
    }

    private TitheBuilder humidify(String status)
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            if (!Lunar.HUMIDIFY.canCast())
            {
                return failed("Humidify cannot be cast");
            }
            context.put("progress", tracker.getState().getWater().getCharges());
            context.put("clicked", tick());
            MagicAPI.cast(Lunar.HUMIDIFY);
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            int before = context.getOrDefault("progress", 0);
            if (tracker.getState().getWater().getCharges() > before)
            {
                return end();
            }
            if (waited(context, RESPONSE_TICKS + 2))
            {
                return failed("Humidify did not fill the cans" + message(context));
            }
            return step + 1;
        });
        return this;
    }

    private TitheBuilder takeSeeds(String status)
    {
        int step = currentStep;
        String seed = plugin.settings().getSeed().getName();
        add(context -> {
            if (tracker.getState().getSeeds() > 0)
            {
                return end();
            }
            if (paused(context))
            {
                return step;
            }
            TileObjectEx table = TileObjectAPI.search().withId(ObjectID.TITHE_PLANT_SEED_TABLE).sortNearest().first();
            if (table == null)
            {
                return failed("Cannot find the seed table");
            }
            int index = optionIndex(table.getActions(), "Search");
            if (index < 0)
            {
                return failed("No Search option on the seed table");
            }
            ClickManagerUtil.queueClickBox(table);
            TileObjectAPI.interact(table, index);
            context.put("clicked", tick());
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (DialogueAPI.isViewingOptions())
            {
                if (!DialogueAPI.selectOption(seed))
                {
                    return failed("The seed table offers no " + seed + " option: " + DialogueAPI.getOptions());
                }
                context.put("waited", 0);
                return step + 2;
            }
            if (!tracker.getState().isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, "The seed table did not open" + message(context));
            }
            return step + 1;
        });
        add(context -> {
            if (tracker.getState().getSeeds() > 0)
            {
                return end();
            }
            if (plugin.getClient().getVarcIntValue(VarClientID.MESLAYERMODE) != 0)
            {
                int amount = MIN_SEEDS + random.nextInt(MAX_SEEDS - MIN_SEEDS + 1);
                DialogueAPI.resumeNumericDialogue(amount);
                context.put("waited", 0);
                return step + 3;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, "The seed table did not ask how many" + message(context));
            }
            return step + 2;
        });
        add(context -> {
            if (tracker.getState().getSeeds() > 0)
            {
                return end();
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, "No seeds were taken" + message(context));
            }
            return step + 3;
        });
        return this;
    }

    private TitheBuilder door(String[] answers, BooleanSupplier done, String status)
    {
        int step = currentStep;
        add(context -> {
            if (done.getAsBoolean())
            {
                return end();
            }
            if (paused(context))
            {
                return step;
            }
            TileObjectEx door = TileObjectAPI.search().withId(ObjectID.HOSIDIUS_TITHE_FARM_DOOR).sortNearest().first();
            if (door == null)
            {
                return failed("Cannot find the farm door");
            }
            int index = optionIndex(door.getActions(), "Open");
            if (index < 0)
            {
                return failed("No Open option on the farm door");
            }
            ClickManagerUtil.queueClickBox(door);
            TileObjectAPI.interact(door, index);
            context.put("clicked", tick());
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (done.getAsBoolean())
            {
                return end();
            }
            if (DialogueAPI.isViewingOptions())
            {
                List<String> options = DialogueAPI.getOptions();
                for (String answer : answers)
                {
                    if (DialogueAPI.selectOption(answer))
                    {
                        context.put("waited", 0);
                        return step + 1;
                    }
                }
                return failed("The door asks a question with unknown options: " + options);
            }
            if (DialogueAPI.continueDialogue())
            {
                context.put("waited", 0);
                return step + 1;
            }
            if (!tracker.getState().isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, status + " did not work" + message(context));
            }
            return step + 1;
        });
        return this;
    }

    private TitheBuilder buy(Reward reward, String status)
    {
        int step = currentStep;
        add(context -> {
            if (tracker.getState().getOwned().contains(reward))
            {
                return end();
            }
            if (ShopAPI.isOpen())
            {
                return step + 2;
            }
            if (paused(context))
            {
                return step;
            }
            NpcEx farmer = NpcAPI.search().withIds(NpcID.HOSIDIUS_TITHE_FARMER).sortNearest().first();
            if (farmer == null)
            {
                return failed("Cannot find Farmer Gricoller");
            }
            int index = -1;
            for (String option : SHOP_OPTIONS)
            {
                index = index < 0 ? optionIndex(farmer.getActions(), option) : index;
            }
            if (index < 0)
            {
                return failed("Farmer Gricoller has no shop option");
            }
            ClickManagerUtil.queueClickBox(farmer);
            NpcAPI.interact(farmer, index);
            context.put("clicked", tick());
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (ShopAPI.isOpen())
            {
                context.put("waited", 0);
                return step + 2;
            }
            if (!tracker.getState().isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, "The reward shop did not open" + message(context));
            }
            return step + 1;
        });
        add(context -> {
            if (tracker.getState().getOwned().contains(reward) || !ShopAPI.isOpen())
            {
                ShopAPI.close();
                return end();
            }
            if (paused(context))
            {
                return step + 2;
            }
            if (context.getOrDefault("bought", false))
            {
                if (waited(context, RESPONSE_TICKS))
                {
                    ShopAPI.close();
                    return failed("Buying " + reward + " did not work" + message(context));
                }
                return step + 2;
            }
            int itemId = -1;
            for (int id : reward.getIds())
            {
                itemId = itemId < 0 && ShopAPI.shopContains(id) ? id : itemId;
            }
            if (itemId < 0)
            {
                ShopAPI.close();
                return failed("The reward shop does not sell " + reward);
            }
            ShopAPI.buy1(itemId);
            context.put("bought", true);
            context.put("clicked", tick());
            acted(context);
            return step + 2;
        });
        return this;
    }

    private TitheBuilder step(WorldPoint template, String status)
    {
        int step = currentStep;
        add(context -> {
            WorldPoint tile = tracker.getCoords().instance(template);
            if (tile == null || template.equals(tracker.getState().getPlayer()))
            {
                return end();
            }
            MovementAPI.walkToWorldPoint(tile);
            context.put("waited", 0);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (template.equals(tracker.getState().getPlayer()) || waited(context, MOVE_TICKS))
            {
                return end();
            }
            return step + 1;
        });
        return this;
    }

    private static int optionIndex(String[] actions, String option)
    {
        if (actions == null)
        {
            return -1;
        }
        for (int i = 0; i < actions.length; i++)
        {
            if (actions[i] != null && actions[i].equalsIgnoreCase(option))
            {
                return i;
            }
        }
        return -1;
    }

    private String message(StepContext context)
    {
        String message = tracker.getLastMessage();
        int clicked = context.getOrDefault("clicked", Integer.MAX_VALUE);
        return message != null && tracker.getLastMessageTick() >= clicked ? " (" + message + ")" : "";
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
        return end();
    }

    private int end()
    {
        plugin.actionEnded();
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

    private static int rollDelay(TitheFarmConfig config)
    {
        double value = config.tickDelayWeightedDistribution()
                ? -Math.log(Math.abs(random.nextGaussian())) * config.tickDelayDeviation() + config.tickDelayTarget()
                : Math.round(random.nextGaussian() * config.tickDelayDeviation() + config.tickDelayTarget());
        return (int) Math.max(config.tickDelayMin(), Math.min(config.tickDelayMax(), value));
    }
}
