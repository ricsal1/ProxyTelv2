package me.tontito.proxytel;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

public final class ProxyTel extends JavaPlugin {

    public final String VERSION = getDescription().getVersion();
    public boolean echoLogging;
    public int serverVersion = 0;
    boolean ProtectListen = true;
    Hashtable<String, ServerListen.ConnectionCounter> accessControlHash = new Hashtable();
    private int ListenPort;
    private int MinecraftPort;
    private String MinecraftServer;
    private int logLevel;
    private ServerListen s;
    private final Hashtable hash = new Hashtable();

    @Override
    public void onEnable() {
        String version = Bukkit.getVersion().toUpperCase();

        if (version.contains("PAPER")) {
            serverVersion = 1;
        } else if (version.contains("BUKKIT")) {
            serverVersion = 2;
        } else if (version.contains("SPIGOT")) {
            serverVersion = 3;
        } else if (version.contains("PURPUR")) {
            serverVersion = 4;
        } else if (version.contains("PUFFERFISH")) {
            serverVersion = 5;
        } else if (version.contains("-PETAL-")) {
            serverVersion = 6;
        } else if (version.contains("-SAKURA-")) {
            serverVersion = 7;
        } else if (version.contains("-FOLIA-")) {
            serverVersion = 8;
        } else {

            //server type name
            String minecraftVersion2 = Bukkit.getServer().getName();

            if (minecraftVersion2.toUpperCase().contains("FOLIA")) {
                serverVersion = 8;
            } else {
                getLogger().info("Server type not tested! " + version);
            }
        }

        if (serverVersion == 2 || serverVersion == 3) {
            new UpdateCheckerBukkSpig(this);

        } else if (serverVersion > 0)  { //make sure if identifies a version
            new UpdateChecker(this);
        }

        getLogger().info(" Loading configs!");

        setupConfig();
        s = new ServerListen(ListenPort, MinecraftServer, MinecraftPort, this);

        getLogger().info(VERSION + " enabled!");
    }

    @Override
    public void onDisable() {
        s.Dispose();
        getLogger().info("Disabled!");
    }


    private void setupConfig() {
        FileConfiguration config = getConfig();

        if (!config.contains("ListenPort")) {
            config.options().header("==== ProxyTel Config ==== #");

            config.addDefault("EchoLogging", false);
            config.addDefault("ProtectListen", true);
            config.addDefault("ListenPort", 22000);
            config.addDefault("MinecraftListen", 25566);

            config.options().copyDefaults(true);
            saveConfig();

        } else if (!config.contains("ProtectListen")) {
            config.addDefault("ProtectListen", true);
            config.addDefault("EchoLogging", false);

            config.options().copyDefaults(true);
            saveConfig();

        } else if (!config.contains("EchoLogging")) {
            config.addDefault("EchoLogging", false);

            config.options().copyDefaults(true);
            saveConfig();
        }
        setupSettings();
    }


    private void setupSettings() {
        ListenPort = getConfig().getInt("ListenPort");

        ProtectListen = getConfig().getBoolean("ProtectListen", true);
        echoLogging = getConfig().getBoolean("EchoLogging");
        logLevel = getConfig().getInt("LogLevel");

        MinecraftServer = getConfig().getString("MinecraftServer", "127.0.0.1");
        MinecraftPort = getConfig().getInt("MinecraftListen");
    }


    void logToFile(String nome, String message) {
        try {
            File dataFolder = getDataFolder();

            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Set the Time Format
            LocalDateTime now = LocalDateTime.now(); // Get the time
            File saveTo = new File(getDataFolder(), dtf.format(now) + " " + nome + ".txt");

            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); // Set the Time Format
            now = LocalDateTime.now(); // Get the time
            message = dtf.format(now) + ": " + message;

            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(message);
            pw.flush();
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    void logToFile(String nome, byte[] message) {
        try {
            File dataFolder = getDataFolder();

            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Set the Time Format
            LocalDateTime now = LocalDateTime.now(); // Get the time
            File saveTo = new File(getDataFolder(), dtf.format(now) + " " + nome + ".txt");

            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            OutputStream os = new FileOutputStream(saveTo, true);
            os.write(message);

            os.flush();
            os.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String GetIpFromSocket(String enderecoPorta) {
        try {
            return (String) hash.get(enderecoPorta);
        } catch (Exception e) {
        }

        logToFile("ProxyTel", "didnt find " + enderecoPorta);
        return null;
    }


    int getLogLevel() {
        return logLevel;
    }


    void DeleteIpFromSocket(String endereco) {
        try {
            if (hash.get(endereco) != null) {
                hash.remove(endereco);
            }
        } catch (Exception e) {
            logToFile("ProxyTel", "Missed registry removal");
        }
    }


    void RegisterIpFromSocket(String enderecoExterno, String enderecoPorta) {
        try {
            if (hash.get(enderecoPorta) == null) {
                hash.put(enderecoPorta, enderecoExterno);
            }
        } catch (Exception e) {
            logToFile("ProxyTel", "Missed registry in hash");
        }
    }


}