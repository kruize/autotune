/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.dbactivites.init;


import com.autotune.utils.AnalyzerConstants;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class KruizeHibernateUtil {
    private static final SessionFactory sessionFactory;
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeHibernateUtil.class);

    static {
        SessionFactory sfTemp = null;
        try {
            String configFile = System.getenv(AnalyzerConstants.DBConstants.CONFIG_FILE) ;
            JSONObject databaseObj = null;
            try {
                InputStream is = new FileInputStream(configFile);
                String jsonTxt = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject jsonObj = new JSONObject(jsonTxt);
                databaseObj = jsonObj.getJSONObject(AnalyzerConstants.DBConstants.CONFIG_FILE_DB_KEY);
            }catch (FileNotFoundException fileNotFoundException){
                LOGGER.error("DB init failed due to {}", fileNotFoundException.getMessage());
                try{
                    databaseObj = new JSONObject();
                    for(String env : Arrays.asList(AnalyzerConstants.DBConstants.DB_DRIVER,
                            AnalyzerConstants.DBConstants.HOSTNAME,
                            AnalyzerConstants.DBConstants.PORT,
                            AnalyzerConstants.DBConstants.NAME,
                            AnalyzerConstants.DBConstants.USERNAME,
                            AnalyzerConstants.DBConstants.PASSWORD)){
                        if (null == System.getenv(env)){
                            throw new Exception("env: "+ env + " not set");
                        }else{
                            databaseObj.put(env, System.getenv(env));
                        }
                    }
                }catch(Exception e){
                    databaseObj = null;
                    LOGGER.error("DB connection failed due to {}",e.getMessage());
                    LOGGER.error("Either {} parameter or following env should be set for db integration {},{},{},{},{}",
                            AnalyzerConstants.DBConstants.CONFIG_FILE,
                            AnalyzerConstants.DBConstants.HOSTNAME,
                            AnalyzerConstants.DBConstants.PORT,
                            AnalyzerConstants.DBConstants.NAME,
                            AnalyzerConstants.DBConstants.USERNAME,
                            AnalyzerConstants.DBConstants.PASSWORD);
                }
            }catch (Exception e){
                LOGGER.error("DB connection failed due to {}",e.getMessage());
                LOGGER.error("Either {} parameter or following env should be set to proceed {},{},{},{},{}",
                        AnalyzerConstants.DBConstants.CONFIG_FILE,
                        AnalyzerConstants.DBConstants.HOSTNAME,
                        AnalyzerConstants.DBConstants.PORT,
                        AnalyzerConstants.DBConstants.NAME,
                        AnalyzerConstants.DBConstants.USERNAME,
                        AnalyzerConstants.DBConstants.PASSWORD);
            }
            if(null != databaseObj) {
                Configuration configuration = new Configuration().configure();
                String connectionURL = System.getenv(AnalyzerConstants.DBConstants.DB_DRIVER) +
                        databaseObj.getString(AnalyzerConstants.DBConstants.HOSTNAME) +
                        ":" + databaseObj.getInt(AnalyzerConstants.DBConstants.PORT) +
                        "/" + databaseObj.getString(AnalyzerConstants.DBConstants.NAME);

                configuration.setProperty("hibernate.connection.url", connectionURL);
                configuration.setProperty("hibernate.connection.username", databaseObj.getString(AnalyzerConstants.DBConstants.USERNAME));
                configuration.setProperty("hibernate.connection.password", databaseObj.getString(AnalyzerConstants.DBConstants.PASSWORD));
                sfTemp = configuration.buildSessionFactory();
                LOGGER.debug("DB build session is successful !");
            }else{
                LOGGER.debug("DB build session failed !");
            }
        } catch (Exception e) {
            LOGGER.error("DB init failed due to : {}", e.getMessage());
            e.printStackTrace();
        } finally {
            sessionFactory = sfTemp;
        }
    }

    public static Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
