package com.tonic.vitatempoross.api;

/**
 * Tracks an incoming wave from its warning to its end, ending it after a timeout too so a missed message cannot leave it running.
 */
public final class WaveWatch
{
    /**
     * Ticks after the warning when a wave is taken as over regardless of messages.
     */
    public static final int TIMEOUT = 20;

    private static final String WARNING = "colossal wave closes in";
    private static final String[] ENDINGS = {"washes over you", "slams into you"};

    private int start = -1;
    private boolean tethered;

    /**
     * Reads a game message.
     *
     * @param message the message text
     * @param tick the game tick it arrived on
     */
    public void message(String message, int tick)
    {
        String text = message.toLowerCase();
        if (text.contains(WARNING))
        {
            start = tick;
            tethered = false;
            return;
        }
        for (String ending : ENDINGS)
        {
            if (text.contains(ending))
            {
                start = -1;
                return;
            }
        }
    }

    /**
     * Updates the wave for a tick.
     *
     * @param tick the game tick
     * @param isTethered whether the player is tethered
     * @return whether a wave is coming or washing over
     */
    public boolean update(int tick, boolean isTethered)
    {
        if (start < 0)
        {
            return false;
        }
        if (tick - start > TIMEOUT)
        {
            start = -1;
            return false;
        }
        if (isTethered)
        {
            tethered = true;
        }
        else if (tethered)
        {
            start = -1;
            return false;
        }
        return true;
    }

    /**
     * Forgets any wave.
     */
    public void clear()
    {
        start = -1;
        tethered = false;
    }
}
