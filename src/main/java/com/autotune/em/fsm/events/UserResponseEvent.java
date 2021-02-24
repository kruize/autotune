package com.autotune.em.fsm.events;

import com.autotune.em.fsm.api.EMAbstractEvent;
import com.autotune.queue.AutotuneDTO;

public class UserResponseEvent extends EMAbstractEvent {
	
	private AutotuneDTO data;
	
	public UserResponseEvent(AutotuneDTO data) {
		super("UserResponseEvent");
		this.data = data;
	}
	
	public UserResponseEvent(String name) {
		super(name);
	}

	@Override
	public AutotuneDTO getData() {
		return data;
	}

}
