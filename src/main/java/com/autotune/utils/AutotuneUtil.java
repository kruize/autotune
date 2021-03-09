/*******************************************************************************
 * Copyright (c) 2020, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.utils;

/**
 * This is a Utility class at the Autotune level for common constants, functions etc.
 * @author bipkumar
 *
 */
public final class AutotuneUtil {
	
	// Initial capacity of the queue
	public static int INITIAL_QUEUE_CAPACITY = 50;
	
	// Blocking queue names used in Autotune
	public enum Operation {
		ADD, UPDATE, DELETE
	}
	
	public enum QueueName {
		RECMGRQUEUE, EXPMGRQUEUE
	}
}
