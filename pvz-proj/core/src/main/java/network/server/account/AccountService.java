package network.server.account;

import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.server.ClientConnection;
import network.server.PvZServer;
import network.server.RequestDispatcher;

/** Registers and implements every phase-three account/session request. */
public final class AccountService {
    private final PvZServer server;
    private final AccountRepository repository;
    private final ServerSessionManager sessions;
    private final PasswordResetManager resets = new PasswordResetManager();

    public AccountService(PvZServer server, AccountRepository repository, ServerSessionManager sessions) {
        this.server = server;
        this.repository = repository;
        this.sessions = sessions;
    }

    public void registerHandlers(RequestDispatcher dispatcher) {
        dispatcher.register(MessageType.REGISTER, this::register);
        dispatcher.register(MessageType.LOGIN, this::login);
        dispatcher.register(MessageType.RESUME_SESSION, this::resumeSession);
        dispatcher.register(MessageType.LOGOUT, this::logout);
        dispatcher.register(MessageType.ACCOUNT_SYNC, this::sync);
        dispatcher.register(MessageType.ACCOUNT_RENAME, this::rename);
        dispatcher.register(MessageType.CHANGE_PASSWORD, this::changePassword);
        dispatcher.register(MessageType.PASSWORD_RESET_LOOKUP, this::resetLookup);
        dispatcher.register(MessageType.PASSWORD_RESET_VERIFY, this::resetVerify);
        dispatcher.register(MessageType.PASSWORD_RESET_COMMIT, this::resetCommit);
    }

    private NetworkMessage register(ClientConnection client, NetworkMessage request) {
        ServerAccount account = ServerAccount.fromRegistration(request);
        validateUsername(account.getUsername());
        if (!repository.create(account)) throw new IllegalArgumentException("username is already taken");
        server.log("registered account: " + account.getUsername());
        return RequestDispatcher.success(request, "registration successful")
                .put("username", account.getUsername());
    }

    private NetworkMessage login(ClientConnection client, NetworkMessage request) {
        String username = required(request, "username", "username is required");
        String passwordHash = required(request, "passwordHash", "password is required");
        ServerAccount account = repository.find(username);
        if (account == null || !account.verifyPasswordHash(passwordHash)) {
            throw new IllegalArgumentException("invalid username or password");
        }

        String persistentToken = null;
        if (request.getBoolean("stayLoggedIn", false)) {
            persistentToken = ServerCrypto.randomToken(32);
            account.setPersistentToken(persistentToken);
            repository.save(account);
        }

        String sessionToken = sessions.createSession(account.getUsername(), client);
        server.getMatchmakingService().sessionStarted(account.getUsername(), client);
        server.log("login: " + account.getUsername() + " from " + client.getRemoteAddress());
        NetworkMessage response = authenticatedResponse(request, account, sessionToken, "login successful");
        if (persistentToken != null) response.put("persistentToken", persistentToken);
        return response;
    }

    private NetworkMessage resumeSession(ClientConnection client, NetworkMessage request) {
        String username = required(request, "username", "username is required");
        String persistentToken = required(request, "persistentToken", "persistent token is required");
        ServerAccount account = repository.find(username);
        if (account == null || !account.verifyPersistentToken(persistentToken)) {
            throw new IllegalArgumentException("saved session is invalid or expired");
        }
        String sessionToken = sessions.createSession(account.getUsername(), client);
        server.getMatchmakingService().sessionStarted(account.getUsername(), client);
        return authenticatedResponse(request, account, sessionToken, "session restored");
    }

    private NetworkMessage logout(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticatedUsername(client, request);
        ServerAccount account = repository.find(username);
        if (account != null && account.hasPersistentToken()) {
            account.clearPersistentToken();
            repository.save(account);
        }
        server.getMatchmakingService().sessionEnded(username, client, "logged out");
        sessions.logout(client, request.getSessionToken());
        return RequestDispatcher.success(request, "logout successful");
    }

    private NetworkMessage sync(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticatedUsername(client, request);
        ServerAccount account = requireAccount(username);
        account.applyProfileSync(request);
        repository.save(account);
        return RequestDispatcher.success(request, "profile synchronized")
                .put("profileUpdatedAt", account.getUpdatedAtEpochMillis());
    }

    private NetworkMessage rename(ClientConnection client, NetworkMessage request) {
        String oldUsername = requireAuthenticatedUsername(client, request);
        String newUsername = required(request, "newUsername", "new username is required").trim();
        validateUsername(newUsername);
        if (oldUsername.equalsIgnoreCase(newUsername)) throw new IllegalArgumentException("new username is the same as current username");
        if (repository.exists(newUsername)) throw new IllegalArgumentException("username is already taken");

        ServerAccount account = requireAccount(oldUsername);
        account.applyRename(request, newUsername);
        if (!repository.rename(oldUsername, account)) throw new IllegalArgumentException("could not rename account");
        sessions.renameUser(oldUsername, newUsername);
        server.getMatchmakingService().renameUser(oldUsername, newUsername, client);
        server.log("account renamed: " + oldUsername + " -> " + newUsername);
        return RequestDispatcher.success(request, "username changed successfully")
                .put("username", newUsername);
    }

    private NetworkMessage changePassword(ClientConnection client, NetworkMessage request) {
        String username = requireAuthenticatedUsername(client, request);
        String oldHash = required(request, "oldPasswordHash", "old password is required");
        String newHash = required(request, "newPasswordHash", "new password is required");
        if (!ServerCrypto.looksLikeSha256(newHash)) throw new IllegalArgumentException("new password hash must be SHA-256 hex");

        ServerAccount account = requireAccount(username);
        if (!account.verifyPasswordHash(oldHash)) throw new IllegalArgumentException("old password is incorrect");
        if (account.verifyPasswordHash(newHash)) throw new IllegalArgumentException("new password must be different from current password");
        account.setPasswordHash(newHash);
        account.clearPersistentToken();
        account.applyPasswordChangeProfile(request);
        repository.save(account);
        return RequestDispatcher.success(request, "password changed successfully");
    }

    private NetworkMessage resetLookup(ClientConnection client, NetworkMessage request) {
        String username = required(request, "username", "username is required");
        String email = required(request, "email", "email is required");
        ServerAccount account = repository.find(username);
        if (account == null || account.getEmail() == null || !account.getEmail().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("username and email do not match an account");
        }
        String resetId = resets.create(account.getUsername());
        return RequestDispatcher.success(request, "security question loaded")
                .put("resetId", resetId)
                .put("securityQuestion", account.getSecurityQuestion());
    }

    private NetworkMessage resetVerify(ClientConnection client, NetworkMessage request) {
        String resetId = required(request, "resetId", "reset id is required");
        String answerHash = required(request, "securityAnswerHash", "security answer is required");
        String username = resets.usernameForVerification(resetId);
        if (username == null) throw new IllegalArgumentException("password reset request is invalid or expired");
        ServerAccount account = requireAccount(username);
        boolean matches = account.verifySecurityAnswerHash(answerHash);
        resets.markAttempt(resetId, matches);
        if (!matches) throw new IllegalArgumentException("security answer is incorrect");
        return RequestDispatcher.success(request, "security answer accepted");
    }

    private NetworkMessage resetCommit(ClientConnection client, NetworkMessage request) {
        String resetId = required(request, "resetId", "reset id is required");
        String newHash = required(request, "newPasswordHash", "new password is required");
        if (!ServerCrypto.looksLikeSha256(newHash)) throw new IllegalArgumentException("new password hash must be SHA-256 hex");
        String username = resets.consumeVerified(resetId);
        if (username == null) throw new IllegalArgumentException("security answer must be verified before resetting password");
        ServerAccount account = requireAccount(username);
        account.setPasswordHash(newHash);
        account.clearPersistentToken();
        repository.save(account);
        ClientConnection activeConnection = sessions.getConnection(username);
        if (activeConnection != null) {
            server.getMatchmakingService().sessionEnded(username, activeConnection, "session was revoked");
        }
        sessions.invalidateUser(username, "password was reset; please sign in again");
        return RequestDispatcher.success(request, "password reset completed");
    }

    private String requireAuthenticatedUsername(ClientConnection client, NetworkMessage request) {
        String username = sessions.authenticate(client, request.getSessionToken());
        if (username == null) throw new IllegalArgumentException("authentication required or session expired");
        return username;
    }

    private ServerAccount requireAccount(String username) {
        ServerAccount account = repository.find(username);
        if (account == null) throw new IllegalArgumentException("account no longer exists");
        return account;
    }

    private NetworkMessage authenticatedResponse(NetworkMessage request, ServerAccount account,
                                                 String sessionToken, String message) {
        return RequestDispatcher.success(request, message)
                .put("sessionToken", sessionToken)
                .put("username", account.getUsername())
                .put("nickname", account.getNickname())
                .put("email", account.getEmail())
                .put("coins", account.getCoins())
                .put("gems", account.getGems())
                .put("totalGamesPlayed", account.getTotalGamesPlayed())
                .binaryPayload(account.getProfilePayload());
    }

    private static String required(NetworkMessage request, String key, String message) {
        String value = request == null ? null : request.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    private static void validateUsername(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is required");
        String trimmed = username.trim();
        if (trimmed.length() > 128) throw new IllegalArgumentException("username is too long");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z')
                    && !(c >= '0' && c <= '9') && c != '-') {
                throw new IllegalArgumentException("username contains invalid characters");
            }
        }
    }
}
