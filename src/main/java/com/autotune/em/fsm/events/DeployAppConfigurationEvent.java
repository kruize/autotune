package com.autotune.em.fsm.events;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.queue.AutotuneDTO;

public class DeployAppConfigurationEvent extends EMAbstractEvent {
	
	private AutotuneDTO data;

	public DeployAppConfigurationEvent(AutotuneDTO data) {
		super("DeployConfigurationEvent");
		this.data = data;
	}
	
	public DeployAppConfigurationEvent(String name) {
		super(name);
	}

	@Override
	public AutotuneDTO getData() {
		return data;
	}

}
