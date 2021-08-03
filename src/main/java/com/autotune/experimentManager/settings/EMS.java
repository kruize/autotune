/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
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

package com.autotune.experimentManager.settings;

/*
 * class name : EMS (Experiment Manager Settings)
 *
 * EMS is a singleton class which is used to store the settings
 * of the Experiment Manager
 *
 * The EMS object is returned by calling the `getController` function
 *
 * It holds settings like:
 *  - Max Executors Size
 *  - Current Executors Size
 */
public class EMS {

    /*
     *  static variable of type EMS initialized to null
     *  later set to a new EMS instance on the first call
     *  to the getController method
     */
    private static EMS ems = null;

    /*
     *  Maximum number of worker threads
     */
    private int maxExecutors;

    /*
     *  Current number of worker threads to run in parallel
     */
    private int currentExecutors;

    /*
     *  private constructor to make sure that we cannot create the
     *  object for EMS externally
     *
     *  Sets the defaults values for the variables
     */
    private EMS() {
        maxExecutors = 32072;
        currentExecutors = 256;
    }

    /*
     *  method name : getController
     *  return type : EMS object
     *  description : on calling this methods it checks if the `ems` object is null
     *  and if yes it creates a new instance to EMS class and returns it. Subsequent calls
     *  to this function returns the already created EMS instance
     */
    public static EMS getController() {
        if (null == ems) {
            ems = new EMS();
        }
        return ems;
    }

    public int getMaxExecutors() {
        return maxExecutors;
    }

    public void setMaxExecutors(int maxExecutors) {
        this.maxExecutors = maxExecutors;
    }

    public int getCurrentExecutors() {
        return currentExecutors;
    }

    public void setCurrentExecutors(int currentExecutors) {
        this.currentExecutors = currentExecutors;
    }

}
