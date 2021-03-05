package com.autotune.em.fsm.events;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.queue.AutotuneDTO;

public class GatherMatrixEvent extends EMAbstractEvent {
	
	private AutotuneDTO data;

	public GatherMatrixEvent(AutotuneDTO data) {
		super("GatherMatrixEvent");
		this.data = data;
	}
	
	public GatherMatrixEvent(String name) {
		super(name);
	}

	@Override
	public AutotuneDTO getData() {
		return data;
	}

}
