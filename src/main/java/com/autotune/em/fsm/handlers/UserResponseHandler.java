package com.autotune.em.fsm.handlers;

import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.events.UserResponseEvent;

public class UserResponseHandler implements EMEventHandler<UserResponseEvent>{
	
	@Override
	public void handleEvent(UserResponseEvent event) throws Exception {
		System.out.println("Respond user with useful information statistics.......lets work on this information");
		
		//TODO implement logic
	}

}
