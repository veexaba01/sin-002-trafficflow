package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
    }

    /**
     * Checks whether normalized intersection ID exists in the JSON list.
     */
    public static boolean isValidIntersectionId(String json, String rawId)
    {
        if (json == null || rawId == null) {return false;}
        String normalizedId = rawId.trim().toUpperCase();
        String searchKey = "\"id\":\"" + normalizedId + "\"";
        return json.contains(searchKey);
    }

    /**
     * Fetches cleaned intersections JSON from the ingestion-service on port 7020.
     */
    private static void loadFromIngestionService()
    {
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
     * Updates local JSON cache when ingestion-service responds with 200 OK.
     */
    private static void updateCacheIfSuccessful(HttpResponse<String> response)
    {
        if (response.statusCode() == 200)
        {
            cachedIntersectionsJson = response.body();
        }
    }
}

// MQ TODO: publishes a periodic heartbeat to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at
// MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig), consumed by intersection-watchdog.
