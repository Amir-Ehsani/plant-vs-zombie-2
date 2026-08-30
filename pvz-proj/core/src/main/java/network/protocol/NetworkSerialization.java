package network.protocol;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/** Hardened Java-serialization boundary shared by client and the future server. */
public final class NetworkSerialization {
    private static final long MAX_STREAM_BYTES = 16L * 1024L * 1024L;
    private static final long MAX_REFERENCES = 50_000L;
    private static final long MAX_ARRAY_LENGTH = 2_000_000L;
    private static final long MAX_DEPTH = 40L;

    private NetworkSerialization() { }

    public static void secure(ObjectInputStream input) throws IOException {
        if (input == null) throw new IOException("object input stream is null");
        input.setObjectInputFilter(NetworkSerialization::filter);
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > MAX_DEPTH
                || info.references() > MAX_REFERENCES
                || info.streamBytes() > MAX_STREAM_BYTES
                || (info.arrayLength() >= 0 && info.arrayLength() > MAX_ARRAY_LENGTH)) {
            return ObjectInputFilter.Status.REJECTED;
        }

        Class<?> type = info.serialClass();
        if (type == null) return ObjectInputFilter.Status.UNDECIDED;
        if (type.isPrimitive()) return ObjectInputFilter.Status.ALLOWED;
        if (type.isArray()) {
            Class<?> component = type;
            while (component.isArray()) component = component.getComponentType();
            if (component.isPrimitive() || component == String.class || component == Object.class
                    || component.getName().equals("java.util.Map$Entry")
                    || component.getName().startsWith("network.protocol.")) {
                return ObjectInputFilter.Status.ALLOWED;
            }
            return ObjectInputFilter.Status.REJECTED;
        }

        String name = type.getName();
        if (name.startsWith("network.protocol.")) return ObjectInputFilter.Status.ALLOWED;

        // Exact JDK types used by NetworkMessage's String map/list payloads.
        if (type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Boolean.class
                || type == Double.class
                || type == Float.class
                || type == Short.class
                || type == Byte.class
                || type == Character.class
                || name.equals("java.lang.Enum")
                || name.equals("java.lang.Number")
                || name.equals("java.util.ArrayList")
                || name.equals("java.util.LinkedHashMap")
                || name.equals("java.util.HashMap")) {
            return ObjectInputFilter.Status.ALLOWED;
        }

        return ObjectInputFilter.Status.REJECTED;
    }
}
