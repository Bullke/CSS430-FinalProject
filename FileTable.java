import java.util.Vector;

/*
Maintains the file table shared among all user threads.
 */
public class FileTable
{
  private final static int UNUSED = 0;    //inode.flag statuses
  private final static int USED = 1;
  private final static int READ = 2;
  private final static int WRITE = 3;
  private final static int DELETE = 4; // ??

  private Vector <FileTableEntry> table;         // the actual entity of this file table
  private Directory dir;        // the root directory 

  // Constructor
  public FileTable(Directory directory)
  {
     table = new Vector<FileTableEntry>();     // instantiate a file (structure) table
     dir = directory;           // receive a reference to the Director
  }                             // from the file system

  /* Allocates a new file table entry for this file name.
    Updates inode fields.
    Returns a reference to this file table
    */
  public synchronized FileTableEntry falloc(String filename, String mode)
  {
      // allocate a new file (structure) table entry for this file name
      // allocate/retrieve and register the corresponding inode using dir
      // increment this inode's count
      // immediately write back this inode to the disk
      // return a reference to this file (structure) table entry
      short iNumber = -1;
      Inode inode = null;
      while(true)
      {
          iNumber = (filename.equals("/") ? 0 : dir.namei(filename));

          if(iNumber >= 0)
          {
              inode = new Inode(iNumber);
              if(mode.compareTo("r") == 0)
              {
                  if((inode.flag == UNUSED) || (inode.flag == USED) || (inode.flag == READ))
                  {
                      inode.flag = READ;
                      break;
                  }
                  try
                  {
                      wait();
                  }
                  catch (InterruptedException e) {}
              }
              else
              {
                  if((inode.flag == UNUSED) || (inode.flag == USED))
                  {
                      inode.flag = WRITE;
                      break;
                  }
                  try
                  {
                      wait();
                  }
                  catch (InterruptedException e) {}
              }
          }
          else if (!mode.equals("r"))
          {
              iNumber = dir.ialloc(filename);
              inode = new Inode(iNumber);
              inode.flag = WRITE;
              break;

          }
          else
          {
              return null;
          }
      }
      inode.count++;
      inode.toDisk((short)iNumber);
      FileTableEntry newEntry = new FileTableEntry(inode, iNumber, mode);
      table.addElement(newEntry);
      return newEntry;
  }

  /*
  Whenever inode is pointed by a new file structure table entry, its count is incremented.
  When a file structure table entry is released, this count should be decremented.
  A new file structure table entry is created when a thread opens a file and,
  and it is deleted when the thread closes this file.
  */

  public synchronized boolean ffree(FileTableEntry ftEntry)
  {
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table

    if (table.removeElement(ftEntry))
    { // ftEntry exists in File Table
        Inode newInode = new Inode(ftEntry.iNumber);
        if(newInode.flag == READ || newInode.flag == WRITE)
        {
            newInode.flag = USED;
        }
        newInode.count--;
        newInode.toDisk(ftEntry.iNumber);
        notify();
        return true;
    }
    return false;
  }

   /*
   Ffrees the inode that corresponds to this iNumber from table
    */
  public synchronized boolean deleteInode(int iNumber)
  {
    FileTableEntry ftEntry = table.firstElement();
    for (int i = 1; i< table.size(); i++)
    {
        ftEntry = table.elementAt(i);
        if (ftEntry.iNumber == iNumber)
        {
            ffree(ftEntry);
            return true;
        }
    }
    return false;
  }

  public synchronized boolean fempty()
  {
     return table.isEmpty();  // return if table is empty 
  }                            // should be called before starting a format
}