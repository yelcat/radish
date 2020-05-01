package com.radishframework.radish.core.operation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 运维管理服务
 */
public class OperationServer {

    protected final HttpServer server;
    private final ExecutorService executorService;

    /**
     * Start a HTTP server serving Prometheus metrics from the given registry.
     */
    public OperationServer(OperationConfig operationConfig, ApplicationOperation applicationOperation)
            throws IOException {
        server = HttpServer.create(
                new InetSocketAddress(operationConfig.getHostAddress(), operationConfig.getOperationPort()),
                3);

        server.createContext("/status", new StatusHandler(applicationOperation));
        server.createContext("/shutdown", new ShutdownHandler(applicationOperation));

        HttpHandler mHandler = new HTTPMetricHandler(CollectorRegistry.defaultRegistry);
        server.createContext("/", mHandler);
        server.createContext("/metrics", mHandler);

        executorService = Executors.newFixedThreadPool(5, OperationServer.DaemonThreadFactory.defaultThreadFactory(operationConfig.isDaemon()));
        server.setExecutor(executorService);
        start(operationConfig.isDaemon());
    }

    static class ShutdownHandler implements HttpHandler {

        private final ApplicationOperation applicationOperation;

        ShutdownHandler(ApplicationOperation applicationOperation) {
            this.applicationOperation = applicationOperation;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            if (!"PUT".equals(httpExchange.getRequestMethod())) {
                httpExchange.sendResponseHeaders(405, 0);
                OutputStream os = httpExchange.getResponseBody();
                os.close();
                return;
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    applicationOperation.stop();
                    System.exit(0);
                }
            }, 100);

            writeResponse(httpExchange, "shutdown is accepted");
        }
    }

    static class StatusHandler implements HttpHandler {

        private final ApplicationOperation applicationOperation;

        StatusHandler(ApplicationOperation applicationOperation) {
            this.applicationOperation = applicationOperation;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            final String status = applicationOperation.isRunning() ? "ok" : "error";
            final String response = "{ \"status\": \"" + status + "\"}";
            writeResponse(exchange, response);
        }
    }

    static class HTTPMetricHandler implements HttpHandler {
        private CollectorRegistry registry;
        private final LocalByteArray response = new LocalByteArray();

        HTTPMetricHandler(CollectorRegistry registry) {
            this.registry = registry;
        }

        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getRawQuery();

            ByteArrayOutputStream response = this.response.get();
            response.reset();
            OutputStreamWriter osw = new OutputStreamWriter(response);
            TextFormat.write004(osw,
                    registry.filteredMetricFamilySamples(parseQuery(query)));
            osw.flush();
            osw.close();
            response.flush();
            response.close();

            t.getResponseHeaders().set("Content-Type",
                    TextFormat.CONTENT_TYPE_004);
            if (shouldUseCompression(t)) {
                t.getResponseHeaders().set("Content-Encoding", "gzip");
                t.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                final GZIPOutputStream os = new GZIPOutputStream(t.getResponseBody());
                response.writeTo(os);
                os.close();
            } else {
                t.getResponseHeaders().set("Content-Length",
                        String.valueOf(response.size()));
                t.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.size());
                response.writeTo(t.getResponseBody());
            }
            t.close();
        }

    }

    private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {
        protected ByteArrayOutputStream initialValue()
        {
            return new ByteArrayOutputStream(1 << 20);
        }
    }

    protected static boolean shouldUseCompression(HttpExchange exchange) {
        List<String> encodingHeaders = exchange.getRequestHeaders().get("Accept-Encoding");
        if (encodingHeaders == null) return false;

        for (String encodingHeader : encodingHeaders) {
            String[] encodings = encodingHeader.split(",");
            for (String encoding : encodings) {
                if (encoding.trim().toLowerCase().equals("gzip")) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static Set<String> parseQuery(String query) throws IOException {
        Set<String> names = new HashSet<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]")) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }
        return names;
    }


    static class DaemonThreadFactory implements ThreadFactory {
        private ThreadFactory delegate;
        private final boolean daemon;

        DaemonThreadFactory(ThreadFactory delegate, boolean daemon) {
            this.delegate = delegate;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = delegate.newThread(r);
            t.setDaemon(daemon);
            return t;
        }

        static ThreadFactory defaultThreadFactory(boolean daemon) {
            return new OperationServer.DaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
        }
    }

    /**
     * Start a HTTP server by making sure that its background thread inherit proper daemon flag.
     */
    private void start(boolean daemon) {
        if (daemon == Thread.currentThread().isDaemon()) {
            server.start();
        } else {
            FutureTask<Void> startTask = new FutureTask<>(server::start, null);
            OperationServer.DaemonThreadFactory.defaultThreadFactory(daemon).newThread(startTask).start();
            try {
                startTask.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Unexpected exception on starting HTTPSever", e);
            } catch (InterruptedException e) {
                // This is possible only if the current tread has been interrupted,
                // but in real use cases this should not happen.
                // In any case, there is nothing to do, except to propagate interrupted flag.
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        server.stop(0);
        executorService.shutdown(); // Free any (parked/idle) threads in pool
    }

    private static void writeResponse(HttpExchange exchange, String result) throws IOException {
        checkNotNull(result, "response body can not be null");

        exchange.sendResponseHeaders(200, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(result.getBytes());
        os.close();
    }
}
