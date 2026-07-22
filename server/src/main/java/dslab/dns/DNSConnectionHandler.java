package dslab.dns;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class DNSConnectionHandler implements Runnable {

    private final Socket socket;
    private final Map<String,String> records;
    private final Semaphore semaphore;

    public DNSConnectionHandler(Socket socket, Map<String,String> records, Semaphore semaphore) {
        this.socket = socket;
        this.records = records;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("ok SDP");

            String line;
            loop: while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "register":
                        if(parts.length != 3) {
                            out.println("error register <name> <ip:port>");
                        }else{
                            try {
                                semaphore.acquire();
                                records.put(parts[1], parts[2]);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                semaphore.release();
                            }
                            out.println("ok");
                            out.flush();
                        }
                        break;
                    case "unregister":
                        if(parts.length != 2) {
                            out.println("error unregister <name>");
                        }else{
                            try {
                                semaphore.acquire();
                                records.remove(parts[1]);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                semaphore.release();
                            }
                            out.println("ok");
                            out.flush();
                        }
                        break;
                    case "resolve":
                        if(parts.length != 2) {
                            out.println("error resolve <name>");
                        }else if(!records.containsKey(parts[1])) {
                            out.println("error such domain dosesnt exist");
                        }
                        else{
                            out.println(records.get(parts[1]));
                            out.flush();
                        }
                        break;

                    case "exit":
                        if(parts.length != 1) {
                            out.println("error invalid arguments");
                            break;
                        }else{
                            out.println("ok bye");
                            out.flush();
                            break loop;
                        }
                    default:
                        StringBuilder err= new StringBuilder();
                        for(String part : parts) {
                            err.append(part).append(" ");
                        }
                        out.println("error usage "+ err);
                        out.flush();

                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
