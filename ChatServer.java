import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static List<Socket> clientSockets = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Chat Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Get the client's name
                clientName = in.readLine();
                System.out.println("Connected: " + clientName);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(clientName + ": " + inputLine);

                    // Broadcast the message to all connected clients
                    broadcast(inputLine);
                }

                in.close();
                out.close();
                clientSocket.close();
                clientSockets.remove(clientSocket);
                System.out.println("Disconnected: " + clientName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void broadcast(String message) {
            for (Socket socket : clientSockets) {
                try {
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    writer.println(clientName + ": " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
