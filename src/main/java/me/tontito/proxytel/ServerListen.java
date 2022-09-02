package me.tontito.proxytel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

public class ServerListen extends Thread {
    private Integer MinecraftPort;
    private String MinecraftServer;
    private ServerSocket s;
    private Thread t;
    private ProxyTel main;

    //ServerListen(ListenPort, MinecraftServer, MinecraftPort, this);

    public ServerListen(Integer ListenPort, String MinecraftServer, Integer MinecraftPort, ProxyTel main) {
        try {
            this.main = main;
            this.MinecraftPort = MinecraftPort;
            this.MinecraftServer = MinecraftServer;
            startSockets(ListenPort);

            t = new Thread(this);
            t.start();

        } catch (Exception ioexception) {
            main.logToFile("ProxyTel", "Error Starting " + ioexception);
            main.getLogger().info("Error Starting " + ioexception);
        }
    }

    private void startSockets(int port) {
        try {
            s = new ServerSocket(port);
            main.getLogger().info("Listenning on " + port);
            main.logToFile("ProxyTel", "Listenning on " + port);
        } catch (IOException ioexception) {
            main.getLogger().info("Error starting sockets" + ioexception);
            main.logToFile("ProxyTel", "Error starting sockets" + ioexception);
        }
    }


    public void run() {
        try {
            main.logToFile("ProxyTel", "Waiting for connections...");
            main.getLogger().info("Waiting for connections");

            while (t != null) {
                try {
                    Socket socket1 = s.accept();
                    String local = "";

                    if (main.echoLogging && main.getLogLevel() >= 999999)
                        local = " @" + getCountry(new InetSocketAddress(socket1.getInetAddress(), socket1.getPort()));  //not allowed for plugins

                    main.logToFile("ProxyTel", "New connection from " + socket1.getInetAddress().getHostAddress() + local);

                    if (main.echoLogging)
                        main.getLogger().info("New connection from " + socket1.getInetAddress().getHostAddress() + local);

                    new ProxyReceiver(socket1, MinecraftServer, MinecraftPort, main);
                } catch (Exception ioexception) {
                    if (main.echoLogging)
                        main.getLogger().info("Error receiving connection " + ioexception);

                    main.logToFile("ProxyTel", "Error receiving connection " + ioexception);
                }
                sleep(200);
            }
        } catch (Exception ioexception) {
            if (main.echoLogging)
                main.getLogger().info("Error receiving connection22 " + ioexception);

            main.logToFile("ProxyTel", "Error receiving connection22 " + ioexception);
        }
    }


    public void Dispose() {
        main.logToFile("ProxyTel", "Stopping listenning");
        t = null;
        try {
            s.close();
        } catch (Exception ioexception) {
            if (main.echoLogging)
                main.getLogger().info("Error stopping listenning " + ioexception);

            main.logToFile("ProxyTel", "Error stopping listenning " + ioexception);
        }
    }

    public static String getCountry(String ip) throws Exception {
        return getCountry(new InetSocketAddress(ip, 0));

    }

    private static String getCountry(InetSocketAddress ip) throws Exception {
        URL url = new URL("http://ip-api.com/json/" + ip.getHostName());

        BufferedReader stream = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder entirePage = new StringBuilder();
        String inputLine;

        while ((inputLine = stream.readLine()) != null)
            entirePage.append(inputLine);

        stream.close();

        if (!(entirePage.toString().contains("\"country\":\"")))
            return null;

        return entirePage.toString().split("\"country\":\"")[1].split("\",")[0] + "-" + entirePage.toString().split("\"city\":\"")[1].split("\",")[0];
    }


}
