package ru.siaw.personal.rtitle;

import fr.mrmicky.fastboard.FastBoard;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class RTitle extends JavaPlugin {
    public static RTitle inst;
    private static boolean placeholdersSupported = false;

    private static final String SCOREBOARD_JOIN = "Scoreboard.join";
    private static final String SCOREBOARD_CONSTANT = "Scoreboard.constant";

    public RTitle() {
        inst = this;
    }

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info(() -> "Поддержка плейсхолдеров PlaceholderAPI включена!");
            placeholdersSupported = true;
        } else {
            getLogger().warning(() -> "Поддержка PlaceholderAPI отключена!");
        }

        Config.init();
        Bukkit.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onJoin(PlayerJoinEvent e) {
                Bukkit.getScheduler().runTaskLater(RTitle.inst, () -> {
                    boolean scoreboardConstantEnabled = Config.getBoolean(SCOREBOARD_CONSTANT + ".enabled");

                    if (Config.getBoolean(SCOREBOARD_JOIN + ".enabled")) {
                        String fullTime = Config.getMessage(SCOREBOARD_JOIN + ".display_time");

                        int time = Integer.parseInt(fullTime.substring(0, fullTime.length() - 1));
                        TimeUnit unit = parseUnit(fullTime);

                        FastBoard board = showScoreboard(e.getPlayer(), SCOREBOARD_JOIN);
                        Bukkit.getScheduler().runTaskLater(RTitle.inst, () -> {
                            board.delete();
                            if (scoreboardConstantEnabled)
                                showScoreboard(e.getPlayer(), SCOREBOARD_CONSTANT);
                        }, unit.toSeconds(time) * 20);
                    } else if (scoreboardConstantEnabled) {
                        showScoreboard(e.getPlayer(), SCOREBOARD_CONSTANT);
                    }

                    ConfigurationSection bossBar = Config.getSection("BossBar");

                    if (bossBar.getBoolean("enabled")) {
                        ConfigurationSection data = bossBar.getConfigurationSection("bossBars");
                        data.getKeys(false).stream()
                                .map(data::getConfigurationSection)
                                .forEach(bar -> showBossBar(e.getPlayer(), bar));
                    }
                }, 5L);
            }
        }, this);
    }

    private static FastBoard showScoreboard(Player player, String path) {
        FastBoard board = new FastBoard(player);

        String title = Config.getMessage(path + ".title");
        List<String> lines = Config.getList(path + ".text");

        board.updateTitle(adapt(player, title));
        board.updateLines(adapt(player, lines));

        AtomicInteger worker = new AtomicInteger();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(RTitle.inst, () -> {
            if (board.isDeleted()) {
                Bukkit.getScheduler().cancelTask(worker.get());
                return;
            }

            board.updateTitle(adapt(player, title));
            board.updateLines(adapt(player, lines));
        }, 20L, 20L);
        return board;
    }

    private static void showBossBar(Player player, ConfigurationSection section) {
        String title = Config.getMessage("text", section);

        BarColor color = BarColor.valueOf(section.getString("color"));
        BarStyle style = parseBarStyle(section.getInt("segments"));

        BossBar bossbar = Bukkit.createBossBar(adapt(player, title), color, style);
        bossbar.addPlayer(player);

        AtomicInteger worker = new AtomicInteger();
        Bukkit.getScheduler().scheduleSyncRepeatingTask(RTitle.inst, () -> {
            if (bossbar.getPlayers().isEmpty()) {
                Bukkit.getScheduler().cancelTask(worker.get());
                return;
            }

            bossbar.setTitle(adapt(player, title));
        }, 20L, 20L);

        if (section.getBoolean("proccess.enabled")) {
            String fullTime = section.getString("proccess.time");

            int time = Integer.parseInt(fullTime.substring(0, fullTime.length() - 1));
            TimeUnit unit = parseUnit(fullTime);

            setProgress(bossbar, (int) unit.toSeconds(time));
        } else if (!section.getBoolean("eternal")) {
            String fullTime = section.getString("display_time");

            int time = Integer.parseInt(fullTime.substring(0, fullTime.length() - 1));
            TimeUnit unit = parseUnit(fullTime);

            Bukkit.getScheduler().runTaskLater(RTitle.inst,
                    () -> bossbar.removePlayer(player), unit.toSeconds(time) * 20);
        }
    }

    private static String adapt(Player player, String line) {
        if (placeholdersSupported)
            return PlaceholderAPI.setPlaceholders(player, line);

        return line;
    }

    private static List<String> adapt(Player player, List<String> lines) {
        if (placeholdersSupported)
            return PlaceholderAPI.setPlaceholders(player, lines);

        return lines;
    }

    private static void setProgress(BossBar bossBar, int max) {
        setProgress(bossBar, 1d, 1d / max, 0, max);
    }

    private static void setProgress(BossBar bossBar, double progress, double toRemove, int time, int maxTime) {
        if (maxTime >= time) {
            bossBar.setProgress(progress);
            Bukkit.getScheduler().runTaskLater(RTitle.inst,
                    () -> setProgress(bossBar, progress - toRemove, toRemove, time + 1, maxTime), 20L);
        } else {
            bossBar.removeAll();
        }
    }

    private static TimeUnit parseUnit(String time) {
        if (time.endsWith("s"))
            return TimeUnit.SECONDS;

        if (time.endsWith("m"))
            return TimeUnit.MINUTES;

        if (time.endsWith("h"))
            return TimeUnit.HOURS;

        if (time.endsWith("d"))
            return TimeUnit.DAYS;

        throw new IllegalArgumentException("Неизвестное время: " + time);
    }

    private static BarStyle parseBarStyle(int segments) {
        if (segments == 0)
            return BarStyle.SOLID;

        return BarStyle.valueOf("SEGMENTED_" + segments);
    }

    @Override
    public void onDisable() {

    }
}
