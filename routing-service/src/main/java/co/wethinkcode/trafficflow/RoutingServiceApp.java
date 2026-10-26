package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

public class RoutingServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7023);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Provides estimated travel times based on congestion and intersection.)
        // Add domain endpoints for routing-service here.
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
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)
