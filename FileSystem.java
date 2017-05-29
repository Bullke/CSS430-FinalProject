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
		int dirSize = directory.fsize(dirEnt);
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

		return -1;
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
