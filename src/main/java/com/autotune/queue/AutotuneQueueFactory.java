package com.autotune.queue;

import com.autotune.em.utils.EMUtils.QueueName;

public class AutotuneQueueFactory {

	public static AutotuneQueue getQueue(String queueName) {
		if(queueName == null) {
			return null;
		}
		if(queueName.equalsIgnoreCase(QueueName.RECMGRQUEUE.name())) {
			return RecMgrQueue.getInstance();
			
		} else if(queueName.equalsIgnoreCase(QueueName.EXPMGRQUEUE.name())) {
			return ExpMgrQueue.getInstance();
		}
		
		return null;
	}
}
