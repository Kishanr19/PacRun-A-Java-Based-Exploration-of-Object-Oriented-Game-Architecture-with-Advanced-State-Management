import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class MainApplicationTest {

    @Test
    public void testMainApplicationLaunchesMenu() {
        try {
            // Run the main method (which should open MenuScreen)
            MainApplication.main(new String[0]);

            // Just pass the test if no exception occurs
            assertTrue("MainApplication launched without errors", true);

        } catch (Exception e) {
            e.printStackTrace();
            assertTrue("MainApplication threw an exception: " + e.getMessage(), false);
        }
    }
}
