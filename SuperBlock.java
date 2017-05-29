import java.util.*;

public class SuperBlock
{
	// 0th block for SuperBlock
	// 4 blocks of Inode blocks (16 Inodes for each)
	// Rest of blocks for data

	public final static int MAX_BLOCKS = 1000;
	public final static int MAX_INODES = 64; // MAX_FILES

	public int totalBlocks;
	public int totalInodes;
	public int freeList;
	public Vector inodeBlock;

	public SuperBlock(int diskSize)
	{
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.rawread(0, superBlock);
		totalBlocks = SysLib.bytes2int(superBlock, 0);
		totalInodes = SysLib.bytes2int(superBlock, 4);
		freeList = SysLib.bytes2int(superBlock, 8);
		if((totalBlocks == diskSize) && (totalInodes > 0) && (freeList >= 2))
		{
			// Disk contents are valid
			return;
		}
		else
		{
			// Need to format disk
			totalBlocks = diskSize;
			FileSystem.format(MAX_INODES);
		}
	}

	public void sync()
	{
		// Write back totalBlocks, inodeBlocks, and freeList to disk
	}

	public int getFreeBlock()
	{
	    int freeBlock = freeList;

        if(freeBlock != -1) {
            byte[] blockData = new byte[Disk.blockSize];
            SysLib.rawread(freeBlock, blockData);

            // next freeBlock
            this.freeList = SysLib.bytes2int(blockData, 0);

            // zero out first int in the block
            SysLib.int2bytes(0, blockData, 0);
            SysLib.rawwrite(freeBlock, blockData);
        }

		return freeBlock;
	}
	public void returnBlock(int blockNumber)
	{
		// Enqueue a given block to the end of the free list
	}
}
