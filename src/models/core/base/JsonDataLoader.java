package models.core.base;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON reader used for static game data. It intentionally has no
 * third-party dependency so the project can still be compiled with plain javac.
 */
public final class JsonDataLoader {
    private JsonDataLoader() {
    }

    public static Map<String, Object> loadObject(String resourcePath) {
        Object value = new Parser(readText(resourcePath)).parse();
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalStateException("JSON root must be an object: " + resourcePath);
        }
        return castObject(map);
    }

    public static List<Object> getArray(Map<String, Object> object, String key) {
        Object value = object == null ? null : object.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return new ArrayList<>(list);
    }

    public static Map<String, Object> asObject(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return castObject(map);
    }

    public static String getString(Map<String, Object> object, String key, String fallback) {
        Object value = object == null ? null : object.get(key);
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    public static int getInt(Map<String, Object> object, String key, int fallback) {
        Object value = object == null ? null : object.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static double getDouble(Map<String, Object> object, String key, double fallback) {
        Object value = object == null ? null : object.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static List<String> getStringList(Map<String, Object> object, String key) {
        Object value = object == null ? null : object.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private static Map<String, Object> castObject(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static String readText(String resourcePath) {
        String cleaned = resourcePath == null ? "" : resourcePath.replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        ClassLoader loader = JsonDataLoader.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(cleaned)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read JSON resource: " + cleaned, exception);
        }

        String configuredDirectory = System.getProperty("pvz.data.dir", "").trim();
        List<Path> candidates = new ArrayList<>();
        if (!configuredDirectory.isEmpty()) {
            candidates.add(Path.of(configuredDirectory, Path.of(cleaned).getFileName().toString()));
        }
        candidates.add(Path.of(cleaned));
        candidates.add(Path.of("src", cleaned));
        candidates.add(Path.of("..", "src", cleaned));
        candidates.add(Path.of(System.getProperty("user.dir", "."), cleaned));
        candidates.add(Path.of(System.getProperty("user.dir", "."), "src", cleaned));

        for (Path candidate : candidates) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                return Files.readString(candidate, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read JSON file: " + candidate, exception);
            }
        }

        throw new IllegalStateException(
                "Game data file was not found: " + cleaned
                        + ". Keep the data directory beside the source tree or set -Dpvz.data.dir=<path>."
        );
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != source.length()) {
                throw error("Unexpected trailing content");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= source.length()) {
                throw error("Unexpected end of JSON");
            }
            char current = source.charAt(index);
            if (current == '{') {
                return parseObject();
            }
            if (current == '[') {
                return parseArray();
            }
            if (current == '"') {
                return parseString();
            }
            if (current == 't') {
                expect("true");
                return Boolean.TRUE;
            }
            if (current == 'f') {
                expect("false");
                return Boolean.FALSE;
            }
            if (current == 'n') {
                expect("null");
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < source.length()) {
                char current = source.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current != '\\') {
                    builder.append(current);
                    continue;
                }
                if (index >= source.length()) {
                    throw error("Invalid string escape");
                }
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> builder.append(parseUnicode());
                    default -> throw error("Unknown escape sequence: \\" + escaped);
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > source.length()) {
                throw error("Invalid unicode escape");
            }
            String digits = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(digits, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid unicode escape: " + digits);
            }
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                    index++;
                }
                while (index < source.length() && Character.isDigit(source.charAt(index))) {
                    index++;
                }
            }
            String token = source.substring(start, index);
            try {
                return decimal ? Double.parseDouble(token) : Long.parseLong(token);
            } catch (NumberFormatException exception) {
                throw error("Invalid number: " + token);
            }
        }

        private void expect(String token) {
            if (!source.startsWith(token, index)) {
                throw error("Expected " + token);
            }
            index += token.length();
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= source.length() || source.charAt(index) != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < source.length() && source.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
        }

        private IllegalStateException error(String message) {
            return new IllegalStateException(message + " at JSON index " + index);
        }
    }
}
