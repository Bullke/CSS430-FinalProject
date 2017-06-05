import java.util.*;

/*
Superblock occupies block 0 and holds information about file system.
 */
public class SuperBlock
{
	public final static int DEFAULT_BLOCKS = 1000;
	public final static int DEFAULT_INODES = 64; // MAX_FILES

	public int totalBlocks;
	public int totalInodes;
	public int freeList;

	public SuperBlock(int diskSize)
	{
		byte[] SuperBlock = new byte[Disk.blockSize];
		SysLib.rawread(0, SuperBlock);
		totalBlocks = SysLib.bytes2int(SuperBlock, 0);
		totalInodes = SysLib.bytes2int(SuperBlock, 4);
		freeList = SysLib.bytes2int(SuperBlock, 8);
		if((totalBlocks == diskSize) && (totalInodes > 0) && (freeList >= 2))
		{
			// Disk contents are valid
			return;
		}
		else
		{
			// Need to format disk
			totalBlocks = diskSize;
			format(DEFAULT_INODES);
		}
	}
	/*
	Writes back totalBlocks, totalInodes, and freeList to disk.
	 */

	public void sync()
	{

		byte[] tempSuperblock = new byte[512];
		SysLib.int2bytes(totalBlocks, tempSuperblock, 0);
		SysLib.int2bytes(totalInodes, tempSuperblock, 4);
		SysLib.int2bytes(freeList, tempSuperblock, 8);
		SysLib.rawwrite(0, tempSuperblock);
	}

	/*
	Returns the first free block from the free list.
	 */

	public int getFreeBlock()
	{
		int tempFreeList = freeList;
		// Dequeue the top block from the fre list
		if(tempFreeList > 0)
		{
			byte[] tempData = new byte[512];
			SysLib.rawread(freeList, tempData);
			freeList = SysLib.bytes2int(tempData, 0);
		}
		return tempFreeList;
	}

	/*
	Enqueues a given block to the end of the free list.
	 */
	public boolean returnBlock(int blockNumber)
	{

		if(blockNumber >= 0)
		{
			byte[] tempData = new byte[512];
			SysLib.int2bytes(freeList, tempData, 0);
			SysLib.rawwrite(blockNumber, tempData);
			freeList = blockNumber;
			return true;
		}
		return false;
	}

	/*
	Formats the file system.
	 */

	public void format(int inodeNum)
	{
		totalInodes = inodeNum;
		if(inodeNum < 0)
		{
			totalInodes = DEFAULT_INODES;
		}
		for(short position = 0; position < inodeNum; position++)
		{
			Inode newInode = new Inode();
			newInode.flag = 0;
			newInode.toDisk((short)position);
		}
		freeList = ((totalInodes / 16) + 2);
		for(int position = freeList; position < DEFAULT_BLOCKS; position++)
		{
			byte[] newData = new byte[512];
			SysLib.int2bytes((position + 1), newData, 0);
			SysLib.rawwrite(position, newData);
		}
		sync();
	}
}