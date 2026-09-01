package network.protocol;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/**
 * Hardened Java-serialization boundary shared by client and server.
 *
 * Depth and array length are per {@code readObject} graph, so they are safe
 * limits. {@code references()} and {@code streamBytes()} are cumulative for the
 * whole TCP stream and are never reset by {@code ObjectOutputStream.reset()}.
 * Rejecting those would drop a live match after tens of seconds of snapshots.
 */
public final class NetworkSerialization {
    private static final long MAX_ARRAY_LENGTH = 2_000_000L;
    private static final long MAX_DEPTH = 40L;

    private NetworkSerialization() { }

    public static void secure(ObjectInputStream input) throws IOException {
        if (input == null) {
            throw new IOException("object input stream is null");
        }
        input.setObjectInputFilter(NetworkSerialization::filter);
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > MAX_DEPTH
                || (info.arrayLength() >= 0 && info.arrayLength() > MAX_ARRAY_LENGTH)) {
            return ObjectInputFilter.Status.REJECTED;
        }

        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        if (type.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        if (type.isArray()) {
            Class<?> component = type;
            while (component.isArray()) {
                component = component.getComponentType();
            }
            return allowedName(component.getName())
                    || component.isPrimitive()
                    ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED;
        }

        return allowedName(type.getName())
                ? ObjectInputFilter.Status.ALLOWED
                : ObjectInputFilter.Status.REJECTED;
    }

    private static boolean allowedName(String name) {
        if (name == null) {
            return false;
        }
        if (name.startsWith("network.protocol.")) {
            return true;
        }
        if (name.equals("java.util.Map$Entry") || name.equals("java.lang.Enum") || name.equals("java.lang.Number")) {
            return true;
        }
        if (name.startsWith("java.util.")) {
            return true;
        }
        if (name.startsWith("java.lang.")
                && !name.contains("ClassLoader")
                && !name.equals("java.lang.Process")
                && !name.equals("java.lang.Runtime")) {
            return true;
        }
        return name.equals("java.io.Serializable")
                || name.equals("java.lang.Cloneable")
                || name.equals("java.lang.Comparable");
    }
}
