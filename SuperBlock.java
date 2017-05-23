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

	public SuperBlock(int diskSize)
	{

	}
}