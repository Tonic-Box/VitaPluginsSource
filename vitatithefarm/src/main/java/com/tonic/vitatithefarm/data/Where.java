package com.tonic.vitatithefarm.data;

/**
 * Where the player is, as far as Tithe Farm is concerned.
 */
public enum Where
{
    ELSEWHERE("Elsewhere"),
    LOBBY("By the seed table"),
    FARM("In the farm");

    private final String name;

    Where(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
