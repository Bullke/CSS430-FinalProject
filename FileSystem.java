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




    /*
    Returns the number of bytes that have been read or -1 upon an error
     */
	public int read(FileTableEntry fileTableEnt, byte[] buffer) {
	    if (fileTableEnt.mode.equals('w') || fileTableEnt.mode.equals('a')) {
	        return -1;
        }

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


    /*
    Returns the number of bytes that have been written from buffer to disk or -1 upon an error
    Can overwrite or append data
    TODO: write past the end of file?
     */
	public int write(FileTableEntry fileTableEnt, byte[] buffer) {
        if (fileTableEnt.mode.equals('r') ) {
            return -1;
        }

        // 2 cases: file dne or its len = 0
//        if (fileTableEnt.inode.length==0){
//
//        }



        int toWrite = buffer.length;
        int destPosition = 0;
	    int currentPosition = 0;
	    int nextFreeBlock;
        byte[] blockData = new byte[Disk.blockSize];


        if (fileTableEnt.mode.equals('a')) {
            currentPosition = fileTableEnt.inode.length;
            while (toWrite > 0) {
                nextFreeBlock = superblock.getFreeBlock();
                if (nextFreeBlock == -1) {
                    return -1;
                }
                int chunksize = Math.min(toWrite, Disk.blockSize);
                System.arraycopy(buffer, 0, blockData, destPosition, chunksize);
                SysLib.rawwrite(nextFreeBlock, blockData);

                toWrite -= chunksize;
                currentPosition++;
                destPosition += chunksize;
            }
            fileTableEnt.inode.length = buffer.length + destPosition;
        }

        else {
            nextFreeBlock = 0;
            int directBlockId = 0;
            // write directs
            while (toWrite > 0 && directBlockId < fileTableEnt.inode.direct.length ) {

                short directBlock = fileTableEnt.inode.direct[directBlockId];
                if (directBlockId == -1) {
                    directBlock = (short) superblock.getFreeBlock(); // new file size > old
                    if (directBlock == -1) {
                        return -1; // no free space on disk
                    }
                    fileTableEnt.inode.direct[directBlockId] = directBlock;
                }

                int chunksize = Math.min(toWrite, Disk.blockSize);
                if (chunksize < Disk.blockSize) {
                    java.util.Arrays.fill(blockData, (byte) 0);
                }

                System.arraycopy(buffer, currentPosition, blockData, 0, chunksize);
                SysLib.rawwrite(directBlock, blockData);

                toWrite -= chunksize;
                currentPosition+=chunksize;
                //destPosition += chunksize;
                directBlockId++;
            }

            // do we need to write further?
            if (toWrite <= 0 ){
                short directBlock = fileTableEnt.inode.direct[directBlockId];
                for (; directBlockId < fileTableEnt.inode.direct.length; directBlockId++) {
                     if (fileTableEnt.inode.direct[directBlockId] != -1) {
                         superblock.returnBlock(fileTableEnt.inode.direct[directBlockId]);
                         fileTableEnt.inode.direct[directBlockId] = -1;
                     }
                }
            }


            fileTableEnt.inode.length = buffer.length;
        }
        fileTableEnt.inode.count++;
        fileTableEnt.inode.flag = 1;
        return buffer.length;
	}


	public int fsize(FileTableEntry fileTableEnt) {
	    assert (fileTableEnt != null);
	    return fileTableEnt.inode.length;
	    //return -1;
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
