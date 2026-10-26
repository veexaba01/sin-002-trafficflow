package co.wethinkcode.trafficflow;

import co.wethinkcode.trafficflow.mq.MqConfig;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class IntersectionWatchdogApp {

    private static final long TIMEOUT_MS = 10_000;
    private static long lastHeartbeatTime = 0;

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7024);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Cries for help if the Intersection Service crashes, since routes can no longer be validated.)
        // Mechanism: ActiveMQ Queue heartbeat/dead-letter
        app.get("/alert", IntersectionWatchdogApp::handleAlertCheck);
    }

    /**
     * Returns 503 ALERT if heartbeats from intersection-service are missed, or 200 HEALTHY if alive.
     */
    private static void handleAlertCheck(Context ctx) {
        long now = System.currentTimeMillis();

        if (isHeartbeatMissed(lastHeartbeatTime, now)) {
            ctx.status(503).contentType("application/json")
                    .result("{\"status\":\"ALERT\",\"message\":\"Intersection Service heartbeat missed\"}");
        } else {
            ctx.status(200).contentType("application/json")
                    .result("{\"status\":\"HEALTHY\",\"message\":\"Intersection Service is alive\"}");
        }
    }

    /**
     * Returns true if no heartbeat was ever received or if the timeout has elapsed.
     */
    public static boolean isHeartbeatMissed(long lastSeenMs, long currentTimeMs) {
        return lastSeenMs == 0 || (currentTimeMs - lastSeenMs) > TIMEOUT_MS;
    }

    /**
     * Subscribes to MqConfig.HEARTBEAT_QUEUE on ActiveMQ and records each heartbeat timestamp.
     */
    private static void listenForHeartbeats() {
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(MqConfig.BROKER_URL);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue(MqConfig.HEARTBEAT_QUEUE);
            MessageConsumer consumer = session.createConsumer(queue);

            consumer.setMessageListener(message -> lastHeartbeatTime = System.currentTimeMillis());
        } catch (Exception e) {
            System.out.println("Could not subscribe to heartbeat queue: " + e.getMessage());
        }
    }
}

// MQ TODO: subscribes to ActiveMQ queue MqConfig.HEARTBEAT_QUEUE at MqConfig.BROKER_URL
// (see co.wethinkcode.trafficflow.mq.MqConfig) and alerts if a heartbeat from
// intersection-service is missed or a message lands in the dead-letter queue.
