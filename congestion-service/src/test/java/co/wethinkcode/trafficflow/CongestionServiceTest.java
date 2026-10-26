package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CongestionServiceTest {

    /**
     * Verifies that levels 0-8 are valid and out-of-range levels are rejected.
     */
    @Test
    public void testIsValidLevel_EnforcesZeroToEightBounds() {
        assertTrue(CongestionServiceApp.isValidLevel(0));
        assertTrue(CongestionServiceApp.isValidLevel(8));
        assertFalse(CongestionServiceApp.isValidLevel(-1));
        assertFalse(CongestionServiceApp.isValidLevel(9));
    }

    /**
     * Verifies that parseLevel extracts the integer from a JSON body or string.
     */
    @Test
    public void testParseLevel_ExtractsLevelFromJson() {
        assertEquals(5, CongestionServiceApp.parseLevel("{\"level\": 5}"));
        assertEquals(-1, CongestionServiceApp.parseLevel("invalid"));
    }

    /**
     * Verifies that buildLevelJson formats the level payload for ActiveMQ.
     */
    @Test
    public void testBuildLevelJson_FormatsTopicPayload() {
        assertEquals("{\"level\":4}", CongestionServiceApp.buildLevelJson(4));
    }
}