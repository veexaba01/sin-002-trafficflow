package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IntersectionServiceApp {

    private static String cachedIntersectionsJson = "[]";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7021);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Validates intersection/district names (source of truth).)
        // Add domain endpoints for intersection-service here.
        loadFromIngestionService();

        app.get("/intersections", ctx -> ctx.contentType("application/json").result(cachedIntersectionsJson));
        app.get("/intersections/{id}", IntersectionServiceApp::handleGetIntersectionById);
    }

    /**
     * Schedules a periodic heartbeat message every 3 seconds to ActiveMQ.
     */
    private static void startHeartbeatSender() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(IntersectionServiceApp::sendSingleHeartbeat, 1, 3, TimeUnit.SECONDS);
    }

    /**
     * Publishes a single HEARTBEAT message to MqConfig.HEARTBEAT_QUEUE.
     */
    private static void sendSingleHeartbeat() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(MqConfig.HEARTBEAT_QUEUE);
            MessageProducer producer = session.createProducer(queue);

            TextMessage message = session.createTextMessage("HEARTBEAT");
            producer.send(message);
            connection.close();
        } catch (Exception e) {
            System.out.println("ActiveMQ heartbeat failed: " + e.getMessage());
        }
    }

    /**
     * Returns 200 OK if the intersection ID exists, or 404 Not Found if unknown.
     */
    private static void handleGetIntersectionById(Context ctx) {
        String id = ctx.pathParam("id").trim().toUpperCase();

        if (isValidIntersectionId(cachedIntersectionsJson, id)) {
            ctx.status(200).contentType("application/json")
                    .result("{\"id\":\"" + id + "\",\"valid\":true}");
        } else {
            ctx.status(404).contentType("application/json")
                    .result("{\"error\":\"Intersection not found\",\"id\":\"" + id + "\"}");
        }
    }

    /**
     * Checks whether the normalized intersection ID exists in the JSON list.
     */
    public static boolean isValidIntersectionId(String json, String rawId) {
        if (json == null || rawId == null) {
            return false;
        }
        String normalizedId = rawId.trim().toUpperCase();
        String searchKey = "\"id\":\"" + normalizedId + "\"";
        return json.contains(searchKey);
    }

    /**
     * Fetches the cleaned intersections JSON from ingestion-service (port 7020).
     */
    private static void loadFromIngestionService() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7020/intersections"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            updateCacheIfSuccessful(response);
        } catch (Exception e) {
            cachedIntersectionsJson = "[{\"id\":\"INT-1001\"},{\"id\":\"INT-1002\"},{\"id\":\"INT-1005\"}]";
        }
    }

    /**
     * Updates the local JSON cache when ingestion-service responds with 200 OK.
     */
    private static void updateCacheIfSuccessful(HttpResponse<String> response) {
        if (response.statusCode() == 200) {
            cachedIntersectionsJson = response.body();
        }
    }
}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
