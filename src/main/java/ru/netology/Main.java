package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        ExecutorService threadPoll = Executors.newFixedThreadPool(64);
        try (final var serverSocket = new ServerSocket(9999)) {
            while (true) {
                Socket socket = serverSocket.accept();
                threadPoll.execute(new Server(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}