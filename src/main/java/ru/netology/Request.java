package ru.netology;

import org.apache.commons.fileupload.FileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class Request {
    private final String method;
    private final String path;
    private final List<NameValuePair> headers;
    private List<NameValuePair> queryParams;
    private String body;

    public Request(BufferedInputStream in, BufferedOutputStream out) throws IOException {
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
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
        this.method = requestLine[0];
        // check query string
        if (!requestLine[1].contains("?")) {
            this.path = requestLine[1];
        } else {
            int queryStringBegin = requestLine[1].indexOf("?");
            this.path = requestLine[1].substring(0, queryStringBegin);
            this.queryParams = URLEncodedUtils.parse(requestLine[1].substring(queryStringBegin + 1), Charset.defaultCharset());
            System.out.println(queryParams);
        }
        //search headers
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

        this.headers = URLEncodedUtils.parse(headers, Charset.defaultCharset());
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
                final var length = Integer.parseInt(getHeader("Content-Length").get().getValue());
                final var bodyBytes = in.readNBytes(length);

                final var body = new String(bodyBytes);
                this.body = body;
        }
        System.out.println(method);
        System.out.println(path);
        System.out.println(queryParams);
        System.out.println(getHeaders());
        System.out.println(body);
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

    public Optional<NameValuePair> getQueryParam(String name) {
        return queryParams.stream()
                .filter(o -> o.getName().equals(name))
                .findFirst();
    }

    public Optional<NameValuePair> getHeader(String name) {
        return headers.stream()
                .filter(o -> o.getName().equals(name))
                .findFirst();
    }

    public List<NameValuePair> getHeaders() {
        return headers;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}