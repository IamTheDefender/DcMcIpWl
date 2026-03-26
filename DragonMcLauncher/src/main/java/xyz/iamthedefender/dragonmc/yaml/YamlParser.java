package xyz.iamthedefender.dragonmc.yaml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YamlParser {

    public static ConfigurationSection parse(File file) throws IOException {
        return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    public static ConfigurationSection parse(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
        return parse(buf.toString(StandardCharsets.UTF_8));
    }

    public static ConfigurationSection parse(String yaml) {
        List<String> lines = new ArrayList<>(Arrays.asList(yaml.split("\n", -1)));
        ConfigurationSection root = new ConfigurationSection();
        parseMappingBlock(lines, new int[]{0}, -1, root);
        return root;
    }

    private static void parseMappingBlock(List<String> lines, int[] idx,
                                          int parentIndent, ConfigurationSection section) {
        while (idx[0] < lines.size()) {
            String raw = lines.get(idx[0]);
            String stripped = stripComment(raw);

            if (stripped.trim().isEmpty()) {
                idx[0]++;
                continue;
            }

            int indent = countIndent(stripped);

            if (indent <= parentIndent) return;

            String trimmed = stripped.trim();

            if (trimmed.startsWith("- ") || trimmed.equals("-")) {
                idx[0]++;
                continue;
            }

            int colonPos = findMappingColon(trimmed);
            if (colonPos < 0) {
                idx[0]++;
                continue;
            }

            String key = unquote(trimmed.substring(0, colonPos).trim());
            String valueRaw = trimmed.substring(colonPos + 1);
            if (!valueRaw.isEmpty() && valueRaw.charAt(0) == ' ') valueRaw = valueRaw.substring(1);

            idx[0]++;

            if (valueRaw.equals("|") || valueRaw.equals(">")) {
                boolean folded = valueRaw.equals(">");
                section.set(key, parseBlockScalar(lines, idx, indent, folded));
                continue;
            }

            if (valueRaw.startsWith("[")) {
                section.set(key, parseInlineSequence(valueRaw));
                continue;
            }

            if (valueRaw.isEmpty()) {
                int peekIdx = idx[0];
                while (peekIdx < lines.size()) {
                    String peeked = stripComment(lines.get(peekIdx));
                    if (!peeked.trim().isEmpty()) break;
                    peekIdx++;
                }
                if (peekIdx < lines.size()) {
                    String nextStripped = stripComment(lines.get(peekIdx));
                    int nextIndent = countIndent(nextStripped);
                    String nextTrimmed = nextStripped.trim();
                    if (nextIndent > indent) {
                        idx[0] = peekIdx;
                        if (nextTrimmed.startsWith("- ") || nextTrimmed.equals("-")) {
                            section.set(key, parseBlockSequence(lines, idx, indent));
                        } else {
                            ConfigurationSection child = new ConfigurationSection(key);
                            parseMappingBlock(lines, idx, indent, child);
                            section.set(key, child);
                        }
                        continue;
                    }
                }
                section.set(key, null);
                continue;
            }

            section.set(key, parseScalar(valueRaw.trim()));
        }
    }

    private static List<Object> parseBlockSequence(List<String> lines, int[] idx, int parentIndent) {
        List<Object> list = new ArrayList<>();
        while (idx[0] < lines.size()) {
            String raw = lines.get(idx[0]);
            String stripped = stripComment(raw);
            if (stripped.trim().isEmpty()) {
                idx[0]++;
                continue;
            }

            int indent = countIndent(stripped);
            if (indent <= parentIndent) break;

            String trimmed = stripped.trim();
            if (!trimmed.startsWith("-")) break;

            String afterDash = trimmed.substring(1);
            if (!afterDash.isEmpty() && afterDash.charAt(0) == ' ') afterDash = afterDash.substring(1);

            idx[0]++;

            if (afterDash.trim().isEmpty()) {
                ConfigurationSection item = new ConfigurationSection();
                parseMappingBlock(lines, idx, indent, item);
                list.add(item);
            } else if (afterDash.startsWith("{")) {
                list.add(parseInlineMapping(afterDash));
            } else {
                list.add(parseScalar(afterDash.trim()));
            }
        }
        return list;
    }

    private static String parseBlockScalar(List<String> lines, int[] idx, int parentIndent, boolean folded) {
        StringBuilder sb = new StringBuilder();
        int scalarIndent = -1;
        while (idx[0] < lines.size()) {
            String raw = lines.get(idx[0]);
            if (raw.trim().isEmpty()) {
                sb.append(folded ? " " : "\n");
                idx[0]++;
                continue;
            }
            int indent = countIndent(raw);
            if (scalarIndent < 0) scalarIndent = indent;
            if (indent <= parentIndent && scalarIndent > parentIndent) break;
            if (indent < scalarIndent) break;
            if (sb.length() > 0 && !folded) sb.append("\n");
            sb.append(raw.substring(Math.min(scalarIndent, raw.length())));
            idx[0]++;
        }
        return sb.toString().stripTrailing();
    }

    private static List<Object> parseInlineSequence(String raw) {
        List<Object> list = new ArrayList<>();
        String inner = raw.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        for (String part : splitInlineItems(inner)) {
            list.add(parseScalar(part.trim()));
        }
        return list;
    }

    private static ConfigurationSection parseInlineMapping(String raw) {
        ConfigurationSection s = new ConfigurationSection();
        String inner = raw.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);
        for (String part : splitInlineItems(inner)) {
            int col = findMappingColon(part.trim());
            if (col >= 0) {
                String k = unquote(part.trim().substring(0, col).trim());
                String v = part.trim().substring(col + 1).trim();
                s.set(k, parseScalar(v));
            }
        }
        return s;
    }

    static Object parseScalar(String s) {
        if (s == null || s.isEmpty() || s.equals("~") || s.equalsIgnoreCase("null")) return null;

        if ((s.startsWith("\"") && s.endsWith("\"")) ||
                (s.startsWith("'") && s.endsWith("'"))) {
            return unquote(s);
        }

        switch (s.toLowerCase()) {
            case "true":
            case "yes":
            case "on":
                return Boolean.TRUE;
            case "false":
            case "no":
            case "off":
                return Boolean.FALSE;
        }

        if (s.matches("-?\\d+")) {
            long l = Long.parseLong(s);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
            return l;
        }
        if (s.matches("-?\\d*\\.\\d+([eE][+-]?\\d+)?") ||
                s.matches("-?\\d+[eE][+-]?\\d+")) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }

        return s;
    }

    private static int countIndent(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
        return i;
    }

    private static String stripComment(String line) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == '#' && !inSingle && !inDouble && (i == 0 || line.charAt(i - 1) == ' ')) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int findMappingColon(String s) {
        boolean inSingle = false, inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (c == ':' && !inSingle && !inDouble) {
                if (i + 1 >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t') {
                    return i;
                }
            }
        }
        return -1;
    }

    static String unquote(String s) {
        if (s.length() >= 2) {
            if ((s.startsWith("\"") && s.endsWith("\"")) ||
                    (s.startsWith("'") && s.endsWith("'"))) {
                String inner = s.substring(1, s.length() - 1);
                if (s.startsWith("\"")) {
                    inner = inner.replace("\\n", "\n")
                            .replace("\\t", "\t")
                            .replace("\\r", "\r")
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                } else {
                    inner = inner.replace("''", "'");
                }
                return inner;
            }
        }
        return s;
    }

    private static List<String> splitInlineItems(String s) {
        List<String> items = new ArrayList<>();
        int depth = 0;
        boolean inSingle = false, inDouble = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inDouble) inSingle = !inSingle;
            else if (c == '"' && !inSingle) inDouble = !inDouble;
            else if (!inSingle && !inDouble) {
                if (c == '[' || c == '{') depth++;
                else if (c == ']' || c == '}') depth--;
                else if (c == ',' && depth == 0) {
                    items.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) items.add(current.toString());
        return items;
    }
}