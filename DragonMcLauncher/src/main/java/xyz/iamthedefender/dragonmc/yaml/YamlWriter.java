package xyz.iamthedefender.dragonmc.yaml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class YamlWriter {

    private final int indentSize;
    private final String headerComment;

    public YamlWriter() {
        this(2, null);
    }

    public YamlWriter(int indentSize) {
        this(indentSize, null);
    }

    public YamlWriter(int indentSize, String headerComment) {
        if (indentSize < 1) throw new IllegalArgumentException("indentSize must be >= 1");
        this.indentSize = indentSize;
        this.headerComment = headerComment;
    }

    public String write(ConfigurationSection section) {
        StringBuilder sb = new StringBuilder();
        if (headerComment != null && !headerComment.isEmpty()) {
            for (String line : headerComment.split("\n", -1)) {
                sb.append("# ").append(line).append("\n");
            }
            sb.append("\n");
        }
        writeSection(sb, section.getRawMap(), 0);
        return sb.toString();
    }

    public void write(ConfigurationSection section, File file) throws IOException {
        Files.write(file.toPath(), write(section).getBytes(StandardCharsets.UTF_8));
    }

    public void write(ConfigurationSection section, OutputStream out) throws IOException {
        out.write(write(section).getBytes(StandardCharsets.UTF_8));
    }

    // ──────────────────────────── INTERNALS ─────────────────────────────── //

    @SuppressWarnings("unchecked")
    private void writeSection(StringBuilder sb, Map<String, Object> map, int depth) {
        String indent = repeat(depth);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = quoteKey(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof ConfigurationSection) {
                sb.append(indent).append(key).append(":\n");
                writeSection(sb, ((ConfigurationSection) value).getRawMap(), depth + 1);

            } else if (value instanceof Map) {
                sb.append(indent).append(key).append(":\n");
                writeSection(sb, (Map<String, Object>) value, depth + 1);

            } else if (value instanceof List) {
                sb.append(indent).append(key).append(":\n");
                writeList(sb, (List<?>) value, depth + 1);

            } else if (value == null) {
                sb.append(indent).append(key).append(": ~\n");

            } else {
                sb.append(indent).append(key).append(": ").append(scalarToString(value)).append("\n");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writeList(StringBuilder sb, List<?> list, int depth) {
        String indent = repeat(depth);
        for (Object item : list) {
            if (item instanceof ConfigurationSection) {
                sb.append(indent).append("-\n");
                writeSection(sb, ((ConfigurationSection) item).getRawMap(), depth + 1);
            } else if (item instanceof Map) {
                sb.append(indent).append("-\n");
                writeSection(sb, (Map<String, Object>) item, depth + 1);
            } else if (item instanceof List) {
                sb.append(indent).append("- ");
                writeInlineList(sb, (List<?>) item);
                sb.append("\n");
            } else if (item == null) {
                sb.append(indent).append("- ~\n");
            } else {
                sb.append(indent).append("- ").append(scalarToString(item)).append("\n");
            }
        }
    }

    private void writeInlineList(StringBuilder sb, List<?> list) {
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(scalarToString(list.get(i)));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
    }

    // ───────────────────────── SCALAR SERIALIZATION ─────────────────────── //

    private String scalarToString(Object value) {
        if (value == null) return "~";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) return value.toString();
        if (value instanceof String) return quoteStringIfNeeded((String) value);
        return quoteStringIfNeeded(value.toString());
    }

    /**
     * Quote a string value if it contains special characters or could be
     * mistaken for another YAML type.
     */
    private String quoteStringIfNeeded(String s) {
        if (s.isEmpty()) return "\"\"";

        // Booleans, null, numbers – must be quoted so they stay strings
        switch (s.toLowerCase()) {
            case "true":
            case "false":
            case "yes":
            case "no":
            case "on":
            case "off":
            case "null":
            case "~":
                return "\"" + s + "\"";
        }
        if (s.matches("-?\\d+") || s.matches("-?\\d*\\.\\d+([eE][+-]?\\d+)?")) {
            return "\"" + s + "\"";
        }

        // Needs quoting: contains special chars, leading/trailing space, or colon-space
        if (s.startsWith(" ") || s.endsWith(" ") ||
                s.contains(": ") || s.contains(" #") || s.startsWith("#") ||
                s.contains("\n") || s.contains("\t") ||
                s.startsWith("- ") || s.startsWith("* ") || s.startsWith("& ") ||
                s.startsWith("{") || s.startsWith("[") ||
                s.startsWith("\"") || s.startsWith("'") || s.startsWith("|") ||
                s.startsWith(">") || s.startsWith("!") || s.startsWith("%") ||
                s.startsWith("@") || s.startsWith("`")) {
            return "\"" + escapeDoubleQuoted(s) + "\"";
        }

        return s;
    }

    private String quoteKey(String key) {
        // Keys need quoting if they're empty, contain colons, or start with special chars
        if (key.isEmpty() || key.contains(":") || key.contains("#") ||
                key.startsWith(" ") || key.endsWith(" ") ||
                key.startsWith("\"") || key.startsWith("'")) {
            return "\"" + escapeDoubleQuoted(key) + "\"";
        }
        return key;
    }

    private String escapeDoubleQuoted(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String repeat(int depth) {
        char[] spaces = new char[depth * indentSize];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }
}
