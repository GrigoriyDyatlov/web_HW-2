package ru.netology;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
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

    // Получение строки запроса.
    private static String[] getRequestLine(byte[] buffer, int read, BufferedOutputStream out) throws IOException {
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
        }
        return requestLine;
    }

    private static void setHeadersAndBody(byte[] buffer, int read, BufferedInputStream in, BufferedOutputStream out, Request request) throws IOException {
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = new String(headersBytes);
        // Вносим заголовки.
        request.setHeaders(headers);

        if (!request.getMethod().equals("GET")) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = request.getHeader("Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get().getValue());
                final var bodyBytes = in.readNBytes(length);

                final var body = new String(bodyBytes);
                // Вносим тело.
                request.setBody(body);
            }
        }
    }


    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
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
        final var in = new BufferedInputStream(socket.getInputStream());
        final var out = new BufferedOutputStream(socket.getOutputStream());
        return new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        final var limit = 4096;

                        in.mark(limit);
                        final var buffer = new byte[limit];
                        final var read = in.read(buffer);

                        Request request = new Request(getRequestLine(buffer, read, out));
                        setHeadersAndBody(buffer, read, in, out, request);

                        if (!availableHandlers.containsKey(request.getMethod()) || !availableHandlers.get(request.getMethod()).containsKey(request.getPath())) {
//                        notFound(out);
                            out.write((
                                    "HTTP/1.1 200 OK\r\n" +
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