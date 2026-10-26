package co.wethinkcode.trafficflow;

import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * Tracks the city-wide congestion level 0-8 on port 7022.
 */
public class CongestionServiceApp {

    private static int congestionLevel = 0;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7022);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/congestion", ctx -> ctx.contentType("application/json").result("{\"level\":" + congestionLevel + "}"));
        app.post("/congestion", CongestionServiceApp::handleUpdateCongestion);
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

    /**
     * Updates the city-wide congestion level if valid 0-8 or returns 400 Bad Request.
     */
    private static void handleUpdateCongestion(Context ctx) {
        String input = (ctx.queryParam("level") != null) ? ctx.queryParam("level") : ctx.body();
        int newLevel = parseLevel(input);

        if (isValidLevel(newLevel)) {
            congestionLevel = newLevel;
            ctx.status(200).contentType("application/json").result("{\"level\":" + congestionLevel + "}");
        } else {
            ctx.status(400).contentType("application/json")
                    .result("{\"error\":\"Level must be between 0 and 8\"}");
        }
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.trafficflow.mq.MqConfig)