package com.edvaldoszy.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private ServerSocket serverSocket;

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            listen();
        } catch (IOException ex) {
            System.err.println("Error while starting server: " + ex.getMessage());
        }
    }

    private void listen() throws IOException {
        System.out.println("Waiting for connections...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected");

            new Thread(new Worker(clientSocket))
                    .start();
        }
    }

    public static void main(String[] args) {
        new Server(8081);
    }
}
