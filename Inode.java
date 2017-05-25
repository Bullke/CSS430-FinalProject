// Each Inode is one (describes) file
// First 11 pointers point to data (direct) blocks, last points to indirect block
// Length of the file
// Number of file table entries pointing to this node
// Flag: 0 - unused, 1 - used

import java.util.*;

public class Inode
{
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    public Inode()
    {                                     // a default constructor
       length = 0;
       count = 0;
       flag = 1;
       for (int i = 0; i < directSize; i++)
       {
          direct[i] = -1;
       }
       indirect = -1;
    }
    public Inode(short iNumber)
    {                       // retrieving inode from disk
       // design it by yourself.
 	   int blockNumber = (1 + (iNumber / 16));
 	   byte[] data = new byte[Disk.blockSize];
 	   SysLib.rawread(blockNumber, data);
 	   int offset = ((iNumber % 16) * 32)
 	   length = SysLib.bytes2int(data, offset);
 	   offset += 4;
 	   count = SysLib.bytes2short(data, offset);
 	   offset += 2;
 	   flag = SysLib.bytes2short(data, offset);
 	   offset += 2;
 	   .
 	   .
 	   .
 	   //etc
    }
    public int toDisk(short iNumber)
    {                  // save to disk as the i-th inode
       // design it by yourself.
    }
    public short getIndexBlockNumber()
    {

    }
    public Boolean setIndexBlock(short indexBlockNumber)
    {

    }
    public short findTargetBlock(int offset)
    {
    	
    }
}