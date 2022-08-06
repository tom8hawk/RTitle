package ru.siaw.personal.rtitle;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.siaw.personal.rtitle.RTitle.inst;

public final class Config {
    private static final YamlConfiguration configuration = new YamlConfiguration();

    public static void init() {
        File file = new File(inst.getDataFolder() + File.separator + "config.yml");

        if (!file.exists())
            inst.saveResource("config.yml", true);

        try {
            configuration.load(file);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static String getMessage(String path) {
        return getMessage(path, configuration);
    }

    public static String getMessage(String path, ConfigurationSection section) {
        String result = section.getString(path);
        return ChatColor.translateAlternateColorCodes('&', result != null ? result : "");
    }

    public static List<String> getList(String path) {
        return getList(path, configuration);
    }

    public static List<String> getList(String path, ConfigurationSection section) {
        return section.getStringList(path).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    public static boolean getBoolean(String path) {
        return configuration.getBoolean(path);
    }

    public static ConfigurationSection getSection(String path) {
        return configuration.getConfigurationSection(path);
    }
}
