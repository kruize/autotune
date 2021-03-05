package com.autotune.queue;

import java.util.concurrent.Callable;

public class EventProducer implements Callable {
	
	private final AutotuneQueue queue;
	private final AutotuneDTO inputDTO;
	
	public EventProducer(AutotuneQueue queue, AutotuneDTO inputData) {
		this.queue = queue;
		this.inputDTO = inputData;
	}

	
	@Override
	public Object call() {
			boolean wasAdded=false;
			try {
				wasAdded = queue.send(inputDTO);
				
					
			} catch (InterruptedException e) {
				
				System.out.println("Adding Thread was interrupted");
				
			} finally {
				
				System.out.println("Added object to queue of Id:" + inputDTO.getId() + " wasAdded: " + wasAdded);
			}
			
			return inputDTO;
		}
}
