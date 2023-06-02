# Introduction
The provided code implements a simple chat application using Java sockets. It consists of a server-side component (`ChatServer`) and a client-side component (`ChatClient`). The server listens for incoming client connections and relays messages between clients. The clients can send and receive messages in a chat-like interface.

# Goals
The goal of the chat application is to enable real-time communication between multiple clients through a centralized server. Clients can connect to the server, send messages, and receive messages from other connected clients.

# Client-Side (`ChatClient`)
The client-side code (`ChatClient`) is responsible for connecting to the server, sending messages, and displaying received messages.

## Dependencies
The client-side code uses the following dependencies:
- `javax.swing`: Provides the graphical user interface components.
- `java.awt`: Provides the core functionality for creating and managing graphical user interfaces.
- `java.awt.event`: Provides the event handling mechanisms for user interactions.

## Code Overview:

* The `chatclient` class extends `JFrame`, which allows it to create the chat client window. It serves as the main graphical interface for the client-side application.

* The client window is created with a specified title, size, and layout. These settings define the appearance and dimensions of the chat client window.

* The message input field (`JTextField`) is created at the top of the window. It serves as a text box where the user can enter their messages. An `ActionListener` is added to the text field, which listens for the Enter key press event. When the user presses Enter, the `sendMessage()` method is called to send the message to the server.

* The chat area (`JTextArea`) is created in the center of the window. It provides a display area for the received messages from the server. The `JTextArea` is set to be non-editable, meaning users cannot directly modify the contents of the chat area.

* The client establishes a connection to the server by creating a `Socket` instance with the server's IP address and port. In this case, the IP address is set to `"localhost"` (referring to the local machine) and the port is set to `12345`.

* Input and output streams (`BufferedReader` and `PrintWriter`) are created to facilitate communication with the server. The input stream is used to receive messages from the server, while the output stream is used to send messages to the server.

* The client's name is obtained using a dialog box (`JOptionPane`) to provide a prompt for the user to enter their name. The entered name is then sent to the server as an identification for the client.

* To handle incoming messages from the server, a separate thread (`ServerHandler`) is created. This thread runs concurrently with the main thread and is responsible for continuously reading messages from the server.

* The `ServerHandler` thread reads messages from the server using the input stream. Each received message is appended to the chat area, allowing the user to see the messages sent by other clients in real-time.

>Overall, the client-side code establishes a graphical user interface for the chat client, allows the user to send messages to the server, and displays messages received from other clients. The `ServerHandler` thread ensures that messages from the server are continuously read and displayed in the chat area, ensuring real-time communication.

## Code - ChatClient
```java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class chatclient extends JFrame {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JTextField messageField;
    private JTextArea chatArea;

    public chatclient() {
        setTitle("Chat Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Message input field
        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        add(messageField, BorderLayout.NORTH);

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);

        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Get client's name
            String name = JOptionPane.showInputDialog("Enter your name:");
            out.println(name);

            // Create a separate thread to handle server messages
            Thread serverThread = new Thread(new ServerHandler());
            serverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        out.println(message);
        messageField.setText("");
    }

    private void appendToChatArea(String message) {
        chatArea.append(message + "\n");
    }

    class ServerHandler implements Runnable {
        public void run() {
            String serverResponse;
            try {
                while ((serverResponse = in.readLine()) != null) {
                    appendToChatArea(serverResponse);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new chatclient();
            }
        });
    }
}

```

# Server-Side (`ChatServer`)
The server-side code (`ChatServer`) is responsible for accepting client connections, handling client communication, and broadcasting messages to all connected clients.

## Code Overview:

* The server code runs in an infinite loop, continuously listening for incoming client connections using a `ServerSocket` on a specific port (`12345`). This allows the server to accept multiple client connections.

* When a client connects, a new `Socket` is created to handle communication with that client, and the client's socket is added to a list of client sockets (`clientSockets`). This list keeps track of all connected clients.

* For each connected client, a new `ClientHandler` instance is created. The `ClientHandler` implements the `Runnable` interface, allowing it to run in a separate thread. Creating a separate thread for each client enables concurrent handling of multiple client connections.

* The `ClientHandler` receives the client's name from the client via the input stream. The name serves as an identification for the client. Upon receiving the name, the server prints a message indicating that the client is connected.

* Once the client's name is received, the `ClientHandler` enters a loop to receive messages from the client. It continuously reads messages from the client's input stream. Each received message is then broadcasted to all connected clients by calling the `broadcast()` method.

* The `broadcast()` method iterates over the list of client sockets (`clientSockets`) and sends the message to each connected client. This ensures that all clients receive the message sent by any individual client.

* If a client disconnects, its socket is closed, and it is removed from the list of client sockets (`clientSockets`). This ensures that the server stops broadcasting messages to the disconnected client.

>In summary, the server-side code (`ChatServer`) listens for incoming client connections, handles client communication through separate threads (`ClientHandler`), broadcasts messages to all connected clients, and manages the addition and removal of client sockets from the list of connected clients. This allows for concurrent and real-time communication between multiple clients through the central server.

## Code - ChatServer
```java
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
```

# Results and How to Run
To run the chat application, follow these steps:

1. Compile the server-side code (`ChatServer`) and run it.
```
$ javac ChatServer.java
$ java ChatServer
```

2. Compile the client-side code (`ChatClient`) and run it. This will open the chat client window.
```
$ javac chatclient.java
$ java chatclient
```

3. In the chat client window, enter your name in the provided dialog box.
4. Type a message in the input field at the top of the window and press Enter to send it.
5. The message will be sent to the server and broadcasted to all connected clients.
6. Received messages from other clients will be displayed in the chat area of the client window.

Note: Make sure to run the server code before running the client code to establish a connection.  


as you can see all the 3 users can talk and each one of them has a GUI in which he can visualise the group chat

![image](https://github.com/mohammedAcheddad/Guessing_game/assets/105829473/bfbc81db-2f95-43cf-940c-e2f88842eee7)
  
and the server listens to everything and logs the connection status of each user    

![image](https://github.com/mohammedAcheddad/Guessing_game/assets/105829473/22fec1a4-170b-4e53-9987-6f5a37ba422d)

# Conclusion
The provided chat server and chat client code allow multiple clients to connect to a central server and exchange messages in real-time. The server-side code handles client connections and message broadcasting, while the client-side code provides a graphical user interface for sending and receiving messages. By following the instructions to run the code, you can test the chat application and observe the communication between clients. Feel free to modify the code and experiment with additional features to enhance the chat application's functionality.
