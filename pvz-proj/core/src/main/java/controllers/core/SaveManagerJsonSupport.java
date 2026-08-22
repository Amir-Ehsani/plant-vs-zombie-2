package controllers.core;

import models.account.Collection;
import models.account.Greenhouse;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
import models.account.User;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


abstract class SaveManagerJsonSupport {
    protected static final Path SAVE_DIRECTORY = Path.of("data");
    protected static final Path USERS_FILE = SAVE_DIRECTORY.resolve("users.json");

    protected boolean appendField(StringBuilder builder, String name, String value, boolean first) {
        if (!first) {
            builder.append(",");
        }

        builder.append("\n  ")
                .append(jsonString(name))
                .append(": ")
                .append(value);

        return false;
    }

    protected String stringListToJson(List<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                if (i > 0) {
                    builder.append(",");
                }

                builder.append(jsonString(values.get(i)));
            }
        }

        builder.append("]");
        return builder.toString();
    }

    protected String dateTimeToJson(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "null";
        }

        return jsonString(dateTime.toString());
    }

    protected String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\"");

        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            switch (character) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 32) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }

        builder.append("\"");
        return builder.toString();
    }

    protected Map<String, Object> asMap(Object object) {
        if (object instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null) {
                    map.put(entry.getKey().toString(), entry.getValue());
                }
            }

            return map;
        }

        return null;
    }


    protected Map<String, Integer> integerMap(Object object) {
        Map<String, Object> rawMap = asMap(object);
        Map<String, Integer> values = new LinkedHashMap<>();
        if (rawMap == null) {
            return values;
        }
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number number) {
                values.put(entry.getKey(), Math.max(0, number.intValue()));
            } else if (value instanceof String text) {
                try {
                    values.put(entry.getKey(), Math.max(0, Integer.parseInt(text)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return values;
    }

    protected List<?> asList(Object object) {
        if (object instanceof List<?> list) {
            return list;
        }

        return new ArrayList<>();
    }

    protected List<?> list(Map<String, Object> map, String key) {
        return asList(map.get(key));
    }

    protected List<String> stringsFromList(List<?> list) {
        List<String> strings = new ArrayList<>();

        for (Object object : list) {
            if (object != null) {
                strings.add(object.toString());
            }
        }

        return strings;
    }

    protected String string(Map<String, Object> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return "";
        }

        return value.toString();
    }

    protected int integer(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
            }
        }

        return defaultValue;
    }

    protected float decimal(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof String text) {
            try {
                return Float.parseFloat(text);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    protected boolean bool(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);

        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }

        return defaultValue;
    }

    protected LocalDateTime dateTime(Map<String, Object> map, String key) {
        String value = string(map, key);

        if (value.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    protected void setPrivateField(Object target, String fieldName, Object value) {
        if (target == null) {
            return;
        }

        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ignored) {
        }
    }

    protected static final class JsonParser {
        protected final String text;
        protected int index;

        protected JsonParser(String text) {
            this.text = text == null ? "" : text;
            this.index = 0;
        }

        protected Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            return value;
        }

        protected Object parseValue() {
            skipWhitespace();

            if (index >= text.length()) {
                return null;
            }

            char character = text.charAt(index);

            if (character == '{') {
                return parseObject();
            }

            if (character == '[') {
                return parseArray();
            }

            if (character == '"') {
                return parseString();
            }

            if (startsWith("true")) {
                index += 4;
                return true;
            }

            if (startsWith("false")) {
                index += 5;
                return false;
            }

            if (startsWith("null")) {
                index += 4;
                return null;
            }

            return parseNumber();
        }

        protected Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            index++;
            skipWhitespace();

            if (peek('}')) {
                index++;
                return map;
            }

            while (index < text.length()) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    break;
                }

                expect(',');
            }

            return map;
        }

        protected List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            index++;
            skipWhitespace();

            if (peek(']')) {
                index++;
                return list;
            }

            while (index < text.length()) {
                list.add(parseValue());
                skipWhitespace();

                if (peek(']')) {
                    index++;
                    break;
                }

                expect(',');
            }

            return list;
        }

        protected String parseString() {
            StringBuilder builder = new StringBuilder();
            expect('"');

            while (index < text.length()) {
                char character = text.charAt(index++);

                if (character == '"') {
                    break;
                }

                if (character != '\\') {
                    builder.append(character);
                    continue;
                }

                if (index >= text.length()) {
                    break;
                }

                char escaped = text.charAt(index++);

                switch (escaped) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        String hex = text.substring(index, Math.min(index + 4, text.length()));
                        index += Math.min(4, hex.length());

                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    default -> builder.append(escaped);
                }
            }

            return builder.toString();
        }

        protected Number parseNumber() {
            int start = index;

            while (index < text.length()) {
                char character = text.charAt(index);

                if ((character >= '0' && character <= '9')
                        || character == '-'
                        || character == '+'
                        || character == '.'
                        || character == 'e'
                        || character == 'E') {
                    index++;
                } else {
                    break;
                }
            }

            String number = text.substring(start, index);

            try {
                if (number.contains(".") || number.contains("e") || number.contains("E")) {
                    return Double.parseDouble(number);
                }

                return Long.parseLong(number);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        protected void expect(char expected) {
            skipWhitespace();

            if (index < text.length() && text.charAt(index) == expected) {
                index++;
            }
        }

        protected boolean peek(char expected) {
            skipWhitespace();
            return index < text.length() && text.charAt(index) == expected;
        }

        protected boolean startsWith(String value) {
            return text.startsWith(value, index);
        }

        protected void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
