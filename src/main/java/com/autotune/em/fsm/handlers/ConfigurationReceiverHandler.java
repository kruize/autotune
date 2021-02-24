package com.autotune.em.fsm.handlers;

import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.events.RecommendedConfigReceiveEvent;
import com.autotune.queue.AutotuneDTO;

public class ConfigurationReceiverHandler implements EMEventHandler<RecommendedConfigReceiveEvent>{
	
	@Override
	public void handleEvent(RecommendedConfigReceiveEvent event) throws Exception {
		
		AutotuneDTO data = event.getData();
		// Received the configuration from the recommendation manager and process 
		
		
		
		//TODO read the json
		System.out.println("Receive confiruation from the Recommendation manager.......lets work on this information");
	}
	

}
