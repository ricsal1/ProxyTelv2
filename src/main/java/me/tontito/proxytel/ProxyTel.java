package me.tontito.proxytel;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Hashtable;

public final class ProxyTel extends JavaPlugin {

    protected boolean ProtectListen = true;
    private int ListenPort;
    private int MinecraftPort;
    private String MinecraftServer;
    public boolean echoLogging;
    private int logLevel;
    private ServerListen s;
    private Hashtable hash = new Hashtable();
    public final String VERSION = getDescription().getVersion();

    @Override
    public void onEnable() {
        new UpdateChecker(this);

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
        ProtectListen = getConfig().getBoolean("ProtectListen", true);
        ListenPort = getConfig().getInt("ListenPort");
        MinecraftPort = getConfig().getInt("MinecraftListen");
        echoLogging = getConfig().getBoolean("EchoLogging");
        logLevel = getConfig().getInt("LogLevel");

        MinecraftServer = getConfig().getString("MinecraftServer","127.0.0.1");
    }


    protected void logToFile(String nome, String message) {
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


    protected void logToFile(String nome, byte[] message) {
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


    protected int getLogLevel() {
        return logLevel;
    }


    protected void DeleteIpFromSocket(String endereco) {
        try {
            if (hash.get(endereco) != null) {
                hash.remove(endereco);
            }
        } catch (Exception e) {
            logToFile("ProxyTel", "Missed registry removal");
        }
    }


    protected void RegisterIpFromSocket(String enderecoExterno, String enderecoPorta) {
        try {
            if (hash.get(enderecoPorta) == null) {
                hash.put(enderecoPorta, enderecoExterno);
            }
        } catch (Exception e) {
            logToFile("ProxyTel", "Missed registry in hash");
        }
    }

}