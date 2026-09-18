package se233.vectorize.model;

import java.util.ArrayList;
import java.util.List;

public final class AppSession {
    private final List<ImageDocument> documents = new ArrayList<>();
    private int selectedIndex;

    public List<ImageDocument> documents() { return List.copyOf(documents); }
    public void addAll(List<ImageDocument> values) { documents.addAll(values); }
    public boolean isEmpty() { return documents.isEmpty(); }
    public int size() { return documents.size(); }
    public int selectedIndex() { return selectedIndex; }
    public ImageDocument selected() { return documents.get(selectedIndex); }
    /** Phase 5, FR-006: report whether navigation toward the first document is
     * possible. Test empty, first, middle, and last positions. FX thread only. */
    public boolean canBack() { throw new UnsupportedOperationException("TODO Phase 5: Back bounds"); }
    /** Phase 5, FR-006: report whether another document follows the selection. */
    public boolean canNext() { throw new UnsupportedOperationException("TODO Phase 5: Next bounds"); }
    /** Phase 5, FR-006: move selection without losing that image's settings or
     * cached preview. Do not move past a boundary; test navigation round-trips. */
    public void back() { throw new UnsupportedOperationException("TODO Phase 5: Back navigation"); }
    /** Phase 5, FR-006: symmetric forward navigation; controller redraws on FX. */
    public void next() { throw new UnsupportedOperationException("TODO Phase 5: Next navigation"); }
}
