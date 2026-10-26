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

public class RoutingServiceApp {
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static int currentCongestionLevel = 0;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7023);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Provides estimated travel times based on congestion and intersection.)
        // Add domain endpoints for routing-service here.
        subscribeToCongestionTopic();
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
     * Subscribes to MqConfig.TOPIC on ActiveMQ to receive congestion level changes asynchronously.
     */
    private static void subscribeToCongestionTopic() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(RoutingServiceApp::handleIncomingMessage);
        } catch (Exception e) {
            System.out.println("ActiveMQ broker not running: " + e.getMessage());
        }
    }

    /**
     * Processes incoming ActiveMQ topic messages containing congestion updates.
     */
    private static void handleIncomingMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                updateCongestionFromMessage(textMessage.getText());
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Updates the cached congestion level from a topic message payload.
     */
    public static void updateCongestionFromMessage(String messageText) {
        currentCongestionLevel = extractDigits(messageText);
    }

    /**
     * Returns the currently cached city-wide congestion level.
     */
    public static int getCurrentCongestionLevel() {
        return currentCongestionLevel;
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

        double travelMinutes = calculateTravelTime(currentCongestionLevel);
        ctx.status(200).contentType("application/json")
                .result(buildEstimateJson(from, to, currentCongestionLevel, travelMinutes));
    }
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
