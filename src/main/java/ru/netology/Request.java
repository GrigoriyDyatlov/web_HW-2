package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

public class Request {
    private final String method;
    private final String path;
    private List<NameValuePair> headers;
    private List<NameValuePair> queryParams;
    private String body;

    public Request(String[] requestLine) {
        this.method = requestLine[0];
        if (!requestLine[1].contains("?")) {
            this.path = requestLine[1];
        } else {
            int queryStringBegin = requestLine[1].indexOf("?");
            this.path = requestLine[1].substring(0, queryStringBegin);
            this.queryParams = URLEncodedUtils.parse(requestLine[1].substring(queryStringBegin + 1), Charset.defaultCharset());
        }
        System.out.println(getMethod());
        System.out.println(queryParams);
    }
    public void setHeaders (String headers){
        this.headers = URLEncodedUtils.parse(headers, Charset.defaultCharset());
        System.out.println(headers);
    }

    public void setBody (String body){
        this.body = body;
        System.out.println(body);
    }

    public Optional<NameValuePair> getQueryParam(String name) {
        return queryParams.stream()
                .filter(o -> o.getName().equals(name))
                .findAny();
    }

    public Optional<NameValuePair> getHeader(String name) {
        return headers.stream()
                .filter(o -> o.getName().equals(name))
                .findAny();
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