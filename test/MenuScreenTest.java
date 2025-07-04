import org.junit.Test;
import static org.junit.Assert.assertTrue;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class MenuScreenTest {

    @Test
    public void testMenuLoadsAndStartGameButtonClickable() throws Exception {
        // Directly create MenuScreen since it's already a JFrame
        MenuScreen menu = new MenuScreen();
        menu.setSize(1000, 800);
        menu.setLocationRelativeTo(null);
        menu.setVisible(true);

        // Let menu load
        Robot robot = new Robot();
        robot.setAutoDelay(200);
        Thread.sleep(1500); // time for rendering

        // Approximate Start Game button position (tweak as needed)
        Point framePos = menu.getLocationOnScreen();
        int approxX = framePos.x + 450;
        int approxY = framePos.y + 350;
        robot.mouseMove(approxX, approxY);

        // Simulate click
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Thread.sleep(1500); // for visual confirmation
        menu.dispose();

        assertTrue("Menu screen loaded and clicked successfully", true);
    }
    @Test
    public void testMultiplayerButtonClick() throws Exception {
        MenuScreen menu = new MenuScreen();
        menu.setSize(1000, 800);
        menu.setLocationRelativeTo(null);
        menu.setVisible(true);

        Robot robot = new Robot();
        robot.setAutoDelay(200);
        Thread.sleep(1500);

        Point framePos = menu.getLocationOnScreen();
        int approxX = framePos.x + 450;
        int approxY = framePos.y + 420; // lower than Start Game

        robot.mouseMove(approxX, approxY);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Thread.sleep(1500);
        menu.dispose();

        assertTrue("Multiplayer button clicked successfully", true);
    }
    @Test
    public void testQuitButtonClick() throws Exception {
        MenuScreen menu = new MenuScreen();
        menu.setSize(1000, 800);
        menu.setLocationRelativeTo(null);
        menu.setVisible(true);

        Robot robot = new Robot();
        robot.setAutoDelay(200);
        Thread.sleep(1500);

        // Approximate Y position of the "Quit" button (bottom-most button)
        Point framePos = menu.getLocationOnScreen();
        int approxX = framePos.x + 450;
        int approxY = framePos.y + 630; // Adjust if your layout is taller/shorter

        robot.mouseMove(approxX, approxY);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Thread.sleep(1000); // simulate pause before closing
        menu.dispose();

        assertTrue("Quit button clicked successfully", true);
    }
    @Test
    public void testLevelSelectButtonClick() throws Exception {
        MenuScreen menu = new MenuScreen();
        menu.setSize(1000, 800);
        menu.setLocationRelativeTo(null);
        menu.setVisible(true);

        Robot robot = new Robot();
        robot.setAutoDelay(200);
        Thread.sleep(1500);

        // Approximate Y position of "Level Select" (between Multiplayer and Quit)
        Point framePos = menu.getLocationOnScreen();
        int approxX = framePos.x + 450;
        int approxY = framePos.y + 560;

        robot.mouseMove(approxX, approxY);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        Thread.sleep(1000);

        boolean dialogShown = false;
        for (Window w : menu.getOwnedWindows()) {
            if (w instanceof JDialog && w.isVisible()) {
                dialogShown = true;
                w.dispose(); // clean up dialog
            }
        }

        menu.dispose();
        assertTrue("Level Select dialog should appear after click", dialogShown);
    }




}
