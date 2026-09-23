// Team ownership: Day (Backend/Logic). See README.md "Team split".
package se233.vectorize.model;

import java.util.ArrayList;
import java.util.List;

public final class AppSession {
    private final List<ImageDocument> documents = new ArrayList<>();
    private int selectedIndex;

    public List<ImageDocument> documents() {
        return List.copyOf(documents);
    }
    public void addAll(List<ImageDocument> values) {
        documents.addAll(values);
    }
    public boolean isEmpty() {
        return documents.isEmpty();
    }
    public int size() {
        return documents.size();
    }
    public int selectedIndex() {
        return selectedIndex;
    }
    public ImageDocument selected() {
        return documents.get(selectedIndex);
    }
    /** Phase 5, FR-006: report whether navigation toward the first document is
     * possible. Test empty, first, middle, and last positions. FX thread only. */
    public boolean canBack() {
        return selectedIndex > 0;
    }
    /** Phase 5, FR-006: report whether another document follows the selection. */
    public boolean canNext() {
        return selectedIndex < documents.size() - 1;
    }
    /** Phase 5, FR-006: move selection without losing that image's settings or
     * cached preview. Do not move past a boundary; test navigation round-trips. */
    public void back() {
        if(canBack()) {
            selectedIndex--;
        } else {
            throw new IllegalStateException("Cannot navigate back from the first document");
        }
    }
    /** Phase 5, FR-006: symmetric forward navigation; controller redraws on FX. */
    public void next() {
        if(canNext()) {
            selectedIndex++;
        } else {
            throw new IllegalStateException("Cannot navigate forward from the last document");
        }
    }
}
