package xyz.iamthedefender.dragonmc.yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class YamlConfig extends ConfigurationSection {

    private final YamlWriter writer;

    public YamlConfig() {
        this(2, null);
    }

    public YamlConfig(int indentSize, String headerComment) {
        super();
        this.writer = new YamlWriter(indentSize, headerComment);
    }

    public static YamlConfig load(File file) throws IOException {
        YamlConfig cfg = new YamlConfig();
        cfg.loadFrom(file);
        return cfg;
    }

    public static YamlConfig load(String yaml) {
        YamlConfig cfg = new YamlConfig();
        cfg.loadFrom(yaml);
        return cfg;
    }

    public static YamlConfig load(InputStream in) throws IOException {
        YamlConfig cfg = new YamlConfig();
        cfg.loadFrom(in);
        return cfg;
    }

    public void loadFrom(File file) throws IOException {
        merge(YamlParser.parse(file));
    }

    public void loadFrom(String yaml) {
        merge(YamlParser.parse(yaml));
    }

    public void loadFrom(InputStream in) throws IOException {
        merge(YamlParser.parse(in));
    }

    private void merge(ConfigurationSection other) {
        for (String key : other.getKeys()) {
            Object val = other.getRawMap().get(key);
            getRawMap().put(key, val);
        }
    }

    public void save(File file) throws IOException {
        writer.write(this, file);
    }

    public String saveToString() {
        return writer.write(this);
    }

    public void save(OutputStream out) throws IOException {
        writer.write(this, out);
    }
}
