#!/usr/bin/python3

import threading
import time

exitFlag = 0

def exp_test_loop(threadName, start, end):
   print("Starting " + threadName)
   for x in range(start, end):
      if exitFlag:
         threadName.exit()
      time.sleep(0)
      print("%s: %s" % (threadName, x))
   print("Exiting " + threadName)


def split_processing(expcount, num_splits=10):
    split_size = expcount / num_splits
    threads = []
    for i in range(num_splits):
        # determine the indices of the list this thread will handle
        start = i
        # special case on the last chunk to account for uneven splits
        end = int(min((i*split_size), expcount))
        name = 'Thread-%s'%(i)
        # create the thread
        threads.append(threading.Thread(target=exp_test_loop, args=(name, start, end)))
        threads[-1].start() # start the thread we just created

    # wait for all threads to finish
    for t in range(num_splits):
        threads[t-1].join()

split_processing(10000)
print("Exiting Main Thread")
