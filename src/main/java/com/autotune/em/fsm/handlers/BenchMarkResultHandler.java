package com.autotune.em.fsm.handlers;

import com.autotune.em.fsm.api.EMEventHandler;
import com.autotune.em.fsm.events.BenchMarkResultEvent;

public class BenchMarkResultHandler implements EMEventHandler<BenchMarkResultEvent>{
	
	@Override
	public void handleEvent(BenchMarkResultEvent event) throws Exception {
		System.out.println("Bench mark the result receieved from the prometheous from the after apply new configuration.......lets work on this information");
		
		//TODO implement the logic here.
	}

}
