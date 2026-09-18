// Team ownership: Shared - both review changes here. See README.md "Team split".
package se233.vectorize;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every behavioral/integration test class in one pass, coursebook
 * §5.5.5 style. Each listed test already calls {@link FxTestSupport#start()}
 * itself (idempotent — guarded by an AtomicBoolean), so unlike the
 * coursebook's own example this suite does not also call
 * {@code Platform.startup(...)} in a {@code @BeforeAll}: doing that here
 * would start the JavaFX toolkit before a test's own start() call, and
 * JavaFX throws if startup() runs twice.
 */
@Suite
@SelectClasses({
        ProcessingPolicyTest.class,
        ImportAndZipTest.class,
        VectorPipelineTest.class,
        BatchFailureTest.class,
        ExportIntegrationTest.class,
        ExportAndPresetTest.class,
        ParallelExportTest.class,
        UiFlowTest.class,
        UiSnapshotTest.class
})
public class JUnitTestSuite {
}
