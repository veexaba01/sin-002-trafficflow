package co.wethinkcode.trafficflow;

import io.javalin.Javalin;

/**
 * Tracks the city-wide congestion level 0-8 on port 7022.
 */
public class CongestionServiceApp {

    private static int congestionLevel = 0;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7022);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/congestion", ctx -> ctx.contentType("application/json").result("{\"level\":" + congestionLevel + "}"));
    }

    /**
     * Returns true if the congestion level is within the valid 0 to 8 range.
     */
    public static boolean isValidLevel(int level) {
        return level >= 0 && level <= 8;
    }

    /**
     * Extracts the integer congestion level from a query parameter or JSON body, or returns -1 if invalid.
     */
    public static int parseLevel(String rawInput) {
        if (rawInput == null) {
            return -1;
        }
        String digits = rawInput.replaceAll("[^0-9-]", "");
        return digits.isEmpty() ? -1 : Integer.parseInt(digits);
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)