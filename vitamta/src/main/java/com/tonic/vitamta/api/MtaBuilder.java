package com.tonic.vitamta.api;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.entities.TileItemAPI;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.BankAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.spellbooks.Standard;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileItemEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.WorldPointUtil;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitamta.MtaConfig;
import com.tonic.vitamta.MtaPlugin;
import com.tonic.vitamta.data.Action;
import com.tonic.vitamta.data.Reward;
import com.tonic.vitamta.data.Room;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.widgets.Widget;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds the step handler for one arena action: click, then wait for the game to show it worked, with action delays, controlled click boxes and retries.
 */
public final class MtaBuilder extends AbstractHandlerBuilder<MtaBuilder>
{
    private static final int ATTEMPTS = 3;
    private static final int RESPONSE_TICKS = 6;
    private static final int LOAD_TICKS = 12;
    private static final int DIRECT_RANGE = 15;
    private static final int POINTS_TICKS = 3;
    private static final String NEW_HERE = "I'm new to this place";
    private static final String GOODBYE = "Thanks, bye!";
    private static final int[] CONTINUE_WIDGETS = {InterfaceID.ChatLeft.CONTINUE, InterfaceID.ChatRight.CONTINUE, InterfaceID.Objectbox.UNIVERSE, InterfaceID.Messagebox.CONTINUE};
    private static final Random random = new Random();

    private final MtaPlugin plugin;
    private final ArenaTracker tracker;

    private MtaBuilder(MtaPlugin plugin)
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
    public static StepHandler forAction(MtaPlugin plugin, Action action)
    {
        MtaBuilder builder = new MtaBuilder(plugin);
        ArenaTracker tracker = builder.tracker;
        switch (action.getType())
        {
            case WALK_TO_ARENA:
                return builder.walkTo(ArenaTracker.LOBBY).build();
            case BANK_COINS:
                return builder.walkTo(ArenaTracker.BANK).bankCoins().build();
            case TALK_TO_ENTRANCE_GUARDIAN:
                return builder.approach(ArenaTracker.ENTRANCE).introduce().build();
            case READ_POINTS:
                return builder.approach(WorldPointUtil.getCenter(ArenaTracker.LOBBY)).waitTicks(POINTS_TICKS).build();
            case EQUIP:
                return builder.equip(action.getSetup().getWeapon()).equip(action.getSetup().getShield()).build();
            case ENTER_ROOM:
                return builder.approach(WorldPointUtil.getCenter(ArenaTracker.LOBBY))
                        .interact("Entering the " + action.getRoom(), () -> builder.object(action.getObjectId(), null), "Enter", () -> tracker.getState().getRoom() == action.getRoom(), LOAD_TICKS)
                        .build();
            case LEAVE_ROOM:
            {
                Room from = tracker.getState().getRoom();
                return builder.interact(action.getReason(), () -> builder.object(ObjectID.MAGICTRAINING_RETURNDOOR, null), "Enter", () -> tracker.getState().getRoom() != from, LOAD_TICKS).build();
            }
            case BUY_REWARD:
                return builder.approach(ArenaTracker.REWARDS).buy(action.getReward()).build();
            case TAKE_SHAPES:
                return builder.interactForMore(action.getReason(), action.getObjectId(), action.getTile(), "Take-from", action.getItemId()).build();
            case GRAB_BONES:
                return builder.grabBone(action).build();
            case TAKE_ITEMS:
                return builder.interactForMore(action.getReason(), action.getObjectId(), action.getTile(), "Take-5", action.getItemId()).build();
            case TAKE_DRAGONSTONE:
                return builder.takeDragonstone(action.getTile()).build();
            case CAST_ON_ITEM:
                return builder.castOnItem(action.getSpell(), action.getItemId(), action.getReason()).build();
            case CAST:
                return builder.castFruit(action.getSpell(), action.getReason()).build();
            case DEPOSIT:
                return builder.deposit(action.getObjectId(), action.getItemId(), action.getReason()).build();
            case EAT:
                return builder.eat(action.getItemId(), action.getCount()).build();
            case DROP:
                return builder.drop(action.getItemId()).build();
            case SEARCH_CUPBOARD:
                return builder.search(action).build();
            case DODGE:
            case MOVE:
                return builder.step(action.getTile(), action.getReason()).build();
            case TELEGRAB:
                return builder.telegrab().build();
            case NEW_MAZE:
                return builder.newMaze().build();
            default:
                return builder.waitTicks(1).build();
        }
    }

    private MtaBuilder approach(WorldPoint target)
    {
        int step = currentStep;
        add(context -> {
            WorldPoint player = tracker.getState().getPlayer();
            boolean near = player != null && player.getPlane() == target.getPlane() && player.distanceTo(target) <= DIRECT_RANGE;
            return near ? step + 2 : step + 1;
        });
        walkTo(target, false);
        return this;
    }

    private MtaBuilder waitTicks(int ticks)
    {
        int step = currentStep;
        add(context -> waited(context, ticks) ? HandlerBuilder.END_EXECUTION : step);
        return this;
    }

    private MtaBuilder interact(String status, Supplier<TileObjectEx> target, String option, BooleanSupplier done, int timeout)
    {
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
            TileObjectEx object = target.get();
            if (object == null)
            {
                return failed("Cannot find where to " + option.toLowerCase());
            }
            ClickManagerUtil.queueClickBox(object);
            object.interact(option);
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (done.getAsBoolean())
            {
                return closeDialogue(context, HandlerBuilder.END_EXECUTION, step + 1);
            }
            if (DialogueAPI.dialoguePresent())
            {
                String text = dialogueText();
                if (text != null)
                {
                    tracker.gameMessage(text);
                }
                continueDialogue();
                return step + 1;
            }
            if (MovementAPI.isMoving())
            {
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

    private MtaBuilder interactForMore(String status, int objectId, WorldPoint tile, String option, int itemId)
    {
        int before = count(itemId);
        return interact(status, () -> object(objectId, tile), option, () -> count(itemId) > before, RESPONSE_TICKS);
    }

    private MtaBuilder grabBone(Action action)
    {
        int before = bones();
        return interact(action.getReason(), () -> object(action.getObjectId(), action.getTile()), "Grab", () -> bones() > before, RESPONSE_TICKS);
    }

    private MtaBuilder deposit(int objectId, int itemId, String status)
    {
        return interact(status, () -> object(objectId, null), "Deposit", () -> count(itemId) == 0, LOAD_TICKS);
    }

    private MtaBuilder search(Action action)
    {
        int index = cupboardIndex(action.getObjectId());
        int step = currentStep;
        add(context -> {
            tracker.searching(index);
            return step + 1;
        });
        return interact(action.getReason(), () -> object(action.getObjectId(), action.getTile()), "Search", () -> !tracker.isSearching(), RESPONSE_TICKS);
    }

    private MtaBuilder takeDragonstone(WorldPoint tile)
    {
        int before = count(ItemID.MAGICTRAINING_DRAGONSTONE);
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            TileItemEx item = TileItemAPI.search().withId(ItemID.MAGICTRAINING_DRAGONSTONE).atLocation(tile).first();
            if (item == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            ClickManagerUtil.queueClickBox(item);
            item.interact("Take");
            acted(context);
            plugin.setStatus("Picking up a dragonstone");
            return step + 1;
        });
        add(context -> {
            if (count(ItemID.MAGICTRAINING_DRAGONSTONE) > before)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (!MovementAPI.isMoving() && waited(context, LOAD_TICKS))
            {
                return HandlerBuilder.END_EXECUTION;
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder castOnItem(Spell spell, int itemId, String status)
    {
        int before = count(itemId);
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            ItemEx item = InventoryAPI.getItem(itemId);
            if (item == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            ClickManagerUtil.queueClickBox(item);
            spell.castOn(item);
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (count(itemId) < before)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, status + " did not work");
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder castFruit(Spell spell, String status)
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            spell.cast();
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (bones() == 0)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, status + " did not work");
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder eat(int food, int times)
    {
        int step = currentStep;
        add(context -> {
            if (context.getOrDefault("eaten", 0) >= times)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (paused(context))
            {
                return step;
            }
            ItemEx item = InventoryAPI.getItem(food);
            if (item == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            ClickManagerUtil.queueClickBox(item);
            InventoryAPI.interact(item, "Eat");
            context.put("eaten", context.getOrDefault("eaten", 0) + 1);
            context.put("pause", Math.max(rollDelay(plugin.getConfig()), 1));
            plugin.setStatus("Eating");
            return step;
        });
        return this;
    }

    private MtaBuilder drop(int itemId)
    {
        add(() ->
        {
            InventoryAPI.dropAll(itemId);
        });
        return waitTicks(1);
    }

    private MtaBuilder step(WorldPoint tile, String status)
    {
        int step = currentStep;
        add(context -> {
            WorldPoint player = tracker.getState().getPlayer();
            if (tile.equals(player))
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (paused(context))
            {
                return step;
            }
            MovementAPI.walkToWorldPoint(tile);
            acted(context);
            plugin.setStatus(status);
            return step + 1;
        });
        add(context -> {
            if (tile.equals(tracker.getState().getPlayer()))
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (!MovementAPI.isMoving() && waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, status + " did not work");
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder telegrab()
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            NpcEx guardian = guardian();
            if (guardian == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            ClickManagerUtil.queueClickBox(guardian);
            Standard.TELEKINETIC_GRAB.castOn(guardian);
            acted(context);
            plugin.setStatus("Pulling the maze guardian");
            return step + 1;
        });
        add(context -> {
            ArenaState state = tracker.getState();
            if (state.isGuardianMoving() || state.isTelegrabInFlight())
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, "Telekinetic Grab did not move the guardian");
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder newMaze()
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            NpcEx guardian = guardian();
            if (guardian == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            ClickManagerUtil.queueClickBox(guardian);
            NpcAPI.interact(guardian, "New-maze");
            acted(context);
            plugin.setStatus("Starting a new maze");
            return step + 1;
        });
        add(context -> {
            if (!tracker.getState().isGuardianComplete())
            {
                return HandlerBuilder.END_EXECUTION;
            }
            if (!MovementAPI.isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, "The guardian did not start a new maze");
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder bankCoins()
    {
        int step = currentStep;
        add(context -> {
            if (BankAPI.isOpen())
            {
                return step + 2;
            }
            if (paused(context))
            {
                return step;
            }
            TileObjectEx chest = TileObjectAPI.search().withNameContains("Bank").withAnyOfActions("Use", "Bank").sortNearest().first();
            if (chest == null)
            {
                return failed("Cannot find the bank chest");
            }
            ClickManagerUtil.queueClickBox(chest);
            chest.interact("Use", "Bank");
            acted(context);
            plugin.setStatus("Opening the bank");
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
            if (count(ItemID.COINS) == 0)
            {
                BankAPI.close();
                return HandlerBuilder.END_EXECUTION;
            }
            if (paused(context))
            {
                return step + 2;
            }
            if (!context.getOrDefault("deposited", false))
            {
                BankAPI.deposit(ItemID.COINS, -1);
                context.put("deposited", true);
                acted(context);
                plugin.setStatus("Depositing coins");
                return step + 2;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return failed("Depositing coins did not work");
            }
            return step + 2;
        });
        return this;
    }

    private MtaBuilder introduce()
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            NpcEx guardian = NpcAPI.search().withIds(NpcID.MAGICTRAINING_GUARD_ENTRANCE).nearest();
            if (guardian == null)
            {
                return failed("Cannot find the Entrance Guardian");
            }
            ClickManagerUtil.queueClickBox(guardian);
            NpcAPI.interact(guardian, "Talk-to");
            acted(context);
            plugin.setStatus("Talking to the Entrance Guardian");
            return step + 1;
        });
        add(context -> {
            if (DialogueAPI.isViewingOptions())
            {
                context.put("waited", 0);
                if (chooseOption(GOODBYE))
                {
                    tracker.introduced();
                    return step + 2;
                }
                if (!chooseOption(NEW_HERE))
                {
                    return failed("Unexpected options " + DialogueAPI.getOptions());
                }
                return step + 1;
            }
            if (DialogueAPI.dialoguePresent())
            {
                context.put("waited", 0);
                continueDialogue();
                return step + 1;
            }
            if (!MovementAPI.isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, "The Entrance Guardian did not answer");
            }
            return step + 1;
        });
        add(context ->
        {
            return closeDialogue(context, HandlerBuilder.END_EXECUTION, step + 2);
        });
        return this;
    }

    private MtaBuilder equip(int itemId)
    {
        if (itemId == Castability.NONE)
        {
            return this;
        }
        int step = currentStep;
        add(context -> {
            if (tracker.getState().getWeapon() == itemId || tracker.getState().getShield() == itemId)
            {
                return step + 2;
            }
            if (paused(context))
            {
                return step;
            }
            ItemEx item = InventoryAPI.getItem(itemId);
            if (item == null)
            {
                return failed("The item to wield is gone");
            }
            ClickManagerUtil.queueClickBox(item);
            InventoryAPI.interact(item, "Wield", "Wear", "Equip");
            acted(context);
            plugin.setStatus("Wielding " + item.getName());
            return step + 1;
        });
        add(context -> {
            if (tracker.getState().getWeapon() == itemId || tracker.getState().getShield() == itemId)
            {
                return step + 2;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, "Wielding did not work");
            }
            return step + 1;
        });
        return this;
    }

    private MtaBuilder buy(Reward reward)
    {
        int step = currentStep;
        add(context -> {
            if (WidgetAPI.isVisible(InterfaceID.MagictrainingShop.UNIVERSE))
            {
                return step + 2;
            }
            if (paused(context))
            {
                return step;
            }
            NpcEx guardian = NpcAPI.search().withIds(NpcID.MAGICTRAINING_GUARD_REWARDS).nearest();
            if (guardian == null)
            {
                return failed("Cannot find the Rewards Guardian");
            }
            ClickManagerUtil.queueClickBox(guardian);
            NpcAPI.interact(guardian, "Trade-with");
            acted(context);
            plugin.setStatus("Opening the rewards shop");
            return step + 1;
        });
        add(context -> {
            if (WidgetAPI.isVisible(InterfaceID.MagictrainingShop.UNIVERSE))
            {
                context.put("waited", 0);
                return step + 2;
            }
            if (!MovementAPI.isMoving() && waited(context, LOAD_TICKS))
            {
                return retry(context, step, "The rewards shop did not open");
            }
            return step + 1;
        });
        add(context -> {
            if (paused(context))
            {
                return step + 2;
            }
            Widget item = shopItem(reward);
            if (item == null)
            {
                return failed(reward + " is not in the rewards shop");
            }
            ClickManagerUtil.queueClickBox(item);
            WidgetAPI.interact(item, 0);
            context.put("pause", 1);
            return step + 3;
        });
        add(context -> {
            if (paused(context))
            {
                return step + 3;
            }
            Widget confirm = WidgetAPI.get(InterfaceID.MagictrainingShop.CONFIRM);
            if (confirm == null)
            {
                return failed("The rewards shop has no confirm button");
            }
            ClickManagerUtil.queueClickBox(confirm);
            WidgetAPI.interact(confirm, 0);
            acted(context);
            plugin.setStatus("Buying " + reward);
            return step + 4;
        });
        add(context -> {
            if (tracker.getState().getOwned().contains(reward) || reward.getItemId() > 0 && tracker.getState().count(reward.getItemId()) > 0)
            {
                plugin.bought(reward);
                ClientScriptAPI.closeInterface();
                return HandlerBuilder.END_EXECUTION;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                ClientScriptAPI.closeInterface();
                return failed("Buying " + reward + " did not work");
            }
            return step + 4;
        });
        return this;
    }

    private static Widget shopItem(Reward reward)
    {
        Widget container = WidgetAPI.get(InterfaceID.MagictrainingShop.REWARDS);
        Widget[] children = container == null ? null : container.getDynamicChildren();
        if (children == null)
        {
            return null;
        }
        for (Widget child : children)
        {
            if (child == null)
            {
                continue;
            }
            if (reward.getItemId() > 0 && child.getItemId() == reward.getItemId())
            {
                return child;
            }
            if (reward == Reward.BONES_TO_PEACHES && child.getName() != null && child.getName().contains("Peaches"))
            {
                return child;
            }
        }
        return null;
    }

    private TileObjectEx object(int objectId, WorldPoint tile)
    {
        if (tile != null)
        {
            TileObjectEx exact = TileObjectAPI.search().withId(objectId).atLocation(tile).first();
            if (exact != null)
            {
                return exact;
            }
        }
        return TileObjectAPI.search().withId(objectId).sortNearest().first();
    }

    private static NpcEx guardian()
    {
        return NpcAPI.search().withIds(NpcID.MAGICTRAINING_GUARD_MAZE_INCOMPLETE, NpcID.MAGICTRAINING_GUARD_MAZE_MOVING, NpcID.MAGICTRAINING_GUARD_MAZE_COMPLETE).nearest();
    }

    private static int cupboardIndex(int objectId)
    {
        return (objectId - ObjectID.MAGICTRAINING_ALCHEM_CUPBOARD0) / 2;
    }

    private int count(int itemId)
    {
        return InventoryAPI.count(itemId);
    }

    private static int bones()
    {
        return InventoryAPI.count(ItemID.MAGICTRAINING_BONES1, ItemID.MAGICTRAINING_BONES2, ItemID.MAGICTRAINING_BONES3, ItemID.MAGICTRAINING_BONES4);
    }

    private static String dialogueText()
    {
        Widget text = WidgetAPI.get(InterfaceID.Messagebox.TEXT);
        return text == null || text.isHidden() ? null : text.getText();
    }

    private int closeDialogue(StepContext context, int next, int self)
    {
        if (!DialogueAPI.dialoguePresent() || waited(context, RESPONSE_TICKS))
        {
            context.put("waited", 0);
            return next;
        }
        continueDialogue();
        return self;
    }

    private static boolean chooseOption(String text)
    {
        Widget options = WidgetAPI.get(InterfaceID.Chatmenu.OPTIONS);
        Widget[] children = options == null ? null : options.getChildren();
        if (children != null)
        {
            for (Widget child : children)
            {
                if (child != null && child.getText() != null && child.getText().toLowerCase().contains(text.toLowerCase()))
                {
                    ClickManagerUtil.queueClickBox(child);
                    break;
                }
            }
        }
        return DialogueAPI.selectOption(text);
    }

    private static void continueDialogue()
    {
        for (int widget : CONTINUE_WIDGETS)
        {
            Widget continueWidget = WidgetAPI.get(widget);
            if (continueWidget != null && !continueWidget.isHidden())
            {
                ClickManagerUtil.queueClickBox(continueWidget);
                break;
            }
        }
        DialogueAPI.continueDialogue();
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
        if (PlayerEx.getLocal().getPlayer().getAnimation() == -1)
        {
            context.put("pause", remaining - 1);
        }
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

    private static int rollDelay(MtaConfig config)
    {
        double value = config.tickDelayWeightedDistribution()
                ? -Math.log(Math.abs(random.nextGaussian())) * config.tickDelayDeviation() + config.tickDelayTarget()
                : Math.round(random.nextGaussian() * config.tickDelayDeviation() + config.tickDelayTarget());
        return (int) Math.max(config.tickDelayMin(), Math.min(config.tickDelayMax(), value));
    }
}
