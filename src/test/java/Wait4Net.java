import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic utility to wait for open port and HTTP status code.
 */
public class Wait4Net {
    public static void main(String[] args) {
        Wait4Net wait4Net =new Wait4Net();
        long started = System.currentTimeMillis();
        if(args.length!=3) {
            wait4Net.log("Expected 3 arguments:" +
                "\nwaitForHttpStatus: url status-code[,status-code]* timeoutMs)"+
                "\nwaitForPort: hostname port timeoutMs");
        }
        long timeoutMillis = Long.parseLong(args[2]);
        if(args[0].matches("^https*://.*")) {
            List<Integer> statusCodes = Stream.of(args[1].split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
            wait4Net.wait4HttpStatus(args[0], statusCodes, timeoutMillis);
        } else {
            wait4Net.wait4Port(args[0], Integer.parseInt(args[1]), timeoutMillis);
        }
        wait4Net.log("OK: "+(System.currentTimeMillis()-started)+" ms");
    }

    public void wait4Port(String hostname, int port, long timeoutMs) {
        log("Waiting for port " + port);
        long startTs = System.currentTimeMillis();
        boolean scanning = true;
        while (scanning) {
            if (System.currentTimeMillis() - startTs > timeoutMs) {
                throw new RuntimeException("Timeout waiting for port " + port);
            }
            try {
                SocketAddress address = new InetSocketAddress(hostname, port);
                Selector.open();
                try (SocketChannel socketChannel = SocketChannel.open()) {
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(address);
                }

                scanning = false;
            } catch (Exception e) {
                log("Still waiting for port " + port);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    log("Interrupted", ie);
                }
            }
        }
        log("Port " + port + " ready.");
    }

    public void wait4HttpStatus(String url, Collection<Integer> expectedStatuses, long timeoutMS) {
        HttpClient client = HttpClient.newBuilder().sslContext(trustingSSLContext()).build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(timeoutMS))
            .build();
        log("Waiting " + timeoutMS + "ms for " + url + " to respond with status: " + expectedStatuses);
        long startTS = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTS < timeoutMS) {
            try {
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if(expectedStatuses.contains(response.statusCode())) {
                    return;
                }
            }
            catch (Exception e) {
                if(e instanceof ConnectException) {
                    continue;
                }
                log(e.getMessage());
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException("Aborted", e);
            }
        }

        log("Timeout");
        System.exit(1);
    }

    private SSLContext trustingSSLContext() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {

                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return null; }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };

        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException|KeyManagementException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    private void log(String message) {
        log(message, null);
    }

    private void log(String message, Throwable t) {
        System.out.println(message);
        if (t != null) {
            t.printStackTrace();
        }
    }
}