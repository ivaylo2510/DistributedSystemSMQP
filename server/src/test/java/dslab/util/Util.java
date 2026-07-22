package dslab.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class Util {
    private static boolean isTcpPortClosed(int port) {
        try {
            // Try to connect to the port
            SocketChannel socket = SocketChannel.open();
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress(Constants.LOCALHOST, port));
            boolean connected = socket.finishConnect();
            socket.close();
            // If we successfully connect, the port is still open, so return false
            return !connected;
        } catch (IOException e) {
            // If an exception occurs, it means the port is closed, so return true
            return true;
        }
    }

    public static void waitForTcpPortsToClose(int... ports) {
        for (int port : ports) {
            await()
                    .pollInterval(5, TimeUnit.MILLISECONDS)  // Adjust poll interval as needed
                    .atMost(1, TimeUnit.SECONDS)              // Maximum wait time before timing out
                    .until(() -> Util.isTcpPortClosed(port));     // Custom method to check if port is closed
        }
    }

    public static boolean isUdpPortListening(String host, int port) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(100);  // Set a timeout for the operation
            InetAddress address = InetAddress.getByName(host);
            byte[] buf = "ping".getBytes();  // Create a simple ping message
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);

            // Send the packet and check if it gets through without exception
            socket.send(packet);
            return true;  // If no exception is thrown, assume the port is listening
        } catch (IOException e) {
            return false;  // Port is not ready yet
        }
    }
}
