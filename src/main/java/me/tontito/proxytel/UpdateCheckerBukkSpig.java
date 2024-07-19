package me.tontito.proxytel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateCheckerBukkSpig extends BukkitRunnable {

    private final ProxyTel main;
    private final String projectName;

    public UpdateCheckerBukkSpig(ProxyTel main) {
        this.main = main;
        projectName = main.getDescription().getName();

        runTaskLaterAsynchronously(main, 20);
    }

    public void run() {
        try {
            StringBuilder page = makeAsyncGetRequest("https://cld.pt/dl/download/51c19f75-8900-49f2-8e1b-a92256bf2d4a/bukkit.txt?download=true/");

            if (page != null && page.length() > 10) {
                String pagina = page.toString();
                int pointer = pagina.indexOf("project-file-name-container-" + projectName);
                pagina = pagina.substring(pointer); //smaller data

                String tmp = pagina.substring(pagina.indexOf("https://cdn.modrinth.com/"));
                String version = tmp.substring(tmp.indexOf("data-name=\"") + 11).split("\"")[0];
                String url = tmp.split("\"")[0];

                promptUpdate(version, url);
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("[" + projectName + "] " + ChatColor.RED + "Connection exception: " + e.getMessage());
        }
    }

    private StringBuilder makeAsyncGetRequest(String url) {
        StringBuilder response = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    response.append(line);
                }
            }
        } catch (Exception ex) {
        }
        return response;
    }

    private void promptUpdate(String serverVersion, String Url) {

        if (serverVersion == null) {
            Bukkit.getConsoleSender().sendMessage("[" + projectName + "] " + ChatColor.RED + "Unknown error checking version");
            return;
        }

        String tmpServerVersion = null;
        if (serverVersion.split(" v").length > 1) tmpServerVersion = serverVersion.split(" v")[1];
        if (tmpServerVersion == null) tmpServerVersion = serverVersion.split(" ")[1];
        serverVersion = tmpServerVersion;

        String currentVersion = main.getDescription().getVersion();
        int versionStatus = Utils.checkGreater(serverVersion, currentVersion);

        if (versionStatus == -1) {
            Bukkit.getConsoleSender().sendMessage("[" + projectName + "] " + ChatColor.GREEN + "THERE IS A NEW UPDATE AVAILABLE Version: " + serverVersion +
                    " Download it from here: " + Url);

        } else if (versionStatus == 0) {
            Bukkit.getConsoleSender().sendMessage("[" + projectName + "] " + ChatColor.DARK_GREEN + "You have the latest released version");
        } else if (versionStatus == 1) {
            Bukkit.getConsoleSender().sendMessage("[" + projectName + "] " + ChatColor.YELLOW + "Congrats, you are testing a new version!");
        } else {
            Bukkit.getConsoleSender().sendMessage("[" + projectName + "] " + ChatColor.RED + "Unknown error checking version (" + versionStatus + ")" + serverVersion + "   " + currentVersion);
        }

    }

}