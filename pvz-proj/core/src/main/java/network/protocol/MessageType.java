package network.protocol;

import java.io.Serializable;

/**
 * Wire-level message kinds shared by every phase-three client and server.
 *
 * Keep this enum backward compatible once the server is deployed: add new values,
 * but do not rename existing values that may be present in serialized traffic.
 */
public enum MessageType implements Serializable {
    // Connection / generic replies
    PING,
    SUCCESS,
    ERROR,

    // Account lifecycle
    REGISTER,
    LOGIN,
    RESUME_SESSION,
    LOGOUT,
    ACCOUNT_SYNC,
    ACCOUNT_RENAME,
    CHANGE_PASSWORD,
    PASSWORD_RESET_LOOKUP,
    PASSWORD_RESET_VERIFY,
    PASSWORD_RESET_COMMIT,
    SESSION_REPLACED,

    // Leaderboard / scored game
    LEADERBOARD_REQUEST,
    SCORE_SUBMIT,

    // Matchmaking
    DIRECT_CHALLENGE,
    CHALLENGE_INCOMING,
    CHALLENGE_RESPONSE,
    RANDOM_QUEUE_JOIN,
    RANDOM_QUEUE_LEAVE,

    // Authoritative I, Zombie match
    MATCH_STARTED,
    MATCH_ACTION,
    MATCH_SNAPSHOT,
    MATCH_FINISHED,
    MATCH_LEAVE,

    // In-match reactions
    REACTION_SEND,
    REACTION_RECEIVED
}
