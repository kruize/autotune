package com.autotune.queue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutotuneExecutor {
	
	private final int DEFAULT_POOL_SIZE=5;
	private ExecutorService executorService=null;
	
	public AutotuneExecutor(int poolSize) {
		executorService = Executors.newFixedThreadPool(poolSize);
	}
	
	public AutotuneExecutor() {
		executorService = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE);
		
	}
	
	public Future<AutotuneDTO> submitTask(Callable<AutotuneDTO> task) {
		if(null != this.executorService ) {
			return executorService.submit(task);
		}
		return null;
	}
	
	public void shutdown() {
		executorService.shutdown();
	}
	
}
