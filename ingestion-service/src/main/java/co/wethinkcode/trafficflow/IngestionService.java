package co.wethinkcode.trafficflow;

public class IngestionService
{
    public Intersection cleanRecord(String[] rawLine)
    {
        if (rawLine == null || rawLine.length < 4) {return null;}

    String id = cleanId(rawLine[0]);
    String district = cleanDistrict(rawLine[1]);
    String signalType = cleanSignalType(rawLine[2]);
    boolean active = cleanActiveFlag(rawLine[3]);

    return new Intersection(id, district, signalType, active);
    }

    private String cleanId(String raw)
    {
        if (raw == null || raw.trim().isEmpty()) {return null;}
        return raw.trim().toUpperCase();
    }

    private String cleanDistrict(String raw)
    {
        if (raw == null || raw.trim().isEmpty()) {return null;}
        String word = raw.trim().replaceAll("\\s+", " ").toLowerCase();
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private String cleanSignalType(String raw)
    {
        if (raw == null || raw.trim().isEmpty()) {return null;}
        return raw.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private boolean cleanActiveFlag(String raw)
    {
        if (raw == null) {return false;}
        return raw.trim().equalsIgnoreCase("y");
    }
}
