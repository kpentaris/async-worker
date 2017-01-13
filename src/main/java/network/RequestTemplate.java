package main.java.network;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author KPentaris - 13-Jan-17.
 */
public class RequestTemplate {

    private URL url;
    private HttpURLConnection connection;
    private Map<String, String> headers;
    private Map<String, String> parameters;

    public RequestTemplate(String urlString, String method) throws IOException {
        url = new URL(urlString);
        connection = (HttpURLConnection) this.url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
    }

    public void addRequestHeader(String key, String value) {
        if (headers == null)
            headers = new HashMap<>();
        headers.put(key, value);
    }

    public void addRequestParam(String key, String value) {
        if (parameters == null)
            parameters = new HashMap<>();
        parameters.put(key, value);
    }

    public String performRequest() throws IOException {
        headers.forEach((k, v) -> {
            connection.setRequestProperty(k, v);
        });

        try(OutputStream output = connection.getOutputStream()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                output.write((entry.getKey() + "=" + entry.getValue()).getBytes());
            }
        }

        StringBuilder response = new StringBuilder();
        try (InputStream input = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

}
