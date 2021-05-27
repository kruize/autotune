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

package com.autotune.experimentmanager.fsm.handlers;

import com.autotune.experimentmanager.fsm.api.EMEventHandler;
import com.autotune.experimentmanager.fsm.events.DeployAppConfigurationEvent;
import com.autotune.experimentmanager.fsm.object.ExperimentTrialObject;
import com.autotune.experimentmanager.utils.EMUtils;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
/**
 * This will create a new deployment using recommended configuration.
 * @author Bipin Kumar
 *
 * @Date Mar 30, 2021
 */
public class DeployAppConfigurationHandler implements EMEventHandler<DeployAppConfigurationEvent>{
	
	@Override
	public void handleEvent(DeployAppConfigurationEvent event) throws Exception {
		System.out.println("Starting the deployment...");
		ExperimentTrialObject data = event.getData();
		Deployment trialDeployment = data.getTrialDeployment();
		KubernetesClient client = new DefaultKubernetesClient();
		
		String appName = data.getDeploymentName() + "-" + EMUtils.NEW_DEPLOYMENT_NAME_SUFIX +"-" + data.getTrialNumber();
		
		trialDeployment.getMetadata().setName(appName);
		System.out.println("new app name: "+ appName);
		Deployment createdDeployment = client.apps().deployments().inNamespace(EMUtils.NAMESPACE).createOrReplace(trialDeployment);
		data.setTrialDeployment(createdDeployment);
		System.out.println("new deployment of name=" + createdDeployment.getMetadata().getName() + " created successfully");
		
	}

}
