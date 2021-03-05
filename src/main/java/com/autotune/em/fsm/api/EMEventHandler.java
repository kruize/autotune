package com.autotune.em.fsm.api;
public interface EMEventHandler<E extends EMEvent> {

    /**
     * Event Handler method to execute when an event occurs in the experiment manager.
     * @param event the triggered event
     * @throws Exception thrown if a problem occurs during action performing
     */
	
    void handleEvent(E event) throws Exception;

}
