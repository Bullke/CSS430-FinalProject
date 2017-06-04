import sun.net.ftp.FtpClient;

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

	/*
	Writes all data, superblock and directory to disk
	 */
	public void sync(){
        FileTableEntry dirEntry = open ("/", "w");
        byte[] data = directory.directory2bytes();
        write(dirEntry, data);
        close(dirEntry);

        superblock.sync();
    }


    /*
    Formats the disk. Creates the files number of Inodes
     */
    public boolean  format(int files) {
        while (!filetable.fempty()) {
            ; // wait
        }
        // format superblock, directory, filetable
        superblock.format(files);
        directory = new Directory(files);
        filetable = new FileTable(directory);
	    return true;
    }


    /*
     Closes the fileTableEntry if no threads are using it
      */
    public synchronized Boolean close (FileTableEntry ftEntry) {
        ftEntry.count--;
        if (ftEntry.count > 0) {
            return false;
        }
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
        int currentDirect = fileTableEnt.inode.seekDirectBlock(fileTableEnt.seekPtr); //UPDATED
        int destPosition = 0;

        byte[] blockData = new byte[Disk.blockSize];


        while (toRead > 0 && currentDirect < fileTableEnt.inode.direct.length) {
            assert fileTableEnt.inode.direct[currentDirect] != -1;
            int chunksize = 0;
            int blockOffset = 0;

            if (fileTableEnt.seekPtr % Disk.blockSize != 0) {
                // read the block data starting from seek pointer
                chunksize = Math.min(toRead, (fileTableEnt.seekPtr / Disk.blockSize + 1) * Disk.blockSize - fileTableEnt.seekPtr);
                blockOffset = fileTableEnt.seekPtr % Disk.blockSize;
            } else {
                chunksize = Math.min(toRead, Disk.blockSize);
                blockOffset = 0;
            }

            SysLib.rawread(fileTableEnt.inode.direct[currentDirect], blockData);
            System.arraycopy(blockData, blockOffset, buffer, destPosition, chunksize);

            toRead -= chunksize;
            currentDirect++;
            destPosition += chunksize;
            fileTableEnt.seekPtr += chunksize;
        }

        assert (toRead == 0 && fileTableEnt.inode.indirect == -1) || (toRead > 0 && fileTableEnt.inode.indirect != -1);

        // 4. read data from indirects
        short nextIndirect = fileTableEnt.inode.indirect;
        short indirectBlockSize = Disk.blockSize - 2;
        while (toRead > 0 && nextIndirect != -1) {
            int chunksize;
            int blockOffset = 2;

            if (fileTableEnt.seekPtr % indirectBlockSize != 0) {
                chunksize = Math.min(toRead, (fileTableEnt.seekPtr / indirectBlockSize + 1) * indirectBlockSize - fileTableEnt.seekPtr);
                blockOffset = fileTableEnt.seekPtr % indirectBlockSize;
            } else {
                chunksize = Math.min(toRead, indirectBlockSize);
            }
            SysLib.rawread(nextIndirect, blockData);
            System.arraycopy(blockData, blockOffset, buffer, destPosition, chunksize);
            nextIndirect = SysLib.bytes2short(blockData, 0);

            toRead -= chunksize;
            destPosition += chunksize;
            fileTableEnt.seekPtr+= chunksize;
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

        int toWrite = buffer.length;
	    int currentBufferPosition = 0;
        byte[] blockData = new byte[Disk.blockSize];

        if (fileTableEnt.mode.equals('a')) {
            append(fileTableEnt, buffer);

        } else {  // overwrite

            int directBlockId = -1;
            if (fileTableEnt.seekPtr <= fileTableEnt.inode.direct.length * Disk.blockSize) {
                directBlockId = fileTableEnt.inode.seekDirectBlock(fileTableEnt.seekPtr);
            }
            int chunksize = 0;
            int blockOffset = 0;

            // write directs
            while (toWrite > 0 && directBlockId < fileTableEnt.inode.direct.length && directBlockId >= 0) {

                short directBlock = fileTableEnt.inode.direct[directBlockId];
                if (directBlockId == -1) {
                    directBlock = (short) superblock.getFreeBlock(); // new file size > old
                    if (directBlock == -1) {
                        return -1; // no free space on disk
                    }
                    fileTableEnt.inode.direct[directBlockId] = directBlock;
                }

                if (fileTableEnt.seekPtr % Disk.blockSize != 0) {
                    // read the block data starting from seek pointer
                    chunksize = Math.min(toWrite, (fileTableEnt.seekPtr / Disk.blockSize + 1) * Disk.blockSize - fileTableEnt.seekPtr);
                    blockOffset = fileTableEnt.seekPtr % Disk.blockSize;
                } else {
                    chunksize = Math.min(toWrite, Disk.blockSize);
                    blockOffset = 0;
                }

                if (chunksize < Disk.blockSize) {
                    java.util.Arrays.fill(blockData, (byte) 0);
                }

                System.arraycopy(buffer, currentBufferPosition, blockData, blockOffset, chunksize);
                SysLib.rawwrite(directBlock, blockData);

                toWrite -= chunksize;
                currentBufferPosition+=chunksize;
                //destPosition += chunksize;
                directBlockId++;
                fileTableEnt.seekPtr+= chunksize;
            }

            // do we need to write further?
            if (toWrite <= 0 ){
                // free directs
                short directBlock = fileTableEnt.inode.direct[directBlockId];
                for (; directBlockId < fileTableEnt.inode.direct.length; directBlockId++) {
                     if (fileTableEnt.inode.direct[directBlockId] != -1) {
                         superblock.returnBlock(fileTableEnt.inode.direct[directBlockId]);
                         fileTableEnt.inode.direct[directBlockId] = -1;
                     }
                }
                // free indirects
                freeIndirectBlocks(fileTableEnt.inode.indirect);
                fileTableEnt.inode.indirect = -1;
            } else {
                // write indirects
                int currentIndirectPosition = toWrite;
                short targetBlock = -1;
                int indirectBlockSize = Disk.blockSize - 2;
                int indirectBlockOffset = -1;
                //short oldIndirectChainHead = fileTableEnt.inode.indirect;

                // find a block that contains seek pointer
                targetBlock = fileTableEnt.inode.seekIndirectBlock(fileTableEnt.seekPtr);
                if (targetBlock == -1) {
                    return ERROR;
                }
                SysLib.cout("Indirect block to overwrite data to" + targetBlock);


//                // make an array of indirect block ids to which we will write data
//                int indirectBlockSize = (Disk.blockSize - 2);
//
//                // array of free blocks to use as next
//                short[] nextIndirectBlocks = new short[(toWrite - 1) / indirectBlockSize + 1];
//                Arrays.fill(nextIndirectBlocks, (short)-1);
//
//                short indirectBlock = (short)superblock.getFreeBlock();
//                if (indirectBlock == -1) {
//                    return -1;
//                }
//                // find free blocks to write
//                for ( int i = 0; i < nextIndirectBlocks.length - 1; i++) {
//                    nextIndirectBlocks[i] = (short) superblock.getFreeBlock();
//                    if (nextIndirectBlocks[i] == -1) {
//                        freeBlocks(nextIndirectBlocks);
//                        return -1;
//                    }
//                }

                int iteration = 0; // keeps the next indirect block's id
                short indirectBlock = targetBlock;
                short nextIndirectBlock = -1;
                while (toWrite > 0) {
                    if (fileTableEnt.seekPtr % indirectBlockSize != 0) {
                        // read the block data starting from seek pointer
                        chunksize = Math.min(toWrite, (fileTableEnt.seekPtr / indirectBlockSize + 1) * indirectBlockSize - fileTableEnt.seekPtr);
                        blockOffset = fileTableEnt.seekPtr % indirectBlockSize;
                    } else {
                        chunksize = Math.min(toWrite, indirectBlockSize);
                        blockOffset = 2;
                    }
                    // find the block number of next indirect block
                    byte[] temp = new byte[Disk.blockSize];
                    SysLib.rawread(indirectBlock,temp );
                    nextIndirectBlock = SysLib.bytes2short(temp, 0);
                    if (nextIndirectBlock == -1) {
                        nextIndirectBlock = (short) superblock.getFreeBlock();
                        if (nextIndirectBlock == -1) {
                            return  ERROR;
                        }
                    }

                    SysLib.short2bytes(indirectBlock, blockData, 0);
                    System.arraycopy(buffer, currentBufferPosition, blockData, blockOffset, chunksize);
                    SysLib.rawwrite(indirectBlock, blockData);
//
                    toWrite -= chunksize;
                    fileTableEnt.seekPtr+= chunksize;
                    indirectBlock = nextIndirectBlock;
                }

//               /* while (toWrite > 0) {
//                    assert indirectBlock != -1;
//
//                    chunksize = Math.min(toWrite, indirectBlockSize);
//
//                    if (chunksize < indirectBlockSize) {
//                        java.util.Arrays.fill(blockData, (byte) 0);
//                    }
//
//                    SysLib.short2bytes(nextIndirectBlocks[iteration], blockData, 0);
//                    System.arraycopy(buffer, currentBufferPosition, blockData, 2, chunksize);
//                    SysLib.rawwrite(indirectBlock, blockData);
//
//                    toWrite -= chunksize;
//                    iteration++;
//                    indirectBlock = nextIndirectBlocks[iteration];
//                    fileTableEnt.seekPtr+= chunksize;
//                }
//                fileTableEnt.inode.indirect = indirectBlock;
//                freeIndirectBlocks(oldIndirectChainHead);*/
            }




            fileTableEnt.inode.length = buffer.length;
        }
        fileTableEnt.inode.count++;
        fileTableEnt.inode.flag = 1;
        return buffer.length;
	}

    private int append(FileTableEntry fileTableEnt, byte[] buffer) {
        int toWrite = buffer.length;
        fileTableEnt.seekPtr = buffer.length;
        int lastOccupiedBlock = fileTableEnt.inode.length / Disk.blockSize;
        int freeSpaceOffsetInLastOccupiedBlock = -1;
        int lastOccupiedIndirectBlock = -1;
        if (lastOccupiedBlock <= fileTableEnt.inode.direct.length) {
            freeSpaceOffsetInLastOccupiedBlock = fileTableEnt.inode.length % Disk.blockSize;
        } else {
            int bytesInIndirectBlocks = fileTableEnt.inode.direct.length - Disk.blockSize * fileTableEnt.inode.direct.length;
            lastOccupiedIndirectBlock = bytesInIndirectBlocks / (Disk.blockSize - 2);
            freeSpaceOffsetInLastOccupiedBlock = bytesInIndirectBlocks % (Disk.blockSize - 2);
        }

        byte[] blockData = new byte[Disk.blockSize];
        //int chunksize;

        // case: append to last block which has free space

        if (lastOccupiedIndirectBlock == -1) {
            return appendStartingFromDirectBlock(fileTableEnt, buffer, lastOccupiedBlock, freeSpaceOffsetInLastOccupiedBlock);
        } else {
            return appendStartingFromIndirectBlock(fileTableEnt, buffer, lastOccupiedIndirectBlock, freeSpaceOffsetInLastOccupiedBlock);
        }

    }

    int appendStartingFromDirectBlock(FileTableEntry fileTableEnt, byte[] buffer, int lastOccupiedBlock, int offset) {
	    byte[] blockData = new byte[Disk.blockSize];
        int chunksize = Disk.blockSize - offset;
        int toWrite = buffer.length;
        int srcPosition = 0;

        // fill the last block
        if (offset > 0) {
            SysLib.rawread(fileTableEnt.inode.direct[lastOccupiedBlock], blockData);
            System.arraycopy(buffer, srcPosition, blockData, offset, chunksize);
            SysLib.rawwrite(fileTableEnt.inode.direct[lastOccupiedBlock], blockData);
            srcPosition += chunksize;
            lastOccupiedBlock++;
            toWrite -= chunksize;
            fileTableEnt.seekPtr += chunksize;
        }

        for (; lastOccupiedBlock < fileTableEnt.inode.direct.length; lastOccupiedBlock++) {
            if (toWrite <= 0) {
                break;
            }
            short nextFreeBlock = (short) superblock.getFreeBlock();
            if (nextFreeBlock == -1) {
                return -1;
            }
            fileTableEnt.inode.direct[lastOccupiedBlock] = nextFreeBlock;
            chunksize = Math.min(toWrite, Disk.blockSize);
            Arrays.fill(blockData, (byte) 0);
            System.arraycopy(buffer, srcPosition, blockData, 0, chunksize);
            SysLib.rawwrite(fileTableEnt.inode.direct[lastOccupiedBlock], blockData);
            srcPosition += chunksize;
            toWrite -= chunksize;
            fileTableEnt.seekPtr += chunksize;
        }

        if (toWrite > 0) {

            byte[] remainingBuffer = new byte[toWrite];
            System.arraycopy(buffer, srcPosition, remainingBuffer, 0, toWrite);
            appendStartingFromIndirectBlock(fileTableEnt,remainingBuffer, -1,0 );
        }

        return 0;
    }

    /**
     *  appendStartingFromIndirectBlock(fileTableEnt, buffer, -1, x) - inode.indirect is not set
     *  appendStartingFromIndirectBlock(fileTableEnt, buffer, 0, x)  - inode.indirect is set, and that block is not full
     *  appendStartingFromIndirectBlock(fileTableEnt, buffer, 1, x)  - regular case
     *
     */
    int appendStartingFromIndirectBlock(FileTableEntry fileTableEnt, byte[] buffer, int lastOccupiedBlock, int offset) {
        byte[] blockData = new byte[Disk.blockSize];
        int chunksize = Disk.blockSize - 2 - offset;
        int toWrite = buffer.length;
        int srcPosition = 0;
        short nextIndirectIdx = -1;

        if (lastOccupiedBlock == -1) {
            nextIndirectIdx  = (short)superblock.getFreeBlock();
            if (nextIndirectIdx == -1) {
                return -1;
            }
            fileTableEnt.inode.indirect = nextIndirectIdx;
        } else {

            // find the block to fill
            nextIndirectIdx = fileTableEnt.inode.indirect;
            for (int i = 0; i < lastOccupiedBlock; i++) {
                SysLib.rawread(nextIndirectIdx, blockData);
                nextIndirectIdx = SysLib.bytes2short(blockData, 0);
            }

            // fill the last block
            if (offset > 0) {
                SysLib.rawread(nextIndirectIdx, blockData);
                System.arraycopy(buffer, srcPosition, blockData, offset + 2, chunksize);

                srcPosition += chunksize;
                toWrite -= chunksize;
                fileTableEnt.seekPtr += chunksize;

                short lastIndirect = nextIndirectIdx;
                nextIndirectIdx = (short) superblock.getFreeBlock();
                if (toWrite > 0 && nextIndirectIdx != -1) {
                    SysLib.short2bytes(nextIndirectIdx, blockData, 0);
                }
                SysLib.rawwrite(lastIndirect, blockData);
            }
        }

        short currentBlock = nextIndirectIdx;

        while (toWrite > 0) {

            assert currentBlock != -1;

            chunksize = Math.min(toWrite, Disk.blockSize-2);
            if (chunksize < Disk.blockSize - 2) {
                java.util.Arrays.fill(blockData, (byte) 0);
            }

            nextIndirectIdx = (short) superblock.getFreeBlock();
            if (nextIndirectIdx == -1) {
                return -1;
            }

            SysLib.short2bytes(nextIndirectIdx, blockData, 0);
            System.arraycopy(buffer, srcPosition, blockData, 2, chunksize);
            SysLib.rawwrite(currentBlock, blockData);

            toWrite -= chunksize;
            fileTableEnt.seekPtr += chunksize;
            currentBlock = nextIndirectIdx;
        }

        return srcPosition;
    }

    // deletes all the chain of indirect blocks
	void freeIndirectBlocks(short blockId) {
	    byte[] blockData = new byte[Disk.blockSize];
	    short currentBlock = blockId;
	    short nextBlock;

        while (currentBlock != -1) {
            SysLib.rawread(blockId,blockData);
            nextBlock = SysLib.bytes2short(blockData, 0);
            superblock.returnBlock(currentBlock);
            currentBlock = nextBlock;
        }
    }


    //delete blocks
    private void freeBlocks(short[] blocks) {
	    for (short block : blocks){
	        if ( block != -1) {
                superblock.returnBlock(block);
            }
        }
    }

    /*
    Returns the size of this file
     */
	public synchronized int fsize(FileTableEntry fileTableEnt) {
	    assert (fileTableEnt != null);
	    return fileTableEnt.inode.length;

	}

	/*
    Deletes all blocks to which inode points
	 */
	public Boolean deallocAllBlocks(FileTableEntry ftEntry) {



		return false;
	}

	/*
	Delete a file.
	Wait if it is used by other threads.
	 */
	public synchronized Boolean delete(String filename ) {
        short iNumber = directory.namei(filename);
        if (filetable.deleteInode(iNumber) ) {
            return directory.ifree(iNumber);
        }
        return false;
    }


	public int seek(FileTableEntry ftEntry, int offset, int whence) { // not seekArgs, but ints or shorts
        if (ftEntry == null) {
            return ERROR;
        }
        switch (whence){

            case SEEK_SET:
                //seekPtr -> beginning + offset
                int adjusted_offset = offset;
                if ( adjusted_offset < 0 ) {
                    adjusted_offset = 0;
                }
                else if (adjusted_offset > ftEntry.inode.length){
                    adjusted_offset = ftEntry.inode.length;
                }
                ftEntry.seekPtr = adjusted_offset;
                return OK;


            case SEEK_CUR:
                //seekPtr -> current seekPtr + offset
                seek(ftEntry, ftEntry.seekPtr + offset, SEEK_SET);
                break;

            case SEEK_END:
                //seekPtr -> size + offset
                // This case contradicts the requirement to set the ptr to the eof ???????????????????????????
                seek(ftEntry, ftEntry.inode.length + offset, SEEK_SET);
                break;
        }
		return ERROR;
	}
}
