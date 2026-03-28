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
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
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
            return;
        }
        
        inst = new Lanshare();
        if (!extractArgsBool(args, "-u", true)) {
            inst.loadFile(extractArgs(args, "-c", "default.dat"));
        } else {
            System.out.println("Using UI");
            JFrame d = new JFrame();
            TextArea la = new TextArea();
            d.add(new Label("Enter port and backlog with port:backlog or filename or blank to load default.dat"));
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
        server.bind(new InetSocketAddress(data.port), data.backlog);
        init();
        server.start();
        System.out.println("Server started!");
    }
    
    /**
     * Run ONLY after loadFile()!
     */
    public void init() {
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("/".equals(exchange.getRequestURI().getPath())) {
                    exchange.getResponseHeaders().add("Location", "/index.html");
                    exchange.sendResponseHeaders(301, -1);
                    return;
                }
                String path = exchange.getRequestURI().getPath().substring("/".length());
                System.out.println("Visit logged at " + path);
                Path file = Path.of("public", path);
                
                if (!Files.exists(file)) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                switch (exchange.getRequestMethod()) {
                    case "GET" -> {
                        if (Files.isDirectory(file)) {
                            /*var arr = Files.list(file)
                               .map(p -> "[\"" + p.getFileName().toString().replace("\\", "/") +
                                "\",\"" + Files.isDirectory(p) + "\"]")
                                .toArray(String[]::new);
                            String json = "[" + String.join(",", arr) + "]";
                            byte[] data = json.getBytes();
                            exchange.getResponseHeaders().add("Content-Type", "application/json");
                            exchange.sendResponseHeaders(200, data.length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(data);
                            }*/
                            exchange.sendResponseHeaders(422, -1);
                        } else {
                            String type = URLConnection.getFileNameMap().getContentTypeFor(file.toString());
                            exchange.getResponseHeaders().add("Content-Type", type==null?"application/octet-stream":type);
                            exchange.sendResponseHeaders(200, Files.size(Path.of("public", path)));
                            try (OutputStream os = exchange.getResponseBody(); InputStream is = Files.newInputStream(Path.of("public", path))) {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = is.read(buf)) != -1) {
                                    os.write(buf, 0, n);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    }
                    case "HEAD" -> {
                        if (Files.isDirectory(file)) {
                            exchange.sendResponseHeaders(204, -1);
                            break;
                        }
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
