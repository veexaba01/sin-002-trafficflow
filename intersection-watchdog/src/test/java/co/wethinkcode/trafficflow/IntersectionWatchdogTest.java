package co.wethinkcode.trafficflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntersectionWatchdogTest {

    /**
     * Verifies that a missed or timed-out heartbeat triggers an alert.
     */
    @Test
    public void testIsHeartbeatMissed_DetectsTimeoutAndHealthyStates() {
        assertTrue(IntersectionWatchdogApp.isHeartbeatMissed(0, 5_000));
        assertFalse(IntersectionWatchdogApp.isHeartbeatMissed(1_000, 5_000));
        assertTrue(IntersectionWatchdogApp.isHeartbeatMissed(1_000, 15_000));
    }
}