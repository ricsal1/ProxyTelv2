package me.tontito.proxytel;

import java.io.*;
import java.net.Socket;
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
    ProxyReceiver p1;
    DataOutputStream out;
    DataInputStream in;
    public Socket c;
    Thread t;
    Integer NewPort = 0;
    ProxyTel main;
    public String address;
    private long contaBytes = 0;
    private long contaLatencia = 0; //saves in nanosegundos
    private long contaDuracaoLigacao = System.currentTimeMillis(); //saves in millis
    private String MinecraftServer;
    private boolean hasCompression = false;
    private Deflater compress;
    private Inflater decompress;

    //receives from client
    public ProxyReceiver(Socket c, String MinecraftServer, Integer NewPort, ProxyTel main) throws IOException {
        this.NewPort = NewPort;
        this.MinecraftServer = MinecraftServer;
        this.main = main;
        this.c = c;
        address = c.getInetAddress().getHostAddress();
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

        while (t != null) {
            try {

                if (NewPort == 0) { //received data can be aggregated

                    if (check <= 64) {
                        sleep(4);
                    } else if (check <= 512) {
                        sleep(2);
                    }
                    //greater then 512 doesn't wait
                }

                check = in.read(data);
                time = System.nanoTime();

                if (check > 0) {
                    array = new byte[check];
                    System.arraycopy(data, 0, array, 0, check);

                    contaBytes = contaBytes + check;

                    if (p1 == null) { //for first thread

                        if (main.ProtectListen) {

                            if (!hasCompression && (array[array[0]] == 31 && array[array[1]] == 139 && array[array[2]] == 8 && array[array[3]] == 8)) {
                                main.getLogger().info("Received new compressed connection from " + address);
                                compress = new Deflater();
                                decompress = new Inflater();
                                hasCompression = true;
                            }

                            if (hasCompression) {
                                array = decompressData(array);
                            }

                            if (array[1] == 0) {

                                if ((array[0] + 1) <= array.length && (array[array[0]] == 1 || array[array[0]] == 2)) {
                                    p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);
                                } else {
                                    if (main.echoLogging)
                                        main.getLogger().info(address + " start connection malformed");

                                    main.logToFile("ProxyTel", "#### " + address + " start connection malformed");
                                    main.logToFile("ProxyTel", array);
                                }

                            } else if (array[0] == -2 && array[1] == 1 && array[2] == -6) { //legacy
                                p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);
                            } else {
                                //ataque ou inapropriado
                                if (main.echoLogging)
                                    main.getLogger().info(address + " connection malformed " + array[2]);

                                main.logToFile("ProxyTel", "#### " + address + " connection malformed " + array[2]);
                                main.logToFile("ProxyTel", array);

                                try {
                                    in.close();
                                    out.close();
                                    c.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            p1 = new ProxyReceiver(MinecraftServer, NewPort, this, main);
                        }
                    }

                    p1.sendData(array);

                } else if (check < 0) {

                    try {
                        if (p1 != null) {
                            p1.Dispose();
                            p1 = null;
                        }
                    } catch (Exception e) {
                        main.logToFile("ProxyTel", "EIe: " + e.getMessage());
                    }

                    if (t != null) {
                        Dispose();
                    }

                    return;
                }

            } catch (Exception e) {
                try {
                    if (t != null) {
                        Dispose();
                    }

                    if (p1 != null) {
                        main.logToFile("ProxyTel", "Closing connection to " + p1.address + ", after reading " + contaBytes + " bytes, with active time " + ((System.currentTimeMillis() - contaDuracaoLigacao) / 1000) + "s, and total processing time " + (contaLatencia / 1000000000) + "s");

                        p1.Dispose();
                        p1 = null;
                    }
                } catch (Exception ei) {
                    main.logToFile("ProxyTel", "EI: " + ei.getMessage());

                    if (main.echoLogging)
                        main.getLogger().info("EI: " + ei.getMessage());
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
        } catch (Exception e) {
        }
    }


    public void Dispose() {
        if (t == null) {
            return;
        }

        t = null;
        main.DeleteIpFromSocket(c.getInetAddress().getHostAddress() + ":" + c.getLocalPort());

        if (p1 != null) {
            main.logToFile("ProxyTel", "Closed connection to " + p1.address + ", after reading " + contaBytes + " bytes, with active time " + ((System.currentTimeMillis() - contaDuracaoLigacao) / 1000) + "s, and total processing time " + (contaLatencia / 1000000000) + "s");
        }

        try {
            in.close();
            out.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
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

}
