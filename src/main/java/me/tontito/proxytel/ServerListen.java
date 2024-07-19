package me.tontito.proxytel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

public class ServerListen extends Thread {
    private Integer MinecraftPort;
    private String MinecraftServer;
    private ServerSocket s;
    private Thread t;
    private ProxyTel main;


    public ServerListen(Integer ListenPort, String MinecraftServer, Integer MinecraftPort, ProxyTel main) {
        try {
            this.main = main;
            this.MinecraftPort = MinecraftPort;
            this.MinecraftServer = MinecraftServer;

             if (startSockets(ListenPort)) {
                 t = new Thread(this);
                 t.start();
             }

        } catch (Exception ioexception) {
            main.logToFile("ProxyTel", "Error Starting " + ioexception);
            main.getLogger().info("Error Starting " + ioexception);
        }
    }


    private boolean startSockets(int port) {
        try {
            s = new ServerSocket(port);
            main.getLogger().info("Listenning on " + port);
            main.logToFile("ProxyTel", "Listenning on " + port);
            return true;
        } catch (IOException ioexception) {
            main.getLogger().info("Error starting sockets" + ioexception);
            main.logToFile("ProxyTel", "Error starting sockets" + ioexception);
            return false;
        }
    }


    public void run() {
        try {
            main.logToFile("ProxyTel", "Waiting for connections...");
            main.getLogger().info(" Waiting for connections");

            while (t != null) {
                try {
                    Socket socket1 = s.accept();
                    String currentIp = socket1.getInetAddress().getHostAddress();
                    ConnectionCounter check = main.accessControlHash.get(currentIp);
                    boolean skip = false;

                    if (check == null) {
                        check = new ConnectionCounter(currentIp);
                        main.accessControlHash.put(currentIp, check);

                    } else {

                        check.dec();

                        if (check.checkCount() >= 20) {
                            socket1.close();
                            skip = true;
                        }
                    }

                    if (!skip) {
                        new ProxyReceiver(socket1, MinecraftServer, MinecraftPort, main);
                    }

                } catch (Exception ioexception) {
                    if (main.echoLogging)
                        main.getLogger().info("Error receiving connection " + ioexception);

                    main.logToFile("ProxyTel", "Error receiving connection " + ioexception);
                }

                sleep(150);

                try {
                    Enumeration enu = main.accessControlHash.elements();

                    while (enu.hasMoreElements()) {
                        ConnectionCounter checks = (ConnectionCounter) enu.nextElement();

                        if (checks.checkDiffDate() > (180 * 1000) && checks.checkCount() < 20) {
                            checks.reset();

                        } else if (checks.checkDiffDate() > (60 * 60 * 1000) && checks.checkCount() >= 20) { //60m to unblock
                            checks.reset();
                        }
                    }
                } catch (Exception e) {
                }
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


    public class ConnectionCounter {
        private final String ip;
        private Date date;
        private int counter;
        private int activeConnection;
        private String lastUser;
        private Hashtable userLogin = new Hashtable();

        ConnectionCounter(String ip) {
            counter = 1;
            this.ip = ip;
            date = new Date();
            activeConnection = 1;
        }

        //when accepting connection
        public void dec() {
            int initCounter = counter;

            if (activeConnection > 6 && counter < 19) counter = 19;

            if (checkDiffDate() < 60000) {
                counter++;

                if (initCounter < 20 && counter >= 20) {
                    main.logToFile("ProxyTel_Access", "Now blocking spam connection from " + getIp());

                    if (main.echoLogging)
                        main.getLogger().info("Now blocking spam connection from " + getIp());
                }

            } else if (counter > 1) {
                counter--;

                if (initCounter >= 20 && counter < 20) {
                    main.logToFile("ProxyTel_Access", "Temp Unblocking " + getIp());

                    if (main.echoLogging)
                        main.getLogger().info("Temp Unblocking " + getIp());
                }
            }

            date = new Date();
            activeConnection++;
        }

        //when terminating connection
        public void dec(boolean allowinc, int value, long countBytes, long duration, String user) {
            boolean aggravation = false;
            int initCounter = counter;

            if (user != null && lastUser != null && !user.equals(lastUser) && checkDiffDate() < 60000) {
                main.logToFile("ProxyTel", user + "  vs  " + lastUser);
                aggravation = true;
            }

            if (user != null) {
                lastUser = user;
                if (userLogin.get(user) == null) userLogin.put(user, duration);
            }

            if (!aggravation && allowinc && duration >= 500 && duration < 2000 && countBytes < 30) value++;
            else if (!aggravation && allowinc && duration < 500 && countBytes < 30) value = value + 2;
            else if (aggravation && allowinc && duration < 500 && countBytes < 30) value = value + 4;
            else if (aggravation && allowinc && duration < 500) value = value + 2;
            else if (aggravation && userLogin.size() > 5) value = value + 4;

            counter = counter + value;

            if (initCounter < 20 && counter >= 20) {
                main.logToFile("ProxyTel_Access", "Now blocking spam connection from " + getIp());

                if (main.echoLogging)
                    main.getLogger().info("Now blocking spam connection from " + getIp());
            }

            main.logToFile("ProxyTel", aggravation + " Active:" + activeConnection + " inc:" + value + " bytes:" + countBytes + " dur:" + duration + " allow:" + allowinc + " final:" + counter + "   " + user + "    " + getIp());

            date = new Date();
            activeConnection--;
        }

        public void reset() {
            int initCounter = counter;
            //will not reset if having too many active connections
            main.logToFile("ProxyTel", "--> Active: " + activeConnection + "  counter:" + counter + "  from:" + ip);

            if (activeConnection >= 8) return;

            //next time will be faster
            if (counter >= 25) counter = 15;
            else if (counter < 10) counter = 0;

            date = new Date();

            if (initCounter >= 20 && counter < 20) {
                main.logToFile("ProxyTel_Access", "Unblocking " + getIp());

                if (main.echoLogging)
                    main.getLogger().info("Unblocking " + getIp());
            }

        }

        public long checkDiffDate() {
            return ((new Date()).getTime() - date.getTime());
        }

        public int checkCount() {
            return counter;
        }

        public int checkActiveConnections() {
            return activeConnection;
        }

        public String getIp() {
            return ip;
        }

    }

}



