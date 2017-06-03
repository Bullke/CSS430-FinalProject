import java.util.*;

/*
Maintains the file table shared among all user threads.
 */
public class FileTable
{
    private final static int UNUSED = 0;    //inode.flag statuses
    private final static int USED = 1;
    private final static int READ = 2;
    private final static int WRITE = 3;
    private final static int DELETE = 4;

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

           if (iNumber >= 0) {
               inode = new Inode(iNumber); // Inode is already on the disk
           }
           else {
               iNumber = dir.ialloc(filename);
               inode = new Inode(); // Create an empty Inode
           }

           if (inode.flag == DELETE) // Inode is set to be deleted
           {
               iNumber = -1;
               return null;
           }



           if (mode.equals('r')) {
               if (inode.flag != WRITE) {
                   inode.flag = READ;
                   break;
               } else if (inode.flag == WRITE) {
                   try {
                       wait();  // Wait if other threads are writing
                   } catch (InterruptedException e) {}
               }
           }

           else
           {
               if (inode.flag == UNUSED || inode.flag == USED) {
                   inode.flag = WRITE;
                   break;
               } else {
                   try {
                       wait();  // Wait if other threads are writing
                   } catch (InterruptedException e) {}
               }
           }

           FileTableEntry ftEntry = new FileTableEntry(inode, iNumber, mode); // Create a table entry and register is
           table.addElement(ftEntry);
           inode.count++;
           inode.toDisk(iNumber);
           return ftEntry;
       }
       return new  FileTableEntry(inode, iNumber, mode);

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

       if (table.removeElement(ftEntry)) { // ftEntry exists in File Table
           ftEntry.inode.toDisk(ftEntry.iNumber);
           ftEntry.count--;
           ftEntry.inode.flag = UNUSED;
           notifyAll();
           return true;
       }
      return false;
   }

   public synchronized boolean fempty()
   {
      return table.isEmpty();  // return if table is empty 
   }                            // should be called before starting a format
}