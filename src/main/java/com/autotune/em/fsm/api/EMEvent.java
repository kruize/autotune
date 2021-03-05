package com.autotune.em.fsm.api;

import com.autotune.queue.AutotuneDTO;

public interface EMEvent {

	/**
	 * Name of the experiment manager event.
	 * 
	 * @return event name of experiment manager
	 */
	String getEMEventName();

	/**
	 * Timestamp of the event.
	 * 
	 * @return event timestamp
	 */
	long getEMEventTimestamp();
	
	AutotuneDTO getData();

}
