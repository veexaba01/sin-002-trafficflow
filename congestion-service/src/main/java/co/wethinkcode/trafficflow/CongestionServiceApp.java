package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

/**
 * Tracks the city-wide congestion level (0-8) on port 7022 and publishes changes to ActiveMQ.
 */
public class CongestionServiceApp {

    private static int congestionLevel = 0;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7022);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/congestion", ctx -> ctx.contentType("application/json").result(buildLevelJson(congestionLevel)));
        app.post("/congestion", CongestionServiceApp::handleUpdateCongestion);
    }

    /**
     * Updates the city-wide congestion level if valid (0-8) and publishes to ActiveMQ, or returns 400.
     */
    private static void handleUpdateCongestion(Context ctx) {
        String input = (ctx.queryParam("level") != null) ? ctx.queryParam("level") : ctx.body();
        int newLevel = parseLevel(input);

        if (isValidLevel(newLevel)) {
            congestionLevel = newLevel;
            publishCongestionUpdate(congestionLevel);
            ctx.status(200).contentType("application/json").result(buildLevelJson(congestionLevel));
        } else {
            ctx.status(400).contentType("application/json")
                    .result("{\"error\":\"Level must be between 0 and 8\"}");
        }
    }

    /**
     * Publishes the updated congestion level to the ActiveMQ congestion-topic.
     */
    private static void publishCongestionUpdate(int level) {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageProducer producer = session.createProducer(topic);

            TextMessage message = session.createTextMessage(buildLevelJson(level));
            producer.send(message);
            connection.close();
        } catch (Exception e) {
            System.out.println("ActiveMQ broker not running: " + e.getMessage());
        }
    }

    /**
     * Formats the congestion level as a JSON payload string.
     */
    public static String buildLevelJson(int level) {
        return "{\"level\":" + level + "}";
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