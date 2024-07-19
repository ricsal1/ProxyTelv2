package me.tontito.proxytel;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Created by IntelliJ IDEA.
 * User: Tontito
 * Date: 18/Jun/2006
 * Time: 12:03:29
 * To change this template use File | Settings | File Templates.
 */
public class ProxyReceiver extends Thread {
    public Socket c;
    public String address;
    ProxyReceiver p1;
    DataOutputStream out;
    DataInputStream in;
    Thread t;
    Integer NewPort = 0;
    ProxyTel main;
    private long contaPackets = 0;
    private long contaBytes = 0;
    private long contaLatencia = 0; //saves in nanosegundos
    private final long contaDuracaoLigacao = System.currentTimeMillis(); //saves in millis
    private String MinecraftServer;
    private boolean hasCompression = false;
    private Deflater compress;
    private Inflater decompress;
    private ServerListen.ConnectionCounter checkIp;
    public String userLogin;
    private boolean login;

    //receives from client
    public ProxyReceiver(Socket c, String MinecraftServer, Integer NewPort, ProxyTel main) throws IOException {
        this.NewPort = NewPort;
        this.MinecraftServer = MinecraftServer;
        this.main = main;
        this.c = c;
        address = c.getInetAddress().getHostAddress();
        checkIp = main.accessControlHash.get(address);

        OutputStream outs = c.getOutputStream();
        InputStream ins = c.getInputStream();
        out = new DataOutputStream(outs);
        in = new DataInputStream(ins);

        t = new Thread(this);
        t.start();
    }

    //receives from server
    public ProxyReceiver(String host, Integer port, ProxyReceiver p1, ProxyTel main) throws IOException {
        this.main = main;
        c = new Socket(host, port);

        main.RegisterIpFromSocket(p1.c.getInetAddress().getHostAddress(), c.getInetAddress().getHostAddress() + ":" + c.getLocalPort());

        address = "Server";
        this.p1 = p1;
        InputStream ins = c.getInputStream();
        OutputStream outs = c.getOutputStream();
        out = new DataOutputStream(outs);
        in = new DataInputStream(ins);

        t = new Thread(this);
        t.start();
    }


    public void run() {

        byte[] data = new byte[16 * 1024];
        byte[] array;
        Integer check = 0;
        Long time;

        //small pause to assure data read
        try {
            sleep(1);
        } catch (Exception e) {
        }

        while (t != null) {
            try {

                if (false) { //received data can be aggregated (only if server is too fast and data gets out with no agregation)

                    if (check <= 32) {
                        sleep(2);
                    } else if (check <= 256) {
                        sleep(1);
                    }
                    //greater then 512 doesn't wait
                }

                check = in.read(data);
                time = System.nanoTime();
                contaPackets++;

                if (check > 0) {
                    array = new byte[check];
                    System.arraycopy(data, 0, array, 0, check);

                    contaBytes = contaBytes + check;

                    if (p1 == null) { //for first thread

//                        main.getLogger().info(data[0] + "  " + data[1] + "  " + data[2] + "  " + data[3] + "  " + data[4] + "  " + data[5]);
//                        main.getLogger().info((data[0] & 0xff) + "  " + (data[1] & 0xff) + "   " + (data[2] & 0xff) + "  " + (data[3] & 0xff) + "  " + (data[4] & 0xff) + "  " + (data[5] & 0xff));
//                        main.getLogger().info(new String(array, StandardCharsets.UTF_8));

                        String message = "";

                        if (main.ProtectListen) {

                            if (!hasCompression && (array[array[0] & 0xff] == 31 && array[array[1] & 0xff] == 139 && array[array[2] & 0xff] == 8 && array[array[3] & 0xff] == 8)) {
                                message = " compression";
                                compress = new Deflater();
                                decompress = new Inflater();
                                hasCompression = true;
                            }

                            if (hasCompression) {
                                array = decompressData(array);
                            }

                            //handshake
                            if (array[1] == 0) {

                                if ((array[0] + 1) <= array.length && (array[array[0] & 0xff] == 1 || array[array[0] & 0xff] == 2)) {
                                    message = " request " + (array[array[0]] == 1 ? "status" : "login");
                                    p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);

                                    if (array[array[0]] == 2) login = true;

                                    if (login && (array.length - (array[0] + 1)) > 10) {

                                        byte[] array1 = new byte[check];
                                        System.arraycopy(array, (array[0] + 1), array1, 0, (array.length - (array[0] + 1)));

                                        if ((array1[0] + 1) <= array1.length && array1[1] == 0) {

                                            int len = array1[2];
                                            userLogin = new String(array1).substring(3, 3 + len);
                                            p1.userLogin = userLogin;

                                            main.logToFile("ProxyTel", " -------> " + userLogin);
                                        }

                                        login = false;
                                    }

                                } else {
                                    message = " with start connection malformed";

                                    if (!address.equals("Server")) main.logToFile("RAW_ProxyTel_" + address, array);

                                    if (p1 != null) incAttempts(p1.address, false, 2, contaBytes);

                                    return;
                                }

                                //tcpshield
                            } else if (array[1] == 1 && array[2] == 0 && (array[0] + 1) <= array.length) {
                                message = " TCPSHIELD";
                                p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);

                            } else if (array[0] == -2 && array[1] == 1 && array[2] == -6) { //legacy
                                message = " legacy connection";
                                p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);

                            } else {
                                message = " with connection malformed " + array[2];
                                if (!address.equals("Server")) main.logToFile("RAW_ProxyTel_" + address, array);

                                if (p1 != null) incAttempts(p1.address, false, 2, contaBytes);

                                try {
                                    in.close();
                                    out.close();
                                    c.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return;
                            }
                        } else {
                            p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);
                        }

                        String local = "";

                        if (hasCompression) local = " " + (char) 169;

                        if (main.getLogLevel() >= 999999 && checkIp.checkCount() < 6)
                            local = local + " @" + getCountry(new InetSocketAddress(c.getInetAddress(), c.getPort()));  //not allowed for plugins


                        main.logToFile("ProxyTel", "Connection from " + address + local + message);

                        if (main.echoLogging)
                            main.getLogger().info("Connection from " + address + local + message);

                    } else {

                        if (login && array.length > 0) {

                            if ((array[0] + 1) <= array.length && array[1] == 0) {
                                int len = array[2];
                                userLogin = new String(array).substring(3, 3 + len);
                                p1.userLogin = userLogin;

                                main.logToFile("ProxyTel", " -------> " + userLogin);
                            }
                        }
                        login = false;
                    }

                    if (p1 != null) p1.sendData(array);

                } else if (check < 0) {   //if request to close

                    if (p1 != null) {
                        p1.Dispose(true);
                        p1 = null;
                    }

                    if (t != null) Dispose(false);

                    return;
                }

            } catch (Exception e) {

                if (t != null) Dispose(false);

                if (p1 != null) {
                    main.logToFile("ProxyTel", "Closing connection to " + p1.address + ", after reading " + contaBytes + " bytes, with active time " + ((System.currentTimeMillis() - contaDuracaoLigacao) / 1000) + "s, and total processing time " + (contaLatencia / 1000000) + "ms  for " + contaPackets + " packets");

                    p1.Dispose(false);
                    p1 = null;
                }

                return;
            }

            contaLatencia = contaLatencia + (System.nanoTime() - time);
        }

    }


    public void sendData(byte[] bytes) {
        try {
            if (hasCompression) {
                bytes = compressData(bytes);
            }

            out.write(bytes);
            out.flush();
        } catch (Exception e) {
        }
    }


    public void Dispose(boolean requested) {
        try {
            if (t == null) return;

            t = null;
            main.DeleteIpFromSocket(c.getInetAddress().getHostAddress() + ":" + c.getLocalPort());

            if (p1 != null) {
                //if termination started from server
                if (p1.address != null && p1.address.equals("Server")) requested = false;

                main.logToFile("ProxyTel", "Closed connection to " + address + " from " + p1.address + ", after reading " + contaBytes + " bytes, with active time " + ((System.currentTimeMillis() - contaDuracaoLigacao) / 1000) + "s, and total processing time " + (contaLatencia / 1000000) + "ms for " + contaPackets + " packets");

                //inc count because not many bytes transfered
                if (p1.address != null && !p1.address.equals("Server")) {
                    incAttempts(p1.address, requested, 1, contaBytes);
                }
            }

            try {
                in.close();
                out.close();
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            main.logToFile("ProxyTel", "EIe: " + e.getMessage());
        }
    }


    public byte[] decompressData(byte[] input) {
        try {
            decompress.setInput(input);
            byte[] fresult = new byte[input.length + (8 * 1024)];

            int resLength = decompress.inflate(fresult);

            byte[] array = new byte[resLength];
            System.arraycopy(fresult, 0, array, 0, resLength);

            return array;
        } catch (Exception e) {
            return null;
        }
    }


    public byte[] compressData(byte[] input) {

        try {
            byte[] compressedData = new byte[input.length + 1000];
            compress.setInput(input);
            compress.finish();
            int compressLength = compress.deflate(compressedData, 0, compressedData.length);

            byte[] array = new byte[compressLength];
            System.arraycopy(compressedData, 0, array, 0, compressLength);

            return array;
        } catch (Exception e) {
            return null;
        }
    }


    private void incAttempts(String currentIp, boolean allowinc, int inc, long contaBytes) {
        ServerListen.ConnectionCounter checkIp = main.accessControlHash.get(currentIp);

        if (checkIp != null) {
            long duration = (System.currentTimeMillis() - contaDuracaoLigacao);
            checkIp.dec(allowinc, inc, contaBytes, duration, userLogin);
        }
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
