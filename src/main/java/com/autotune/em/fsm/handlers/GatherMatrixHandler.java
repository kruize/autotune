package com.autotune.em.fsm.handlers;

import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.events.GatherMatrixEvent;

public class GatherMatrixHandler implements EMEventHandler<GatherMatrixEvent>{
	
	@Override
	public void handleEvent(GatherMatrixEvent event) throws Exception {
		System.out.println("Connect to the Matrix tool like Prometheous and get matrix.......lets work on this information");
	}

}
