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
		byte[] SuperBlock - new byte[Disk.blockSize];
		Sys.Lib.rawread(0, SuperBlock);
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
			format(MAX_INODES);
		}
	}
	public void sync()
	{
		// Write back totalBlocks, inodeBlocks, and freeList to disk
	}
	public void getFreeBlock()
	{
		// Dequeue the top block from the fre list
	}
	public void returnBlock(int blockNumber)
	{
		// Enqueue a given block to the end of the free list
	}
}