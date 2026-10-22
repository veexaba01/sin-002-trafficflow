package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntersectionServiceTest
{
    /**
     * Verifies that known intersection IDs return true and unknown IDs return false.
     */
    @Test
    public void testIsValidIntersectionId_ChecksForKnownAndUnknownIds()
    {
        String sampleJson = "[{\"id\":\"INT-1001\",\"district\":\"Downtown\"}]";

        assertTrue(IntersectionServiceApp.isValidIntersectionId(sampleJson, "int-1001"));
        assertFalse(IntersectionServiceApp.isValidIntersectionId(sampleJson, "INT-9999"));
    }
}
