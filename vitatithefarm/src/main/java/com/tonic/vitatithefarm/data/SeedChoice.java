package com.tonic.vitatithefarm.data;

/**
 * The seed setting: the best tier for the Farming level, or a fixed tier.
 */
public enum SeedChoice
{
    AUTOMATIC("Automatic", null),
    GOLOVANOVA("Golovanova", Seed.GOLOVANOVA),
    BOLOGANO("Bologano", Seed.BOLOGANO),
    LOGAVANO("Logavano", Seed.LOGAVANO);

    private final String name;
    private final Seed seed;

    SeedChoice(String name, Seed seed)
    {
        this.name = name;
        this.seed = seed;
    }

    /**
     * @param farmingLevel the Farming level
     * @return the seed to plant, or null when the level is too low for it
     */
    public Seed resolve(int farmingLevel)
    {
        if (seed == null)
        {
            return Seed.best(farmingLevel);
        }
        return farmingLevel >= seed.getLevel() ? seed : null;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
