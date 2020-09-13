import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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
        if(args.length!=3) {
            wait4Net.log("Expected 3 arguments:" +
                "\nwaitForHttpStatus: url statuscode[,statuscode]* timeoutMs)"+
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
        HttpClient client = HttpClient.newBuilder().build();
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