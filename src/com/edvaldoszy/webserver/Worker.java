package com.edvaldoszy.webserver;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Worker implements Runnable {

    private Socket socket;
    private InputStream input;
    private Scanner inputScanner;

    private OutputStream output;

    private String method;
    private String path;
    private String httpVersion;

    private Map<String, String> headers;

    private File publicPath = new File("./public");

    public Worker(Socket socket) throws IOException {
        input = socket.getInputStream();
        output = socket.getOutputStream();

        headers = new HashMap<>();

        if (!publicPath.exists()) {
            if (!publicPath.mkdirs()) {
                throw new IOException("Error while creating public directory");
            }
        }
    }

    @Override
    public void run() {
        try {
            parseRequestHeaders();
            parseRequestBody();

            handleResponseBody();
        } catch (IOException ex) {
            System.err.println("Error while parsing request: " + ex.getMessage());
        }
    }

    private void parseRequestHeaders() throws IOException {
        inputScanner = new Scanner(input);
        if (!inputScanner.hasNext()) {
            throw new IOException("Invalid path line");
        }

        String line = inputScanner.nextLine();
        String[] parts = line.split(" ");

        if (parts.length < 3) {
            throw new IOException("Invalid path line");
        }

        this.method = parts[0].toUpperCase();
        this.path = parts[1];
        this.httpVersion = parts[2];

        while (!"".equals(line = inputScanner.nextLine())) {
            parts = line.split(":");
            if (parts.length < 2) {
                throw new IOException("Invalid header line");
            }

            String headerKey = parts[0].trim();
            String headerValue = parts[1].trim();
            headers.put(headerKey, headerValue);
        }
    }

    private void parseRequestBody() {

    }

    private void handleResponseBody() throws IOException {
        try {
            File file = new File(publicPath, path);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }

            response(file);

        } catch (FileNotFoundException ex) {
            response404();

        } finally {
            output.flush();
            output.close();
        }
    }

    private void write(byte[] bytes) throws IOException {
        output.write(bytes);
    }

    private void write(String s) throws IOException {
        System.out.print(s);
        output.write(s.getBytes());
    }

    private byte[] read(File file) throws IOException {
        BufferedInputStream bir = new BufferedInputStream(new FileInputStream(file));

        int size = (int) file.length();
        byte[] bytes = new byte[size];

        bir.read(bytes, 0, size);
        write(bytes);

        return bytes;
    }

    private String getContentType(File file) {
        Map<String, String> extensions = new HashMap<String, String>() {
            {
                put(".*\\.html$", "text/html");
                put(".*\\.txt$", "text/plain");
                put(".*\\.jpe?g$", "image/jpeg");
                put(".*\\.png$", "image/png");
            }
        };

        String name = file.getName();
        for (String e : extensions.keySet()) {
            if (Pattern.matches(e, name)) {
                return extensions.get(e);
            }
        }

        return "text/plain";
    }

    private void response(File file) throws IOException {
        write("HTTP/1.1 200 OK\n");
        write("Content-Type: " + getContentType(file) + "\n");
        write("Content-Length: " + file.length() + "\n");
        write("\n");

        write(read(file));
    }

    private void response404() throws IOException {
        File file = new File(publicPath, "404.html");

        write("HTTP/1.1 404 Not Found\n");
        write("Content-Type: " + getContentType(file) + "\n");
        write("Content-Length: " + file.length() + "\n");
        write("\n");

        write(read(file));
    }
}
