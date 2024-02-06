package com.autotune.jobs;

import com.autotune.analyzer.exceptions.K8sTypeNotSupportedException;
import com.autotune.analyzer.exceptions.DefaultDataSourceNotFoundException;
import com.autotune.database.dao.ExperimentDAOImpl;
import com.autotune.database.helper.DBConstants;
import com.autotune.database.init.KruizeHibernateUtil;
import com.autotune.operator.InitializeDeployment;
import com.autotune.utils.MetricsConfig;
import io.micrometer.core.instrument.Timer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;


public class CreatePartition {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePartition.class);

    public static void main(String[] args) {
        LOGGER.info("Checking Liveliness probe DB connection...");
        Transaction tx = null;
        String statusValue = "failure";
        Timer.Sample timerAddBulkResultsDB = Timer.start(MetricsConfig.meterRegistry());
        try {
            InitializeDeployment.setup_deployment_info();
            // create partitions
            try (Session session = KruizeHibernateUtil.getSessionFactory().openSession()) {
                tx = session.beginTransaction();
                // Get the current year and month
                YearMonth yearMonth = new ExperimentDAOImpl().buildDateForNextMonth(YearMonth.now());
                // Fixing the partition type to 'by_month'
                new ExperimentDAOImpl().addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RESULTS, String.format("%02d", yearMonth.getMonthValue()), String.valueOf(yearMonth.getYear()), 1, DBConstants.PARTITION_TYPES.BY_MONTH);
                new ExperimentDAOImpl().addPartitions(DBConstants.TABLE_NAMES.KRUIZE_RECOMMENDATIONS, String.format("%02d", yearMonth.getMonthValue()), String.valueOf(yearMonth.getYear()), 1, DBConstants.PARTITION_TYPES.BY_MONTH);
                statusValue = "success";
                tx.commit();
                LOGGER.info("Partition creation successful!");
            } catch (Exception partitionException) {
                LOGGER.error(partitionException.getMessage());
                tx.commit();
            } finally {
                if (null != timerAddBulkResultsDB) {
                    MetricsConfig.timerAddBulkResultsDB = MetricsConfig.timerBAddBulkResultsDB.tag("status", statusValue).register(MetricsConfig.meterRegistry());
                    timerAddBulkResultsDB.stop(MetricsConfig.timerAddBulkResultsDB);
                }
            }
        } catch (Exception | K8sTypeNotSupportedException | DefaultDataSourceNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        LOGGER.info("DB Liveliness probe connection successful!");
    }
}
