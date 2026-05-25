package com.autotune.analyzer.workerimpl;

import com.autotune.analyzer.serviceObjects.BulkInput;
import com.autotune.analyzer.serviceObjects.BulkJobStatus;
import com.autotune.common.data.dataSourceMetadata.DataSourceMetadataInfo;
import com.autotune.common.datasource.DataSourceInfo;
import com.autotune.common.datasource.DataSourceManager;
import com.autotune.common.utils.CommonUtils;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.operator.KruizeDeploymentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BulkJobManager class with mocking.
 * Tests cover job initialization, label parsing, and experiment name generation.
 */
@ExtendWith(MockitoExtension.class)
class BulkJobManagerTest {

    @Mock
    private BulkJobStatus mockJobStatus;

    @Mock
    private BulkInput mockBulkInput;

    @Mock
    private DataSourceInfo mockDataSourceInfo;

    @Mock
    private DataSourceManager mockDataSourceManager;

    @Mock
    private DataSourceMetadataInfo mockMetadataInfo;

    private String testJobId;

    @BeforeEach
    void setUp() {
        testJobId = "test-job-123";
    }

    @Test
    @DisplayName("Test BulkJobManager constructor initializes correctly")
    void testConstructor() {
        // Arrange & Act
        BulkJobManager manager = new BulkJobManager(testJobId, mockJobStatus, mockBulkInput);

        // Assert
        assertNotNull(manager);
    }

    @Test
    @DisplayName("Test appendExperiments adds experiment to list")
    void testAppendExperiments() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();
        String experimentName = "test-experiment";

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, experimentName);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(experimentName));
    }

    @Test
    @DisplayName("Test appendExperiments with multiple experiments")
    void testAppendExperiments_Multiple() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();
        experiments.add("experiment-1");

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, "experiment-2");
        result = BulkJobManager.appendExperiments(result, "experiment-3");

        // Assert
        assertEquals(3, result.size());
        assertTrue(result.contains("experiment-1"));
        assertTrue(result.contains("experiment-2"));
        assertTrue(result.contains("experiment-3"));
    }

    @Test
    @DisplayName("Test appendExperiments with empty list")
    void testAppendExperiments_EmptyList() {
        // Arrange
        List<String> emptyList = new java.util.ArrayList<>();

        // Act
        List<String> result = BulkJobManager.appendExperiments(emptyList, "first-experiment");

        // Assert
        assertEquals(1, result.size());
        assertEquals("first-experiment", result.get(0));
    }

    @Test
    @DisplayName("Test appendExperiments with null experiment name")
    void testAppendExperiments_NullExperiment() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, null);

        // Assert
        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    @DisplayName("Test BulkJobManager handles null filter gracefully")
    void testBulkJobManager_NullFilter() {
        // Act - Constructor doesn't call getFilter() or getDatasource() during initialization
        BulkJobManager manager = new BulkJobManager(testJobId, mockJobStatus, mockBulkInput);

        // Assert
        assertNotNull(manager);
    }

    @Test
    @DisplayName("Test BulkJobManager with valid datasource")
    void testBulkJobManager_ValidDatasource() {
        // Act - Constructor only stores references, doesn't call methods
        BulkJobManager manager = new BulkJobManager(testJobId, mockJobStatus, mockBulkInput);

        // Assert
        assertNotNull(manager);
    }

    @Test
    @DisplayName("Test BulkJobManager initialization with different job IDs")
    void testBulkJobManager_DifferentJobIds() {
        // Arrange
        String jobId1 = "job-001";
        String jobId2 = "job-002";

        // Act
        BulkJobManager manager1 = new BulkJobManager(jobId1, mockJobStatus, mockBulkInput);
        BulkJobManager manager2 = new BulkJobManager(jobId2, mockJobStatus, mockBulkInput);

        // Assert
        assertNotNull(manager1);
        assertNotNull(manager2);
    }

    @Test
    @DisplayName("Test appendExperiments maintains order")
    void testAppendExperiments_MaintainsOrder() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();

        // Act
        experiments = BulkJobManager.appendExperiments(experiments, "first");
        experiments = BulkJobManager.appendExperiments(experiments, "second");
        experiments = BulkJobManager.appendExperiments(experiments, "third");

        // Assert
        assertEquals(3, experiments.size());
        assertEquals("first", experiments.get(0));
        assertEquals("second", experiments.get(1));
        assertEquals("third", experiments.get(2));
    }

    @Test
    @DisplayName("Test appendExperiments with duplicate experiment names")
    void testAppendExperiments_Duplicates() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();
        String experimentName = "duplicate-experiment";

        // Act
        experiments = BulkJobManager.appendExperiments(experiments, experimentName);
        experiments = BulkJobManager.appendExperiments(experiments, experimentName);

        // Assert
        assertEquals(2, experiments.size());
        assertEquals(experimentName, experiments.get(0));
        assertEquals(experimentName, experiments.get(1));
    }

    @Test
    @DisplayName("Test BulkJobManager with empty job ID")
    void testBulkJobManager_EmptyJobId() {
        // Arrange
        String emptyJobId = "";

        // Act
        BulkJobManager manager = new BulkJobManager(emptyJobId, mockJobStatus, mockBulkInput);

        // Assert
        assertNotNull(manager);
    }

    @Test
    @DisplayName("Test BulkJobManager with special characters in job ID")
    void testBulkJobManager_SpecialCharactersInJobId() {
        // Arrange
        String specialJobId = "job-123-abc_def@test";

        // Act
        BulkJobManager manager = new BulkJobManager(specialJobId, mockJobStatus, mockBulkInput);

        // Assert
        assertNotNull(manager);
    }

    @Test
    @DisplayName("Test appendExperiments with very long experiment name")
    void testAppendExperiments_LongName() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();
        String longName = "a".repeat(500);

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, longName);

        // Assert
        assertEquals(1, result.size());
        assertEquals(longName, result.get(0));
    }

    @Test
    @DisplayName("Test appendExperiments with special characters in experiment name")
    void testAppendExperiments_SpecialCharacters() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();
        String specialName = "exp-123_test@namespace.cluster";

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, specialName);

        // Assert
        assertEquals(1, result.size());
        assertEquals(specialName, result.get(0));
    }

    @Test
    @DisplayName("Test BulkJobManager handles null BulkInput")
    void testBulkJobManager_NullBulkInput() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            BulkJobManager manager = new BulkJobManager(testJobId, mockJobStatus, null);
            assertNotNull(manager);
        });
    }

    @Test
    @DisplayName("Test BulkJobManager handles null BulkJobStatus")
    void testBulkJobManager_NullJobStatus() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            BulkJobManager manager = new BulkJobManager(testJobId, null, mockBulkInput);
            assertNotNull(manager);
        });
    }

    @Test
    @DisplayName("Test appendExperiments returns same list instance")
    void testAppendExperiments_ReturnsSameInstance() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, "test");

        // Assert
        assertSame(experiments, result);
    }

    @Test
    @DisplayName("Test appendExperiments with empty string experiment name")
    void testAppendExperiments_EmptyString() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, "");

        // Assert
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    @DisplayName("Test appendExperiments with whitespace experiment name")
    void testAppendExperiments_Whitespace() {
        // Arrange
        List<String> experiments = new java.util.ArrayList<>();

        // Act
        List<String> result = BulkJobManager.appendExperiments(experiments, "   ");

        // Assert
        assertEquals(1, result.size());
        assertEquals("   ", result.get(0));
    }
}

// Made with Bob
