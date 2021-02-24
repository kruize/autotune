package com.autotune.queue;

import java.util.concurrent.Callable;

public class EventConsumer implements Callable<AutotuneDTO> {
	private final AutotuneQueue queue;
	
    public EventConsumer(AutotuneQueue queue) {
        this.queue = queue;
    }

	@Override
	public AutotuneDTO call() throws Exception {
		AutotuneDTO taskDto=null;
        try {
        	taskDto = queue.get();
        	System.out.println("Get item from queue: task id: " + taskDto.getId() + " time in nano: " + System.nanoTime());
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		return taskDto;
	}
}
