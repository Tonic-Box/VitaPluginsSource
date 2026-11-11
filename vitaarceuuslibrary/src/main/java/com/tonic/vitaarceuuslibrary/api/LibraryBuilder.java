package com.tonic.vitaarceuuslibrary.api;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.ItemEx;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.ClickManagerUtil;
import com.tonic.util.handler.AbstractHandlerBuilder;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;
import com.tonic.vitaarceuuslibrary.ArceuusLibraryConfig;
import com.tonic.vitaarceuuslibrary.ArceuusLibraryPlugin;
import com.tonic.vitaarceuuslibrary.data.Action;
import com.tonic.vitaarceuuslibrary.data.XpChoice;
import com.tonic.vitaarceuuslibrary.library.Bookcase;
import com.tonic.vitaarceuuslibrary.library.Customer;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds the step handler for one library action: walk there, click, then follow the dialogue or wait for the result, with action delays, controlled click boxes and retries.
 */
public final class LibraryBuilder extends AbstractHandlerBuilder<LibraryBuilder>
{
    private static final int DIRECT_RANGE = 12;
    private static final int ATTEMPTS = 3;
    private static final int RESPONSE_TICKS = 10;
    private static final int SEARCH_TICKS = 20;
    private static final int CLOSE_TICKS = 4;
    private static final int[] CONTINUE_WIDGETS = {InterfaceID.ChatLeft.CONTINUE, InterfaceID.ChatRight.CONTINUE, InterfaceID.Objectbox.UNIVERSE, InterfaceID.Messagebox.CONTINUE};
    private static final Random random = new Random();

    private final ArceuusLibraryPlugin plugin;
    private final LibraryTracker tracker;
    private final LibraryMap map;

    private LibraryBuilder(ArceuusLibraryPlugin plugin)
    {
        this.plugin = plugin;
        this.tracker = plugin.getTracker();
        this.map = plugin.getMap();
    }

    /**
     * Builds the handler for an action.
     *
     * @param plugin the plugin
     * @param action the action
     * @return the handler
     */
    public static StepHandler forAction(ArceuusLibraryPlugin plugin, Action action)
    {
        LibraryBuilder builder = new LibraryBuilder(plugin);
        switch (action.getType())
        {
            case WALK_TO_LIBRARY:
                return builder.walkTo(LibraryAPI.GROUND_FLOOR).build();
            case TALK:
                return builder.approach(() -> builder.customerStands(action.getCustomer()))
                        .talk(action.getCustomer(), () -> builder.tracker.getRequest() != null || builder.tracker.getBusy().contains(action.getCustomer()) || builder.tracker.getLastServed() == action.getCustomer())
                        .build();
            case HAND_IN:
                return builder.approach(() -> builder.customerStands(action.getCustomer()))
                        .talk(action.getCustomer(), () -> builder.tracker.getRequest() == null)
                        .build();
            case SEARCH:
                return builder.approach(() -> builder.map.standTiles(action.getBookcase()))
                        .search(action.getBookcase())
                        .build();
            case READ:
                return builder.read().build();
            default:
                return builder.addDelay(3).build();
        }
    }

    private List<WorldPoint> customerStands(Customer customer)
    {
        NpcEx npc = LibraryAPI.npc(customer);
        return map.standTiles(npc == null ? customer.getHome() : npc.getWorldPoint());
    }

    private LibraryBuilder approach(Supplier<List<WorldPoint>> stands)
    {
        int step = currentStep;
        add(context -> {
            WorldPoint player = LibraryAPI.playerTile();
            List<WorldPoint> tiles = stands.get();
            if (player == null || tiles.isEmpty())
            {
                return step + 2;
            }
            int distance = map.from(Collections.singletonList(player)).toAny(tiles);
            boolean samePlane = tiles.get(0).getPlane() == player.getPlane();
            return samePlane && distance <= DIRECT_RANGE ? step + 2 : step + 1;
        });
        walkToWorldPointSupplier(() -> {
            WorldPoint player = LibraryAPI.playerTile();
            List<WorldPoint> tiles = stands.get();
            WorldPoint nearest = player == null ? null : map.from(Collections.singletonList(player)).nearest(tiles);
            return nearest != null ? nearest : tiles.get(0);
        }, false);
        return this;
    }

    private LibraryBuilder talk(Customer customer, BooleanSupplier done)
    {
        int step = currentStep;
        add(context -> {
            if (done.getAsBoolean())
            {
                return step + 2;
            }
            if (paused(context))
            {
                return step;
            }
            NpcEx npc = LibraryAPI.npc(customer);
            if (npc == null)
            {
                return failed("Cannot find " + customer);
            }
            ClickManagerUtil.queueClickBox(npc);
            NpcAPI.interact(npc, "Talk-to");
            acted(context);
            plugin.setStatus("Talking to " + customer);
            return step + 1;
        });
        add(context -> {
            if (done.getAsBoolean())
            {
                return closeDialogue(context, step + 2);
            }
            if (DialogueAPI.isViewingOptions())
            {
                if (!chooseOption(LibraryTracker.REQUEST_OPTION))
                {
                    return failed("Unexpected options " + DialogueAPI.getOptions());
                }
                context.put("waited", 0);
                return step + 1;
            }
            if (DialogueAPI.dialoguePresent())
            {
                continueDialogue();
                context.put("waited", 0);
                return step + 1;
            }
            if (waited(context, RESPONSE_TICKS) && !MovementAPI.isMoving())
            {
                return retry(context, step, customer + " did not answer");
            }
            return step + 1;
        });
        return this;
    }

    private LibraryBuilder search(Bookcase bookcase)
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            TileObjectEx shelf = LibraryAPI.bookshelf(bookcase.getLocation());
            if (shelf == null)
            {
                return failed("Cannot find the bookshelf at " + bookcase.getLocation());
            }
            ClickManagerUtil.queueClickBox(shelf);
            shelf.interact("Search");
            tracker.searching(bookcase.getLocation());
            acted(context);
            plugin.setStatus("Searching a bookshelf");
            return step + 1;
        });
        add(context -> {
            if (tracker.getSearching() == null)
            {
                return closeDialogue(context, step + 2);
            }
            if (waited(context, SEARCH_TICKS) && !MovementAPI.isMoving())
            {
                tracker.cancelSearch();
                return retry(context, step, "The bookshelf search gave no result");
            }
            return step + 1;
        });
        return this;
    }

    private LibraryBuilder read()
    {
        int step = currentStep;
        add(context -> {
            if (paused(context))
            {
                return step;
            }
            ItemEx book = InventoryAPI.getItem(ItemID.ARCEUUS_LIBRARY_REWARD);
            if (book == null)
            {
                return HandlerBuilder.END_EXECUTION;
            }
            context.put("rewards", tracker.getRewards());
            context.put("skill", chooseSkill());
            ClickManagerUtil.queueClickBox(book);
            InventoryAPI.interact(book, "Read");
            acted(context);
            plugin.setStatus("Reading a Book of arcane knowledge for " + context.get("skill"));
            return step + 1;
        });
        add(context -> {
            int before = context.getOrDefault("rewards", 0);
            if (tracker.getRewards() < before)
            {
                return closeDialogue(context, step + 2);
            }
            Skill skill = context.get("skill");
            String name = skill == Skill.MAGIC ? "Magic" : "Runecraft";
            if (DialogueAPI.isViewingOptions())
            {
                if (!chooseOption(name))
                {
                    return failed("No " + name + " option in " + DialogueAPI.getOptions());
                }
                context.put("waited", 0);
                return step + 1;
            }
            Widget reward = SkillAPI.getRewardWidget(skill);
            if (reward != null && !reward.isHidden())
            {
                ClickManagerUtil.queueClickBox(reward);
                WidgetAPI.interact(reward, 0);
                context.put("waited", 0);
                return step + 1;
            }
            if (DialogueAPI.dialoguePresent())
            {
                continueDialogue();
                context.put("waited", 0);
                return step + 1;
            }
            if (waited(context, RESPONSE_TICKS))
            {
                return retry(context, step, "Reading the book did not offer a choice");
            }
            return step + 1;
        });
        return this;
    }

    private Skill chooseSkill()
    {
        XpChoice choice = plugin.getConfig().xpChoice();
        switch (choice)
        {
            case RUNECRAFT:
                return Skill.RUNECRAFT;
            case LOWEST:
                return SkillAPI.getLevel(Skill.RUNECRAFT) < SkillAPI.getLevel(Skill.MAGIC) ? Skill.RUNECRAFT : Skill.MAGIC;
            case RANDOM:
                return random.nextBoolean() ? Skill.MAGIC : Skill.RUNECRAFT;
            default:
                return Skill.MAGIC;
        }
    }

    private int closeDialogue(StepContext context, int next)
    {
        if (!DialogueAPI.dialoguePresent() || waited(context, CLOSE_TICKS))
        {
            context.put("waited", 0);
            return next;
        }
        continueDialogue();
        return next - 1;
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

    private static int rollDelay(ArceuusLibraryConfig config)
    {
        double value = config.tickDelayWeightedDistribution()
                ? -Math.log(Math.abs(random.nextGaussian())) * config.tickDelayDeviation() + config.tickDelayTarget()
                : Math.round(random.nextGaussian() * config.tickDelayDeviation() + config.tickDelayTarget());
        return (int) Math.max(config.tickDelayMin(), Math.min(config.tickDelayMax(), value));
    }
}
