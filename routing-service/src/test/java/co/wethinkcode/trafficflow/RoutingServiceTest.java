package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoutingServiceTest {

    /**
     * Verifies that travel time increases as congestion level increases.
     */
    @Test
    public void testCalculateTravelTime_IncreasesWithCongestion() {
        assertEquals(10.0, RoutingServiceApp.calculateTravelTime(0));
        assertEquals(20.0, RoutingServiceApp.calculateTravelTime(4));
        assertEquals(30.0, RoutingServiceApp.calculateTravelTime(8));
    }

    /**
     * Verifies that extractDigits parses the congestion level from JSON.
     */
    @Test
    public void testExtractDigits_ParsesLevelFromJson() {
        assertEquals(3, RoutingServiceApp.extractDigits("{\"level\":3}"));
        assertEquals(0, RoutingServiceApp.extractDigits(""));
    }

    /**
     * Verifies that isMissing detects null or blank query parameters.
     */
    @Test
    public void testIsMissing_ChecksNullAndBlankParams() {
        assertTrue(RoutingServiceApp.isMissing(null));
        assertTrue(RoutingServiceApp.isMissing("   "));
        assertFalse(RoutingServiceApp.isMissing("INT-1001"));
    }

    /**
     * Verifies that incoming topic message text updates the cached congestion level.
     */
    @Test
    public void testUpdateCongestionFromMessage_UpdatesCachedLevel() {
        RoutingServiceApp.updateCongestionFromMessage("{\"level\":6}");
        assertEquals(6, RoutingServiceApp.getCurrentCongestionLevel());
    }
}