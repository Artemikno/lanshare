/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.lanshare.lanshare;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Button;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;

/**
 *
 * @author HP
 */
public class Lanshare implements AutoCloseable {
    HttpServer server;
    private SerializableData data;
    private static Lanshare inst;
    
    public static class SerializableData implements Serializable {
        public int port;
        public int backlog;
        
        SerializableData(int a, int b) {
            port = a;
            backlog = b;
        }
    }
    
    public Lanshare getInst() {
        return inst;
    }
    
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.out.println("Hello World!");
        File j = new File("public");
        if (!(j.exists())) {
            j.mkdir();
        }
        
        List l = Arrays.asList(args);
        if (l.contains("-?")) {
            System.out.println("Usage: ");
            System.out.println("-u <true|false> = load from config / no UI");
            System.out.println("-c <file> = config file");
            System.out.println("");
            System.out.println("UI mode is recommended");
            System.out.println("because config files cannot be created any other way");
            return;
        }
        
        inst = new Lanshare();
        if (!extractArgsBool(args, "-u", true)) {
            inst.loadFile(extractArgs(args, "-c", "default.dat"));
        } else {
            System.out.println("Using UI");
            JFrame d = new JFrame();
            TextArea la = new TextArea();
            d.add(new Label("Enter port and backlog with port:backlog or filename"));
            d.add(la);
            Button b = new Button("Save port:backlog");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int i = 0;
                    File h = new File("default.dat");
                    if (h.exists()) {
                        while (true) {
                            File f = new File("Profile" + i + ".dat");
                            if (f.exists()) {
                                continue;
                            }
                            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f.getAbsolutePath()))) {
                                oos.writeObject(new SerializableData(Integer.parseInt(la.getText().split(":")[0]), Integer.parseInt(la.getText().split(":")[1])));
                            } catch (Exception ex) {
                                break;
                            }
                            break;
                        }
                    }
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(h.getAbsolutePath()))) {
                        oos.writeObject(new SerializableData(Integer.parseInt(la.getText().split(":")[0]), Integer.parseInt(la.getText().split(":")[1])));
                    } catch (Exception ex) {
                    }
                }
            });
            d.add(b);
            Button s = new Button("Apply file");
            s.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        inst.loadFile(la.getText().equals("") ? "default.dat" : la.getText());
                    } catch (IOException | ClassNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                    d.hide();
                    d.dispose();
                }
            });
            d.add(s);
            d.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            d.setLayout(new FlowLayout());
            d.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            d.setSize(500,300);
            d.show();
        }
        
    }
    
    /**
     * Loads a configuration file
     * @param file The file to load configuration from
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     */
    public void loadFile(String file) throws IOException, ClassNotFoundException {
        data = (SerializableData) new ObjectInputStream(new FileInputStream(file)).readObject();
                System.out.println("---- 1 ----");
        server.bind(new InetSocketAddress(data.port), data.backlog);
                System.out.println("---- 2 ----");
        init();
                System.out.println("---- 3 ----");
        server.start();
                System.out.println("---- 4 ----");
    }
    
    /**
     * Run ONLY after loadFile()!
     */
    public void init() {
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("/");
                String htmlData = """
                                  <!DOCTYPE html>
                                  <html lang="en">
                                  <head>
                                  <meta charset="UTF-8">
                                  <title>File Browser</title>
                                  <style>
                                    body { font-family: sans-serif; }
                                    ul { list-style-type: "\\1F4C1"; padding-left: 1em; }
                                    li { margin: 0.2em 0; cursor: pointer; color: blue; }
                                  </style>
                                  </head>
                                  <body>
                                  <h1>File Browser</h1>
                                  <ul id="fileTree"></ul>
                                  
                                  <script>
                                  async function loadFiles() {
                                      const res = await fetch('/file/');
                                      const tree = await res.json();
                                      const ul = document.getElementById('fileTree');
                                      ul.innerHTML = '';
                                  
                                      tree.forEach(([dir, file]) => {
                                          const li = document.createElement('li');
                                          li.textContent = (dir ? dir + '/' : '') + file;
                                          li.onclick = () => downloadFile(dir, file);
                                          ul.appendChild(li);
                                      });
                                  }
                                  
                                  function downloadFile(dir, file) {
                                      const path = encodeURIComponent((dir ? dir + '/' : '') + file);
                                      const url = '/file/' + path;
                                      fetch(url)
                                          .then(r => {
                                              if (!r.ok) throw new Error('File not found');
                                              return r.blob();
                                          })
                                          .then(blob => {
                                              const a = document.createElement('a');
                                              a.href = URL.createObjectURL(blob);
                                              a.download = file;
                                              a.click();
                                          })
                                          .catch(e => alert(e));
                                  }
                                  
                                  loadFiles();
                                  </script>
                                  </body>
                                  </html>""";
                byte[] data = htmlData.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            }
        });
        server.createContext("/file", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("/file");
                String path = exchange.getRequestURI().getPath().substring("/file".length());
                if (path.isEmpty() || path.equals("/")) {
                    Path root = Path.of("public");
                    List<List<String>> tree = new ArrayList<>();
                    try (var paths = Files.walk(root)) {
                        paths.forEach(p -> {
                            if (!Files.isDirectory(p)) {
                                tree.add(List.of(root.relativize(p).getParent() == null ? ""
                                        : root.relativize(p).getParent().toString(),
                                        p.getFileName().toString()));
                            }
                        });
                    }
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < tree.size(); i++) {
                        List<String> e = tree.get(i);
                        json.append("[\"").append(e.get(0).replace("\\", "/"))
                                .append("\",\"").append(e.get(1)).append("\"]");
                        if (i < tree.size() - 1) {
                            json.append(",");
                        }
                    }
                    json.append("]");
                    byte[] data = json.toString().getBytes();
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, data.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(data);
                    }
                    return;
                }
                Path file = Path.of("public", path.substring("/".length()));
                if (!Files.exists(file) || Files.isDirectory(file)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                switch (exchange.getRequestMethod()) {
                    case "GET" -> {
                        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                        try (OutputStream os = exchange.getResponseBody(); InputStream is = Files.newInputStream(Path.of("public", exchange.getRequestURI().getPath().substring("/file/".length())))) {
                            exchange.sendResponseHeaders(200, 0);
                            byte[] buf = new byte[8192];
                            int n;
                            while ((n = is.read(buf)) != -1) {
                                os.write(buf, 0, n);
                            }
                        } catch (Exception ex) {
                            exchange.sendResponseHeaders(409, -1);
                        }
                        break;
                    }
                    case "HEAD" -> {
                        exchange.sendResponseHeaders(200, -1);
                        break;
                    }
                    default -> {
                        exchange.sendResponseHeaders(405, -1);
                        break;
                    }
                }
            }
        });
    }
    
    private static String extractArgs(String[] args, String option, String def) {
        Integer i = Arrays.binarySearch(args, option);
        return (Arrays.asList(args).contains(option) && args.length > i) ? args[i+1] : def;
    }
    
    private static boolean extractArgsBool(String[] args, String option, boolean def) {
        int i = Arrays.binarySearch(args, option);
        return (Arrays.asList(args).contains(option) && args.length > i) ? Boolean.parseBoolean(args[i+1]) : def;
    }
    
    private Lanshare(int port, int backlog) throws IOException {
        inst = this;
        server = HttpServer.create(new InetSocketAddress(port), backlog);
        server.start();
    }
    
    private Lanshare() throws IOException {
        inst = this;
        server = HttpServer.create();
    }

    @Override
    public void close() throws Exception {
        server.stop(0);
        data = null;
        server = null;
    }
}
