package com.autotune.em.fsm.events;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.queue.AutotuneDTO;

public class BenchMarkResultEvent extends EMAbstractEvent {
	
	private AutotuneDTO data;

	public BenchMarkResultEvent(AutotuneDTO data) {
		super("BenchMarkResultEvent");
		this.data = data;
	}
	
	public BenchMarkResultEvent(String name) {
		super(name);
	}

	@Override
	public AutotuneDTO getData() {
		return data;
	}

}
