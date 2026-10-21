package co.wethinkcode.trafficflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IngestionServiceTest
{
    private IngestionService service;

    @BeforeEach
    public void setUp()
    {
        service = new IngestionService();
    }

    @Test
    public void testCleanSingleRecord_TrimmingAndCasing()
    {
        String[] rawLine = {"INT-1001", " Downtown", "4-way", "Y"};

        Intersection result = service.cleanRecord(rawLine);

        assertEquals("INT-1001", result.getId(), "ID should be uppercase and trimmed");
        assertEquals("Downtown", result.getDistrict(), "District should be trimmed and Title Cased");
        assertEquals("4-way", result.getSignalType(), "Signal type should be lowercase and trimmed");
        assertTrue(result.isActive(), "Y should be converted to true");
    }

    @Test
    public void testCleanSingleRecord_PlaceholdersBecomeNull()
    {
        String[] rawLine = {"INT-1004", "unknown", "", "Y"};

        Intersection result = service.cleanRecord(rawLine);

        assertEquals("INT-1004", result.getId());
        assertNull(result.getDistrict(), "Placeholder 'unknown' should become null");
        assertNull(result.getSignalType(), "Empty string should become null");
    }
}