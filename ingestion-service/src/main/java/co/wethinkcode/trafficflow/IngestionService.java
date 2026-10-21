package co.wethinkcode.trafficflow;

import java.util.Set;

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
        if (isPlaceholder(raw)) {return null;}
        return raw.trim().toUpperCase();
    }

    private String cleanDistrict(String raw)
    {
        if (isPlaceholder(raw)) {return null;}
        String word = raw.trim().replaceAll("\\s+", " ").toLowerCase();
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    private String cleanSignalType(String raw)
    {
        if (isPlaceholder(raw)) {return null;}
        return raw.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static final Set<String> TRUE_FLAGS = Set.of("y", "yes", "1", "true");

    private boolean cleanActiveFlag(String raw)
    {
        if (raw == null) {return false;}
        String flag = raw.trim().toLowerCase();
        return TRUE_FLAGS.contains(flag);
    }

    private static final Set<String> PLACEHOLDERS = Set.of("", "n/a", "tbd", "unknown", "-", "nan");

    private boolean isPlaceholder(String value)
    {
        if (value == null) {return true;}
        return PLACEHOLDERS.contains(value.trim().toLowerCase());
    }
}
