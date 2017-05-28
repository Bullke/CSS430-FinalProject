import java.util.*;

public class FileSystem
{
	public final static int ERROR = 0;
   	public final static int OK = 1;

	public final static int SEEK_SET = 0;
   	public final static int SEEK_CUR = 1;
  	public final static int SEEK_END = 2;

  	private SuperBlock superblock;
  	private Directory directory;
  	private FileTable filetable;

	public FileSystem(int diskBlocks)
	{
		superblock = new SuperBlock(diskBlocks);
		filetable = new Directory(superblock.totalInodes);
		filetable = new FileTableEntry(directory);

		FileTableEntry dirEnt = open("/", "r");
		int dirSize = Directory::fsize(dirEnt);
		if(dirSize > 0)
		{
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}
	//int SysLib.format( int files );
	//int fd = SysLib.open( String fileName, String mode );
	//int read( int fd, byte buffer[] );
	//int write( int fd, byte buffer[] );
	//int seek( int fd, int offset, int whence );
	//int close( int fd );
	//int delete( String fileName );
	//int fsize( int fd );
	public FileTableEntry open(String filename, String mode)
	{
		FileTableEntry ftEnt = filetable.falloc(filename, mode);
		if(mode.equals("w"))
		{
			if(deallocAllBlocks(ftEnt) == false)
			{
				return null;
			}
		}
		return ftEnt;
	}
	public int read(FileTableEntry fileTableEnt, byte[] buffer)
	{
		return -1;
	}
	public int write(FileTableEntry fileTableEnt, byte[] buffer)
	{
		return -1;
	}
	public int fsize(FileTableEntry fileTableEnt)
	{
		return -1;
	}
	public Boolean deallocAllBlocks(FileTableEntry inputTable)
	{
		return false;
	}
}