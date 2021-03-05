package com.autotune.queue;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.autotune.em.utils.EMUtils.QueueName;

public class RecMgrQueue implements AutotuneQueue, Serializable {
	private static final long serialVersionUID = -6045964856984857449L;
	private final int INITIAL_CAPACITY = 50;
	private final BlockingQueue<AutotuneDTO> queue;
	private final String name;
	private static RecMgrQueue instance;

	private RecMgrQueue()
	{
		this.name = QueueName.RECMGRQUEUE.name();
		queue = new LinkedBlockingQueue<AutotuneDTO>(INITIAL_CAPACITY);
	}

	public static RecMgrQueue getInstance()
	{
		if (instance == null)
		{
			synchronized(RecMgrQueue.class)
			{
				if (instance == null)
				{
					instance = new RecMgrQueue();
				}
			}            
		}

		return instance;
	} 
	//
	private Object readResolve() {
		return getInstance();
	}
	
	@Override
	public boolean send(AutotuneDTO data) throws InterruptedException {
		return queue.offer(data);
	}
	
	@Override
	public AutotuneDTO get() throws InterruptedException {
		return queue.take();
	}
	
	@Override
	public String getName() {
		return this.name;
	}
		
}
