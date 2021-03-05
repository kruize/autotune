package com.autotune.em.fsm.handlers;

import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.events.DeployAppConfigurationEvent;

public class DeployAppConfigurationHandler implements EMEventHandler<DeployAppConfigurationEvent>{
	
	@Override
	public void handleEvent(DeployAppConfigurationEvent event) throws Exception {
		
		System.out.println("Create the yaml which receieved configuration from the Recommendation manager.......start deploy in experiment POD");
		//TODO
	}

}
