package network.server.account;

import network.protocol.NetworkMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/** Server-owned account metadata plus the opaque serialized client profile. */
public final class ServerAccount implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    public static final int MAX_PROFILE_BYTES = 6 * 1024 * 1024;

    private String username;
    private String nickname;
    private String email;

    private String passwordSalt;
    private String passwordVerifier;
    private String securityQuestion;
    private String securityAnswerSalt;
    private String securityAnswerVerifier;
    private String persistentTokenHash;

    private byte[] profilePayload;

    // Server-visible fields needed by leaderboard/account screens later.
    private int coins;
    private int gems;
    private int totalGamesPlayed;
    private String lastChapter;
    private String lastLevel;
    private int miniGamesWon;
    private int dailyQuestsDone;
    private int totalQuestsDone;
    private Integer myPoint;

    private final long createdAtEpochMillis;
    private long updatedAtEpochMillis;

    private ServerAccount(String username, String nickname, String email, String securityQuestion,
                          byte[] profilePayload) {
        this(username, nickname, email, securityQuestion, profilePayload,
                System.currentTimeMillis(), System.currentTimeMillis());
    }

    private ServerAccount(String username, String nickname, String email, String securityQuestion,
                          byte[] profilePayload, long createdAtEpochMillis, long updatedAtEpochMillis) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.securityQuestion = securityQuestion;
        this.profilePayload = clonePayload(profilePayload);
        this.createdAtEpochMillis = createdAtEpochMillis;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
    }

    public static ServerAccount fromRegistration(NetworkMessage request) {
        String username = required(request, "username", "username is required");
        String nickname = required(request, "nickname", "nickname is required");
        String email = required(request, "email", "email is required");
        String passwordHash = required(request, "passwordHash", "password hash is required");
        String question = required(request, "securityQuestion", "security question is required");
        String answerHash = required(request, "securityAnswerHash", "security answer hash is required");
        validateHash(passwordHash, "password hash");
        validateHash(answerHash, "security answer hash");
        byte[] payload = requireProfile(request.getBinaryPayload());

        ServerAccount account = new ServerAccount(username.trim(), nickname.trim(), email.trim(), question.trim(), payload);
        account.setPasswordHash(passwordHash);
        account.setSecurityAnswerHash(answerHash);
        account.applyProfileMetadata(request);
        return account;
    }

    public void applyProfileSync(NetworkMessage request) {
        byte[] payload = requireProfile(request.getBinaryPayload());
        String incomingUsername = request.get("username");
        if (incomingUsername != null && !incomingUsername.isBlank()
                && !incomingUsername.trim().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("profile username does not match the authenticated account");
        }
        String incomingNickname = request.get("nickname");
        String incomingEmail = request.get("email");
        if (incomingNickname != null && !incomingNickname.isBlank()) nickname = incomingNickname.trim();
        if (incomingEmail != null && !incomingEmail.isBlank()) email = incomingEmail.trim();
        profilePayload = clonePayload(payload);
        applyProfileMetadata(request);
        touch();
    }

    public void applyRename(NetworkMessage request, String newUsername) {
        if (newUsername == null || newUsername.isBlank()) throw new IllegalArgumentException("new username is required");
        byte[] payload = requireProfile(request.getBinaryPayload());
        username = newUsername.trim();
        String incomingNickname = request.get("nickname");
        String incomingEmail = request.get("email");
        if (incomingNickname != null && !incomingNickname.isBlank()) nickname = incomingNickname.trim();
        if (incomingEmail != null && !incomingEmail.isBlank()) email = incomingEmail.trim();
        profilePayload = clonePayload(payload);
        applyProfileMetadata(request);
        touch();
    }

    public void applyPasswordChangeProfile(NetworkMessage request) {
        byte[] payload = request.getBinaryPayload();
        if (payload != null && payload.length > 0) profilePayload = clonePayload(requireProfile(payload));
        String incomingNickname = request.get("nickname");
        String incomingEmail = request.get("email");
        if (incomingNickname != null && !incomingNickname.isBlank()) nickname = incomingNickname.trim();
        if (incomingEmail != null && !incomingEmail.isBlank()) email = incomingEmail.trim();
        applyProfileMetadata(request);
        touch();
    }

    private void applyProfileMetadata(NetworkMessage request) {
        coins = nonNegative(request.getInt("coins", coins));
        gems = nonNegative(request.getInt("gems", gems));
        totalGamesPlayed = nonNegative(request.getInt("totalGamesPlayed", totalGamesPlayed));
        miniGamesWon = nonNegative(request.getInt("miniGamesWon", miniGamesWon));
        dailyQuestsDone = nonNegative(request.getInt("dailyQuestsDone", dailyQuestsDone));
        totalQuestsDone = nonNegative(request.getInt("totalQuestsDone", totalQuestsDone));
        if (request.get("lastChapter") != null) lastChapter = request.get("lastChapter");
        if (request.get("lastLevel") != null) lastLevel = request.get("lastLevel");
        touch();
    }

    public boolean verifyPasswordHash(String clientPasswordHash) {
        if (!ServerCrypto.looksLikeSha256(clientPasswordHash)) return false;
        return ServerCrypto.constantTimeEquals(passwordVerifier,
                ServerCrypto.saltedVerifier(passwordSalt, clientPasswordHash));
    }

    public void setPasswordHash(String clientPasswordHash) {
        validateHash(clientPasswordHash, "password hash");
        passwordSalt = ServerCrypto.randomToken(18);
        passwordVerifier = ServerCrypto.saltedVerifier(passwordSalt, clientPasswordHash);
        touch();
    }

    public boolean verifySecurityAnswerHash(String clientAnswerHash) {
        if (!ServerCrypto.looksLikeSha256(clientAnswerHash)) return false;
        return ServerCrypto.constantTimeEquals(securityAnswerVerifier,
                ServerCrypto.saltedVerifier(securityAnswerSalt, clientAnswerHash));
    }

    public void setSecurityAnswerHash(String clientAnswerHash) {
        validateHash(clientAnswerHash, "security answer hash");
        securityAnswerSalt = ServerCrypto.randomToken(18);
        securityAnswerVerifier = ServerCrypto.saltedVerifier(securityAnswerSalt, clientAnswerHash);
        touch();
    }

    public boolean verifyPersistentToken(String rawToken) {
        return rawToken != null && persistentTokenHash != null
                && ServerCrypto.constantTimeEquals(persistentTokenHash, ServerCrypto.sha256(rawToken));
    }

    public void setPersistentToken(String rawToken) {
        persistentTokenHash = rawToken == null || rawToken.isBlank() ? null : ServerCrypto.sha256(rawToken);
        touch();
    }

    public void clearPersistentToken() {
        persistentTokenHash = null;
        touch();
    }

    public ServerAccount copy() {
        ServerAccount result = new ServerAccount(username, nickname, email, securityQuestion, profilePayload,
                createdAtEpochMillis, updatedAtEpochMillis);
        result.passwordSalt = passwordSalt;
        result.passwordVerifier = passwordVerifier;
        result.securityAnswerSalt = securityAnswerSalt;
        result.securityAnswerVerifier = securityAnswerVerifier;
        result.persistentTokenHash = persistentTokenHash;
        result.coins = coins;
        result.gems = gems;
        result.totalGamesPlayed = totalGamesPlayed;
        result.lastChapter = lastChapter;
        result.lastLevel = lastLevel;
        result.miniGamesWon = miniGamesWon;
        result.dailyQuestsDone = dailyQuestsDone;
        result.totalQuestsDone = totalQuestsDone;
        result.myPoint = myPoint;
        return result;
    }

    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getEmail() { return email; }
    public String getSecurityQuestion() { return securityQuestion; }
    public byte[] getProfilePayload() { return clonePayload(profilePayload); }
    public int getCoins() { return coins; }
    public int getGems() { return gems; }
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public String getLastChapter() { return lastChapter; }
    public String getLastLevel() { return lastLevel; }
    public int getMiniGamesWon() { return miniGamesWon; }
    public int getDailyQuestsDone() { return dailyQuestsDone; }
    public int getTotalQuestsDone() { return totalQuestsDone; }
    public Integer getMyPoint() { return myPoint; }
    public long getCreatedAtEpochMillis() { return createdAtEpochMillis; }
    public long getUpdatedAtEpochMillis() { return updatedAtEpochMillis; }
    public boolean hasPersistentToken() { return persistentTokenHash != null; }

    public void setMyPoint(Integer myPoint) {
        this.myPoint = myPoint == null ? null : Math.max(0, myPoint);
        touch();
    }

    private void touch() { updatedAtEpochMillis = System.currentTimeMillis(); }
    private static int nonNegative(int value) { return Math.max(0, value); }

    private static String required(NetworkMessage request, String key, String message) {
        String value = request == null ? null : request.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    private static void validateHash(String hash, String label) {
        if (!ServerCrypto.looksLikeSha256(hash)) throw new IllegalArgumentException(label + " must be SHA-256 hex");
    }

    private static byte[] requireProfile(byte[] payload) {
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("profile payload is required");
        if (payload.length > MAX_PROFILE_BYTES) throw new IllegalArgumentException("profile payload is too large");
        return payload;
    }

    private static byte[] clonePayload(byte[] payload) {
        return payload == null ? null : Arrays.copyOf(payload, payload.length);
    }
}
