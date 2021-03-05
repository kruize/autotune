package com.autotune.em.fsm.events;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.queue.AutotuneDTO;

public class ReceiveLoadEvent extends EMAbstractEvent {
	
	private AutotuneDTO data;
	
	public ReceiveLoadEvent(AutotuneDTO data) {
		super("ReceiveLoadEvent");
		this.data = data;
	}
	
	public ReceiveLoadEvent(String name) {
		super(name);
	}

	@Override
	public AutotuneDTO getData() {
		
		return data;
	}

}
