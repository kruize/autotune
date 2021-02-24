package com.autotune.em.fsm.handlers;

import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.events.ReceiveLoadEvent;

public class RecieveLoadHandler implements EMEventHandler<ReceiveLoadEvent>{
	
	@Override
	public void handleEvent(ReceiveLoadEvent event) throws Exception {
		System.out.println("Receive load from the monitoring MS.......lets work on this information");
	}

}
