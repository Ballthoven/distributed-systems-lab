import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Node {
    private int myPort;
    private List<Integer> peerPorts;
    private Map<Integer, Boolean> alivePeers = new HashMap<>();

    public Node(int myPort, List<Integer> peerPorts) {
        this.myPort = myPort;
        this.peerPorts = peerPorts;
    }
    public void start(){
        //Listen for Heartbeats
        new Thread(this::listen).start();

        //This one sends the heartbeat to others every 2 seconds
        new Thread(() -> {
            while (true){
                for (int peer : peerPorts){
                    sendHeartbeat(peer);
                }
                System.out.println("Node " + myPort + " status: " + alivePeers);
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                }

            }
        }).start();
    }

    private void sendHeartbeat(int peerPort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", peerPort), 500); // 500ms timeout
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ALIVE:" + myPort);
            alivePeers.put(peerPort, true);
        } catch (IOException e) {
            alivePeers.put(peerPort, false);
        }
    }

    private void listen() {
        try (ServerSocket serverSocket = new ServerSocket(myPort)){
            while (true) {
                try (Socket socket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String line = in.readLine(); // Receives "ALIVE:808X" from port
                    int senderPort = Integer.parseInt(line.split(":")[1]);
                    alivePeers.put(senderPort, true);
                } catch (Exception e) {}
            }
    } catch (IOException e) {
        e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        List<Integer> allPorts = Arrays.asList(8081, 8082, 8083);
        List<Integer> peers = new ArrayList<>(allPorts);
        peers.remove(Integer.valueOf(port));

        new Node(port, peers).start();
    }
}
