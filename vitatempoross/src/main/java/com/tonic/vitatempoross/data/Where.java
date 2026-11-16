package com.tonic.vitatempoross.data;

/**
 * Where the player is, as far as Tempoross is concerned.
 */
public enum Where
{
    ELSEWHERE("Elsewhere"),
    UNKAH("Ruins of Unkah"),
    LOBBY_BOAT("Waiting on the boat"),
    GAME("Tempoross Cove"),
    GAME_OVER("Game over");

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
