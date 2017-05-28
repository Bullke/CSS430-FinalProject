import java.util.Vector;

/**
 * Monitor that manages QueueNodes that wait till the scheduler
 * wakes them up to execute
 * QueueNode  pid = condition = queue array index
 */
public class SyncQueue {
    private QueueNode[] queue;
    static final int DEFAULT_COND_NUMBER = 10;
    static final int ERROR = -1;

    public SyncQueue( ) {
        this( DEFAULT_COND_NUMBER );
    }

    public SyncQueue( int cond_max ) {
        queue = new QueueNode[ cond_max ];
        for ( int i=0; i < cond_max; i++ ) {
            queue[ i ] = new QueueNode();
        }
    }

    // Puts a thread to a waiting set. Thread sleeps until the condition = tid satisfied
    // Returns tid of a child thread or Error code
    public int enqueueAndSleep( int condition ) {
        if ( condition >= 0 && condition <queue.length ) {
            if (queue[condition] != null) {
                return queue[condition].sleep();
            }
        }
        return ERROR;
    }

    // Search for thread with pid = condition in the queue
    // and wakeup the first match
    public int dequeueAndWakeup( int condition ) {
        return dequeueAndWakeup( condition, 0 );
    }

    // Returns tid of a child thread of Error code
    public int dequeueAndWakeup( int condition, int tid ) {
        if (queue[condition] != null) {
            queue[condition].wakeup(tid);
            return tid;
        }
        return ERROR;
    }

    /*
    Array of child threads of a current thread with its threadId = condition
    Imitates a FIFO queue: a new thread is added to the end of the queue;
    the thread at position 0 is being woken up
     */
    public class QueueNode {
        private Vector<Integer> waiting_threads;

        // Constructor
        public QueueNode () {
            waiting_threads = new Vector<Integer>();
        }

        // Puts a thread to sleep or removes the first waiting thread
        synchronized public int sleep() {
            // Case: no threads in the queue -> nothing to return and thread sleeps
            if ( waiting_threads.isEmpty() ) {
                try {
                    wait();
                } catch ( InterruptedException ignore ) {};
            }
            // Case: there are threads in queue -> first thread in FIFO is removed
            return waiting_threads.remove( 0 );
        };

        // Wakes up a thread when its tid is called
        // Case: no threads in the queue -> nothing to return
        // Case: there are threads in queue -> first thread wakes up
        synchronized public void wakeup( int tid ){
            waiting_threads.add( tid );
            notify();
        }
    }
}
