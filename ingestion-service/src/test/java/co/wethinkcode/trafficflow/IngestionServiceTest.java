package co.wethinkcode.trafficflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    @Test
    public void testCleanSingleRecord_BooleanFlagVariations()
    {
        Intersection yesRow = service.cleanRecord(new String[]{"INT-1002", "Midtown", "4-way", "yes"});
        Intersection oneRow = service.cleanRecord(new String[]{"INT-1007", "Eastside", "4-way", "1"});
        Intersection zeroRow = service.cleanRecord(new String[]{"INT-1003", "Downtown", "4-way", "0"});
        Intersection falseRow = service.cleanRecord(new String[]{"INT-1009", "Westside", "4-way", "FALSE"});

        assertTrue(yesRow.isActive(), "'yes' should be true");
        assertTrue(oneRow.isActive(), "'1' should be true");
        assertFalse(zeroRow.isActive(), "'0' should be false");
        assertFalse(falseRow.isActive(), "'FALSE' should be false");
    }

    @Test
    public void testCleanCsv_RemovesDuplicates()
    {
        InputStream stream = getClass().getResourceAsStream("/intersections-legacy.csv");
        List<Intersection> cleanedList = service.cleanCsv(stream);

        Set<String> seenIds = new HashSet<>();
        for (Intersection item : cleanedList)
        {
            boolean isNewId = seenIds.add(item.getId());
            assertTrue(isNewId, "Duplicate ID found in cleaned list: " + item.getId());
        }
    }

    @Test
    public void testToJsonArray_FormatsIntersectionsAsJson()
    {
        Intersection item = new Intersection("INT-1001", "Downtown", "4-way", true);
        String json = IngestionServiceApp.toJsonArray(List.of(item));

        assertEquals("[{\"id\":\"INT-1001\",\"district\":\"Downtown\",\"signalType\":\"4-way\",\"active\":true}]", json);
    }
}