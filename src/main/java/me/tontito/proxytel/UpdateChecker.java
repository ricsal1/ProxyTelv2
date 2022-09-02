package me.tontito.proxytel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private final ProxyTel main;
    private final String project;
    private final String projectName;

    public UpdateChecker(ProxyTel main) {
        this.main = main;
        projectName = main.getDescription().getName();
        this.project = main.getDescription().getName().toLowerCase();

        try {
            StringBuilder page = makeAsyncGetRequest("https://dev.bukkit.org/projects/" + project + "/");

            if (page != null && page.length() > 1000) {
                String pagina = page.toString();
                int pointer = pagina.indexOf("project-file-name-container");
                pagina = pagina.substring(pointer); //smaller data

                String versao = pagina.substring(pagina.indexOf("data-name=\"") + 11).split("\"")[0];
                String url = pagina.substring(pagina.indexOf("/projects/" + project)).split("\"")[0];

                promptUpdate(versao, url);
            }
        } catch (Exception e) {
            Bukkit.getLogger().info("[" + projectName + "] Connection exception: " + e.getMessage());

            Bukkit.getConsoleSender().sendMessage(Component
                    .text("[" + projectName + "] Connection exception: " + e.getMessage())
                    .color(NamedTextColor.RED)
            );
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
        TextComponent component;

        if (serverVersion == null) {
            component = Component.text(" Unknown error checking version");

            Bukkit.getConsoleSender().sendMessage(Component
                    .text("[" + projectName + "]")
                    .color(NamedTextColor.RED)
                    .append(component)
            );

            return;
        }

        String tmpServerVersion = null;
        if (serverVersion.split(" v").length > 1) tmpServerVersion = serverVersion.split(" v")[1];
        if (tmpServerVersion == null) tmpServerVersion = serverVersion.split(" ")[1];
        serverVersion = tmpServerVersion;

        String currentVersion = main.getDescription().getVersion();
        int versionStatus = checkGreater(serverVersion, currentVersion);
        NamedTextColor color = NamedTextColor.GRAY;

        if (versionStatus == -1) {
            component = Component.text(" THERE IS A NEW UPDATE AVAILABLE Version: " + serverVersion +
                    " Download it from here: https://dev.bukkit.org" + Url,NamedTextColor.GREEN);
        } else if (versionStatus == 0) {
            component = Component.text(" You have the latest released version",NamedTextColor.GREEN );
        } else if (versionStatus == 1) {
            component = Component.text(" Congrats, you are testing a new version!",NamedTextColor.YELLOW);
        } else {
            component = Component.text(" Unknown error checking version (" + versionStatus + ")" + serverVersion + "   " + currentVersion,NamedTextColor.RED);
        }

        Bukkit.getConsoleSender().sendMessage(Component
                .text("[" + projectName + "]")
                .color(color)
                .append(component)
        );
    }


    private int checkGreater(String v1, String v2) {
        int counter = v1.split("\\.").length;

        if (counter > v2.split("\\.").length) v2 = v2 + ".0";
        if (counter < v2.split("\\.").length) {
            v1 = v1 + ".0";
            counter++;
        }

        for (int k = 0; k < counter; k++) {
            try {
                if (Integer.parseInt(v1.split("\\.")[k]) > Integer.parseInt(v2.split("\\.")[k])) {
                    return -1;
                } else if (Integer.parseInt(v1.split("\\.")[k]) < Integer.parseInt(v2.split("\\.")[k])) {
                    return 1;
                } else {
                    //next loop
                }
            } catch (Exception e) {
                return -2;
            }
        }
        return 0;//same version
    }

}