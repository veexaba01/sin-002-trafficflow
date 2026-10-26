package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RoutingServiceApp {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7023);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Provides estimated travel times based on congestion and intersection.)
        // Add domain endpoints for routing-service here.
        app.get("/routes/estimate", RoutingServiceApp::handleEstimateRoute);
    }

    /**
     * Formats the route travel time estimate as a JSON string.
     */
    public static String buildEstimateJson(String from, String to, int level, double minutes) {
        return "{\"from\":\"" + from.trim().toUpperCase() + "\",\"to\":\"" + to.trim().toUpperCase()
                + "\",\"congestionLevel\":" + level + ",\"estimatedMinutes\":" + minutes + "}";
    }

    /**
     * Calculates estimated travel time in minutes based on the congestion level.
     */
    public static double calculateTravelTime(int congestionLevel) {
        return 10.0 + (congestionLevel * 2.5);
    }

    /**
     * Returns true if a query parameter is null or blank.
     */
    public static boolean isMissing(String param) {
        return param == null || param.isBlank();
    }

    /**
     * Extracts the integer congestion level from a JSON response string.
     */
    public static int extractDigits(String body) {
        if (body == null) {
            return 0;
        }
        String digits = body.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    /**
     * Returns true only when both route endpoints exist in intersection-service.
     */
    private static boolean areBothEndpointsValid(String from, String to) throws Exception {
        return isValidIntersection(from) && isValidIntersection(to);
    }

    /**
     * Calls intersection-service on port 7021 to check if an intersection ID exists.
     */
    private static boolean isValidIntersection(String id) throws Exception {
        String cleanId = id.trim().toUpperCase();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7021/intersections/" + cleanId))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200;
    }

    /**
     * Fetches the current city-wide congestion level from congestion-service on port 7022.
     */
    private static int fetchCongestionLevel() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:7022/congestion"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return extractDigits(response.body());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Handles GET /routes/estimate?from={id}&to={id} requests.
     */
    private static void handleEstimateRoute(Context ctx) {
        String from = ctx.queryParam("from");
        String to = ctx.queryParam("to");

        if (isMissing(from) || isMissing(to)) {
            ctx.status(400).contentType("application/json")
                    .result("{\"error\":\"Missing 'from' or 'to' query parameter\"}");
            return;
        }

        evaluateRoute(ctx, from, to);
    }

    /**
     * Validates endpoints against intersection-service and returns the travel time estimate.
     */
    private static void evaluateRoute(Context ctx, String from, String to) {
        try {
            sendEstimateOrNotFound(ctx, from, to);
        } catch (Exception e) {
            ctx.status(503).contentType("application/json")
                    .result("{\"error\":\"Intersection service is unreachable\"}");
        }
    }

    /**
     * Sends 200 OK with the travel time estimate if both endpoints are valid, or 404 Not Found.
     */
    private static void sendEstimateOrNotFound(Context ctx, String from, String to) throws Exception {
        if (!areBothEndpointsValid(from, to)) {
            ctx.status(404).contentType("application/json")
                    .result("{\"error\":\"Invalid intersection ID\"}");
            return;
        }

        int congestionLevel = fetchCongestionLevel();
        double travelMinutes = calculateTravelTime(congestionLevel);
        ctx.status(200).contentType("application/json")
                .result(buildEstimateJson(from, to, congestionLevel, travelMinutes));
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
