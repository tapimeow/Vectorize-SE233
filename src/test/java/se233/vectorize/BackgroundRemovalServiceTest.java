package se233.vectorize;

import org.junit.jupiter.api.Test;
import se233.vectorize.service.BackgroundRemovalService;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;


import static org.junit.jupiter.api.Assertions.*;

public class BackgroundRemovalServiceTest {
    private final BackgroundRemovalService service = new BackgroundRemovalService();

    private BufferedImage redSquareOnWhite() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,20,20);
        g.setColor(Color.RED);
        g.fillRect(5,5,10,10);
        g.dispose();
        return image;
    }

    @Test void plainWithBackgroundIsRemoveButSquareIsKept() {
        BufferedImage image = redSquareOnWhite();
        boolean[] mask = service.mask(image, true);

        int corner = 0 * 20 + 0;
        int center = 10 * 20 + 10;

        assertTrue(mask[corner]);
        assertFalse(mask[center]);
    }

}
