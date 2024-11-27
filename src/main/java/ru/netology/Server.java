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

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void notFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
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
                    String[] input = in.readLine().split(" ");
                    if (input.length != 3) badRequest(out);
                    Request request = new Request(input);

                    if (!availableHandlers.containsKey(request.getMethod()) || !availableHandlers.get(request.getMethod()).containsKey(request.getPath())) {
                        notFound(out);
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