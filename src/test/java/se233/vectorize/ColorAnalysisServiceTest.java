// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize;

import org.junit.jupiter.api.Test;
import se233.vectorize.service.ColorAnalysisService;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ColorAnalysisServiceTest {
    private final ColorAnalysisService service = new ColorAnalysisService();

    private BufferedImage threeColors() {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0,20, 20);
        g.setColor(Color.BLUE);
        g.fillRect(20, 0, 20, 20);
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 5, 5);
        g.dispose();
        return image;
    }

    private boolean[] nothingExcluded() {
        return new boolean[40 * 20];
    }

    @Test void detectsThreeColors() {
        ColorAnalysisService.Analysis analysis = service.analyze(threeColors(), nothingExcluded());
        assertEquals(3 , analysis.detectedColors().size());
    }

    @Test void palletteOfTwoHasColors() {
        ColorAnalysisService.Analysis analysis = service.analyze(threeColors(), nothingExcluded());
        assertEquals(2, analysis.palette(2).size());
    }

    @Test void askingForMoreColorsThanExistGivesOnlyRealOnes(){
        ColorAnalysisService.Analysis analysis = service.analyze(threeColors(), nothingExcluded());
        assertEquals(3, analysis.palette(5).size());

    }

    @Test void allExcludedGivesEmptyPalette() {
        boolean[] excluded = new boolean[40 * 20];
        Arrays.fill(excluded, true);
        ColorAnalysisService.Analysis analysis = service.analyze(threeColors(), excluded);
        assertTrue(analysis.palette(2).isEmpty());
    }

    @Test void sameImageGivesSamePalette() {
        List<Integer> first = service.analyze(threeColors(), nothingExcluded()).palette(3);
        List<Integer> second = service.analyze(threeColors(), nothingExcluded()).palette(3);
        assertEquals(first, second);
    }

}
