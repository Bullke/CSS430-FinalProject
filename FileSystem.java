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
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);

		FileTableEntry dirEnt = open("/", "r");
		int dirSize = this.fsize(dirEnt);
		if(dirSize > 0)
		{
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}

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

	//TODO: You also need a way to make your FileSystem sync itself.
	public void sync(){


    }

// STATIC??????
    public static boolean  format(int files) {
	    return false;

    }


    // Closes the file
    public Boolean close (FileTableEntry ftEntry) {
        return filetable.ffree(ftEntry);
    }




	public int read(FileTableEntry fileTableEnt, byte[] buffer) {

	    // 1. determine how many bytes to read
        int toRead = fileTableEnt.inode.length;

        // 2. allocate buffer for data
        buffer = new byte[toRead];

        // 3. read data from directs
        int currentDirect = 0;
        int destPosition = 0;

        byte[] blockData = new byte[Disk.blockSize];
        while (toRead > 0 && currentDirect < fileTableEnt.inode.direct.length) {
            assert fileTableEnt.inode.direct[currentDirect] != -1;

            SysLib.rawread(fileTableEnt.inode.direct[currentDirect], blockData);
            int chunksize = Math.min(toRead, Disk.blockSize);
            System.arraycopy(blockData, 0, buffer, destPosition, chunksize);

            toRead -= chunksize;
            currentDirect++;
            destPosition += chunksize;
        }

        assert (toRead == 0 && fileTableEnt.inode.indirect == -1) || (toRead > 0 && fileTableEnt.inode.indirect != -1);

        // 4. read data from indirects
        short nextIndirect = fileTableEnt.inode.indirect;
        while (toRead > 0 && nextIndirect != -1) {
            SysLib.rawread(nextIndirect, blockData);
            int chunksize = Math.min(toRead, Disk.blockSize-2);
            System.arraycopy(blockData, 2, buffer, destPosition, chunksize);
            nextIndirect = SysLib.bytes2short(blockData, 0);

            toRead -= chunksize;
            destPosition += chunksize;
        }
        
		return destPosition;
	}


	public int write(FileTableEntry fileTableEnt, byte[] buffer) {


	    return -1;
	}


	public int fsize(FileTableEntry fileTableEnt) {


	    return -1;
	}
	public Boolean deallocAllBlocks(FileTableEntry inputTable) {

		return false;
	}

	public Boolean delete(String filename ) {
	    return false;
    }


	public int seek(FileTableEntry ftEnt, int offset, int whence) { // not seekArgs, but ints or shorts
		return 0;
	}
}
