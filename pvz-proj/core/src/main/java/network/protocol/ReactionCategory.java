package network.protocol;

import java.io.Serializable;

/** Mandatory text/emoji reactions plus the bonus animated-sticker category. */
public enum ReactionCategory implements Serializable {
    TEXT,
    EMOJI,
    STICKER
}
