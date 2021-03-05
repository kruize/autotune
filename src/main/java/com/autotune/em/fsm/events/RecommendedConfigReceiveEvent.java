package com.autotune.em.fsm.events;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.queue.AutotuneDTO;

public class RecommendedConfigReceiveEvent extends EMAbstractEvent {
	
	private AutotuneDTO data;
	
	public RecommendedConfigReceiveEvent(AutotuneDTO data) {
		super("ConfigurationReceiveEvent");
		this.data = data;
		
	}
	
	public RecommendedConfigReceiveEvent(String name) {
		super(name);
	}

	@Override
	public AutotuneDTO getData() {
		return this.data;
	}

}
