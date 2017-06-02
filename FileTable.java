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

   private Vector table;         // the actual entity of this file table
   private Directory dir;        // the root directory 

    // Constructor
   public FileTable(Directory directory)
   {
      table = new Vector( );     // instantiate a file (structure) table
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

         // ADDED PR5 Create a new Inode
         if (iNumber >= 0) {
            inode = new Inode(iNumber); // Inode is already on the disk
         }
         else {
            inode = new Inode(); // Create an empty Inode
         }


         if (mode.equals('r'))
         {
            break;
         }
         else if(inode.flag == )
         {
            try
            {
               wait();  
            }
            catch (InterruptedException e)
            {}
         }
         else if(true/*inode.flag is "to be deleted"*/)
         {
            iNumber = -1;
            return null;
         }
         else if(true/*mode.compareTo("w")*/)
         {

         }
         inode.count++;
         inode.toDisk(iNumber);
         FileTableEntry e = new FileTableEntry(inode, iNumber, mode); // Create a table entry and register is
         table.addElement(e);
         return e;
      }
      return new  FileTableEntry(inode, iNumber, mode);

   }

   public synchronized boolean ffree(FileTableEntry e)
   {
      // receive a file table entry reference
      // save the corresponding inode to the disk
      // free this file table entry.
      // return true if this file table entry found in my table
      return true;
   }

   public synchronized boolean fempty()
   {
      return table.isEmpty();  // return if table is empty 
   }                            // should be called before starting a format
}