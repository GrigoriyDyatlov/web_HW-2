package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {
    private final static ConcurrentHashMap<String, HashMap<String, Handler>> availableHandlers = new ConcurrentHashMap<>();
    private final ExecutorService threadPoll = Executors.newFixedThreadPool(64);

    public Server() {
    }

    // Поток подключений.
    @Override
    public void run() {
        try (final var serverSocket = new ServerSocket(9999)) {
            while (true) {
                Socket socket = serverSocket.accept();
                threadPoll.execute(newConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Обработка запроса.
    public Runnable newConnection(Socket socket) throws IOException {
        var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        var out = new BufferedOutputStream(socket.getOutputStream());
        return new Runnable() {
            @Override
            public void run() {
                try {
                    final var requestLine = in.readLine();
                    final var parts = requestLine.split(" ");

                    if (parts.length != 3) {
                        // just close socket
                        socket.close();
                    }
                    // проверить правильность запроса, что бы не заводить лишние объекты.
                    Request request = new Request(parts);

                    if (!availableHandlers.containsKey(request.getMethod()) || !availableHandlers.get(request.getMethod()).containsKey(request.getPath())) {
                        out.write((
                                "HTTP/1.1 404 Not Found\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.flush();
                    }
                    availableHandlers.get(request.getMethod()).get(request.getPath()).handle(request, out);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    // Добавление расширения.
    public void addHandler(String method, String path, Handler handler) {
        HashMap<String, Handler> map = new HashMap<>();
        map.put(path, handler);
        availableHandlers.put(method, map);
    }

    //Добавление однотипных расширений.
    public void addListOfPathWithOneHandler(String method, List<String> paths, Handler handler) {
        HashMap<String, Handler> map = new HashMap<>();
        for (String path : paths) {
            map.put(path, handler);
        }
        availableHandlers.put(method, map);
    }

}