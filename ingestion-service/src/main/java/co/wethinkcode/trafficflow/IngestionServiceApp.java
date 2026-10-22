package co.wethinkcode.trafficflow;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.javalin.Javalin;

public class IngestionServiceApp {

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7020);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/intersections-legacy.csv (intersections, districts, signal types data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.
        List<Intersection> intersections = loadCleanedIntersections();

        app.get("/intersections", ctx ->
        {
            ctx.contentType("application/json").result(toJsonArray(intersections));

        });
    }

    public static List<Intersection> loadCleanedIntersections()
    {
        IngestionService service = new IngestionService();
        InputStream csvStream = IngestionServiceApp.class.getResourceAsStream("/intersections-legacy.csv");
        return service.cleanCsv(csvStream);
    }

    public static String toJsonArray(List<Intersection> intersections)
    {
        List<String> jsonItems = new ArrayList<>();
        for (Intersection item : intersections)
        {
            jsonItems.add(item.toJson());
        }
        return "[" + String.join(",", jsonItems) + "]";
    }
}
