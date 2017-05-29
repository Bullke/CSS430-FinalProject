// Each Inode is one (describes) file
// First 11 pointers point to data (direct) blocks, last points to indirect block
// Length of the file
// Number of file table entries pointing to this node
// Flag: 0 - unused, 1 - used

public class Inode
{
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    /*
    Default constructor initializes empty Inode
     */
    public Inode()
    {
       length = 0;
       count = 0;
       flag = 1;
       for (int i = 0; i < directSize; i++)
       {
          direct[i] = -1;
       }
       indirect = -1;
    }

    /*
    If a new file is created, it is given a new inode. In this case, you'll just instantiate it from
    its default constructor where all direct pointers and the indirect pointer are null.
    The contents will be later updated as the file is written.

    An existing file has already had an inode. It is in the disk.
     When such an existing file is opened, you should find the corresponding inode from the disk.
     First, refer to the directory in order to find the inode number. From this inode number, you can calculate
     which disk block contains the inode. Read this disk block and get this inode information.
      Where should you store such inode information then? You should instantiate an inode object first, and then
      reinitialize it with the inode information retrieved from the disk.

      ADDED: FileTable decides whether to create an empty Inode or it already exists
     */
    public Inode(short iNumber)
    {
        // retrieving inode from disk
 	   //int blockNumber = (1 + (iNumber / 16));
        int blockNumber = findTargetBlock(iNumber) + 1;
 	   byte[] data = new byte[Disk.blockSize];
 	   SysLib.rawread(blockNumber, data); // read a block of data from disk
 	   int offset = ((iNumber % 16) * 32);
 	   length = SysLib.bytes2int(data, offset);
 	   offset += 4;
 	   count = SysLib.bytes2short(data, offset);
 	   offset += 2;
 	   flag = SysLib.bytes2short(data, offset);
 	   offset += 2;
 	   for (int i = 0; i <= 10; i+=1, offset +=2) {
 	       direct[i] = SysLib.bytes2short(data, offset);
       }
       indirect = SysLib.bytes2short(data, offset);
    }


    /*
    Write back the contents of Inode to disk
    Returns 0 for success and -1 for failure
     */
    public int toDisk(short iNumber)
    {
        // save to disk as the i-th inode
        //int blockNumber = iNumber % 16;
        int blockNumber = findTargetBlock(iNumber);
        byte[] data = new byte[Disk.blockSize];

        int offset = ((iNumber % 16) * 32);
       SysLib.int2bytes(length, data, offset);
       offset += 4;
       SysLib.short2bytes(count, data, offset);
       offset += 2;
       SysLib.short2bytes(flag, data, offset);
       offset += 2;
       for (int i = 0; i < 11; i+=1, offset +=2) {
             SysLib.short2bytes(direct[i], data, offset);
        }
        SysLib.short2bytes(indirect, data, offset);

       int writeResult = SysLib.rawwrite(blockNumber, data);
       if (writeResult != Kernel.OK ) {
           return -1;
       }
        return 0;
    }

    /*
    Returns the block number
     */
    public short getIndexBlockNumber()
    {
        return indirect;
    }
    /*
    Returns true if indirect is set to indexBlockNumber succesfully
     */
    public Boolean setIndexBlock(short indexBlockNumber)
    {
       if (indirect == -1) {
           indirect = indexBlockNumber;
           return true;
       }
       return false;
    }

    /*
    Finds the block to read/write
     */
//    public short findTargetBlock(int offset)
    public short findTargetBlock(int iNumber)
    {
        if (iNumber >= 0 ) {
            return (short) (iNumber % 16);
        }

        return -1;
    }
}