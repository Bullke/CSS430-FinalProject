public class TCB
{
	private static int ERROR = -1;
	private static int DEFAULT_ENTRIES = 32;
    private Thread thread = null;
    private int tid = 0;
    private int pid = 0;
    private boolean terminated = false;
    private int sleepTime = 0;
    public FileTableEntry[] ftEnt = null; // added for the file system

    public TCB(Thread newThread, int myTid, int parentTid)
    {
		thread = newThread;
		tid = myTid;
		pid = parentTid;
		terminated = false;
		ftEnt = new FileTableEntry[DEFAULT_ENTRIES];    // added for the file system
		System.err.println( "threadOS: a new thread (thread=" + thread + " tid=" + tid + " pid=" + pid + ")");
    }

    public synchronized Thread getThread()
    {
		return thread;
    }

    public synchronized int getTid()
    {
		return tid;
    }

    public synchronized int getPid()
    {
		return pid;
    }

    public synchronized boolean setTerminated()
    {
		terminated = true;
		return terminated;
    }

    public synchronized boolean getTerminated()
    {
		return terminated;
    }

    // added for the file system
    public synchronized int getFd(FileTableEntry entry)
    {
		if(entry != null)
		{
		    for(int position = 3; position < DEFAULT_ENTRIES; position++)
			{
			    if(ftEnt[position] == null)
			    {
					ftEnt[position] = entry;
					return position;
			    }
			}
		}
		return ERROR;
    }

    // added for the file system
    public synchronized FileTableEntry returnFd(int fd)
    {
		if(fd >= 3 && fd < DEFAULT_ENTRIES)
		{
		    FileTableEntry oldEnt = ftEnt[fd];
		    ftEnt[fd] = null;
		    return oldEnt;
		}
		return null;
    }

    // added for the file systme
    public synchronized FileTableEntry getFtEnt(int fd)
    {
		if(fd >= 3 && fd < DEFAULT_ENTRIES)
		{
		    return ftEnt[fd];
		}
		return null;
    }
}
