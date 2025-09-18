package com.autotune.jobs;

import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotFoundException;
import com.autotune.analyzer.exceptions.MonitoringAgentNotSupportedException;
import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.InitializeDeployment;
import com.autotune.operator.KruizeDeploymentInfo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public class PrepareDB {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDB.class);

    public static void main(String[] args) {
        LOGGER.info("Preparing the Kruize DB ...");
        try {
            InitializeDeployment.setup_deployment_info();
            // Read and execute the DDLs here
            if (KruizeDeploymentInfo.is_ros_enabled) {
                executeDDLs(KruizeDeploymentInfo.prep_db);
            }
            // close the session factory
            KruizeHibernateUtil.closeSessionFactory();

        } catch (Exception | K8sTypeNotSupportedException | MonitoringAgentNotSupportedException |
                 MonitoringAgentNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Accepts either a single file name (old behavior) or a directory name.
     * If directory: recursively collects all .sql files and sorts them by numeric version
     *   extracted from names like V001__desc.sql (falls back to lexicographic if not found).
     * Splits SQL on semicolon+newline and executes statements one by one.     *
     */
    public static void executeDDLs(String ddlPathOrFile) throws Exception {
        LOGGER.info("Reading Kruize DB configuration from {}", ddlPathOrFile);
        Logger LOGGER = LoggerFactory.getLogger(PrepareDB.class);
        SessionFactory factory = KruizeHibernateUtil.getSessionFactory();
        Session session = null;

        // TARGET/MIGRATIONS/<ddlPathOrFile>
        Path basePath = Paths.get(AnalyzerConstants.TARGET, AnalyzerConstants.MIGRATIONS, ddlPathOrFile);

        try {
            // collect files: single file or all .sql files under directory
            List<Path> sqlFiles = new ArrayList<>();
            if (Files.notExists(basePath)) {
                throw new IllegalArgumentException("DDL path does not exist: " + basePath);
            }

            if (Files.isRegularFile(basePath)) {
                sqlFiles.add(basePath);
            } else if (Files.isDirectory(basePath)) {
                try (Stream<Path> walk = Files.walk(basePath)) {
                    sqlFiles = walk
                            .filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".sql"))
                            .collect(Collectors.toList());
                }
                // sort files by extracted numeric version if possible, otherwise by filename
                sqlFiles.sort((p1, p2) -> {
                    Integer v1 = extractVersion(p1.getFileName().toString());
                    Integer v2 = extractVersion(p2.getFileName().toString());
                    if (v1 != null && v2 != null) {
                        return Integer.compare(v1, v2);
                    } else if (v1 != null) {
                        return -1;
                    } else if (v2 != null) {
                        return 1;
                    } else {
                        return p1.getFileName().toString().compareTo(p2.getFileName().toString());
                    }
                });
            } else {
                throw new IllegalArgumentException("Provided path is not a file or directory: " + basePath);
            }

            if (sqlFiles.isEmpty()) {
                LOGGER.info("No SQL files found at path: {}", basePath);
                return;
            }

            session = factory.openSession();

            for (Path sqlFilePath : sqlFiles) {
                LOGGER.info("Applying SQL file: {}", sqlFilePath.toString());
                String sqlStatement = Files.readString(sqlFilePath, StandardCharsets.UTF_8);
                Transaction transaction = session.beginTransaction();
                String trimmed = sqlStatement.trim();
                // skip blank and comment-only statements
                if (trimmed.isEmpty()
                        || trimmed.startsWith("--")
                        || trimmed.startsWith("#")
                        || trimmed.startsWith("/*")) {
                    continue;
                }

                try {
                    session.createNativeQuery(trimmed).executeUpdate();
                } catch (Exception e) {
                    String msg = Optional.ofNullable(e.getMessage()).orElse("");
                    if (msg.contains(DBConstants.DB_MESSAGES.ADD_CONSTRAINT)) {
                        LOGGER.warn("sql: {} failed due to : {}{}", trimmed, DBConstants.DB_MESSAGES.ADD_CONSTRAINT, DBConstants.DB_MESSAGES.DUPLICATE_DB_OPERATION);
                    } else if (msg.contains(DBConstants.DB_MESSAGES.ADD_COLUMN)) {
                        LOGGER.warn("sql: {} failed due to : {}{}", trimmed, DBConstants.DB_MESSAGES.ADD_COLUMN, DBConstants.DB_MESSAGES.DUPLICATE_DB_OPERATION);
                    } else {
                        LOGGER.error("sql: {} failed due to : {}", trimmed, msg);
                    }

                    // Commit current transaction and begin a new one
                    try {
                        transaction.commit();
                    } catch (Exception ignore) {
                    }
                    transaction = session.beginTransaction();
                }

                // commit file-level transaction
                try {
                    transaction.commit();
                } catch (Exception e) {
                    LOGGER.error("Failed to commit transaction after processing file {}: {}", sqlFilePath, e.getMessage());
                }
            }

            LOGGER.info(DBConstants.DB_MESSAGES.DB_CREATION_SUCCESS);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while trying to read the DDL(s): {}", e.getMessage());
            throw new Exception(e);
        } finally {
            if (session != null) session.close();
        }

        LOGGER.info(DBConstants.DB_MESSAGES.DB_LIVELINESS_PROBE_SUCCESS);
    }

    /**
     * Extract numeric version from filename patterns like V001__desc.sql or V1__desc.sql
     * Returns Integer value or null if not present.
     */
    private static Integer extractVersion(String filename) {
        if (filename == null) return null;
        Pattern p = Pattern.compile("^V0*([0-9]+)__.*", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(filename);
        if (m.matches()) {
            try {
                return Integer.valueOf(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
