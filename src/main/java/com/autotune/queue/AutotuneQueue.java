package com.autotune.queue;

public interface AutotuneQueue {
	
	public boolean send(AutotuneDTO data) throws InterruptedException;
	
	public AutotuneDTO get() throws InterruptedException;
	
	public String getName();
}
