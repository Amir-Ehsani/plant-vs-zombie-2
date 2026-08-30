package network.protocol;

import java.io.Serializable;

/** The side controlled by a player in network/Couch I, Zombie. */
public enum GameRole implements Serializable {
    PLANTS,
    ZOMBIES;

    public GameRole opponent() {
        return this == PLANTS ? ZOMBIES : PLANTS;
    }
}
