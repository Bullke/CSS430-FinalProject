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
	public synchronized int read(FileTableEntry fileTableEnt, byte[] buffer) {
	    if (fileTableEnt.mode.equals("w") || fileTableEnt.mode.equals("a")) {
	        return -1;
        }

	    // 1. determine how many bytes to read
        int toRead = Math.min(buffer.length, fileTableEnt.inode.length);

        // 2. read data from directs
        int currentDirect = fileTableEnt.inode.seekDirectBlock(fileTableEnt.seekPtr); //UPDATED
        int destPosition = 0;

        byte[] blockData = new byte[Disk.blockSize];


        while (toRead > 0 && currentDirect < fileTableEnt.inode.direct.length && currentDirect >= 0) {
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
//        short nextIndirect = fileTableEnt.inode.indirect;
        short nextIndirect = findIndirectBlockNumberById(fileTableEnt, (fileTableEnt.seekPtr - fileTableEnt.inode.direct.length * Disk.blockSize) / (Disk.blockSize - 2));
        short indirectBlockSize = Disk.blockSize - 2;
        while (toRead > 0 && nextIndirect != -1) {
            int chunksize;
            int blockOffset = 2;

            if ((fileTableEnt.seekPtr - fileTableEnt.inode.direct.length * Disk.blockSize) % indirectBlockSize != 0) {
                chunksize = Math.min(toRead, (fileTableEnt.seekPtr / indirectBlockSize + 1) * indirectBlockSize - fileTableEnt.seekPtr);
                blockOffset += (fileTableEnt.seekPtr - fileTableEnt.inode.direct.length * Disk.blockSize) % indirectBlockSize;
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
	public synchronized int write(FileTableEntry fileTableEnt, byte[] buffer) {
        if (fileTableEnt.mode.equals("r") ) {
            return Kernel.ERROR;
        }

        int toWrite = buffer.length;
	    int currentBufferPosition = 0;
        byte[] blockData = new byte[Disk.blockSize];

        if (fileTableEnt.mode.equals("a")) {
            int numBytes = append(fileTableEnt, buffer);
            fileTableEnt.inode.length += numBytes;
            fileTableEnt.inode.toDisk(fileTableEnt.iNumber);
            return numBytes;

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
                if (directBlock == -1) {
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
                    SysLib.rawread(directBlock, blockData);
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
                // handle seek ??

                // write indirects
                int currentIndirectPosition = toWrite;
//                short indirectBlockId = fileTableEnt.inode.seekIndirectBlock(fileTableEnt.seekPtr);
//                short indirectBlock = fileTableEnt.inode.indirect;
                short indirectBlock = findIndirectBlockNumberById(fileTableEnt, (fileTableEnt.seekPtr - Disk.blockSize * fileTableEnt.inode.direct.length) / (Disk.blockSize - 2));
                if (indirectBlock == -1) {
                    indirectBlock = (short)superblock.getFreeBlock();
                    if (indirectBlock == -1) {
                        return Kernel.ERROR;
                    }
                    fileTableEnt.inode.indirect = indirectBlock;
                }

                int indirectBlockSize = Disk.blockSize - 2;
                int indirectBlockOffset = -1;
                //short oldIndirectChainHead = fileTableEnt.inode.indirect;

                // find a block that contains seek pointer
//                targetBlock = fileTableEnt.inode.seekIndirectBlock(fileTableEnt.seekPtr);
//                if (targetBlock == -1) {
//                    return ERROR;
//                }
//                SysLib.cout(String.format("Indirect block to overwrite data to %s\n", targetBlock));

                int iteration = 0; // keeps the next indirect block's id
                short nextIndirectBlock = -1;
                while (toWrite > 0) {
                    if ((fileTableEnt.seekPtr - Disk.blockSize * fileTableEnt.inode.direct.length)% indirectBlockSize != 0) {
                        // read the block data starting from seek pointer
//                        chunksize = Math.min(toWrite, (fileTableEnt.seekPtr / indirectBlockSize + 1) * indirectBlockSize - fileTableEnt.seekPtr);
                        chunksize = Math.min(toWrite, (fileTableEnt.seekPtr / indirectBlockSize + 1) * indirectBlockSize - fileTableEnt.seekPtr);
                        chunksize = Math.min(chunksize, indirectBlockSize - (fileTableEnt.seekPtr - Disk.blockSize * fileTableEnt.inode.direct.length)% indirectBlockSize);
                        blockOffset = (fileTableEnt.seekPtr - Disk.blockSize * fileTableEnt.inode.direct.length) % indirectBlockSize + 2;
                    } else {
                        chunksize = Math.min(toWrite, indirectBlockSize);
                        blockOffset = 2;
                    }
                    // find the block number of next indirect block
//                    byte[] existingBlock = new byte[Disk.blockSize];
                    SysLib.rawread(indirectBlock, blockData);
                    nextIndirectBlock = SysLib.bytes2short(blockData, 0);
                    if (nextIndirectBlock <= 0) {
                        nextIndirectBlock = (short) superblock.getFreeBlock();
                        if (nextIndirectBlock == -1) {
                            return  Kernel.ERROR;
                        }
                    }

                    SysLib.short2bytes(nextIndirectBlock, blockData, 0);
                    System.arraycopy(buffer, currentBufferPosition, blockData, blockOffset, chunksize);
                    SysLib.rawwrite(indirectBlock, blockData);
//
                    toWrite -= chunksize;
                    fileTableEnt.seekPtr+= chunksize;
                    indirectBlock = nextIndirectBlock;
                    currentBufferPosition += chunksize;
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




//            fileTableEnt.inode.length = buffer.length; // this may or may not be correct if new contents is smaller than old contents
            fileTableEnt.inode.length = Math.max(fileTableEnt.inode.length, fileTableEnt.seekPtr); // this may or may not be correct if new contents is smaller than old contents
        }
        //fileTableEnt.inode.count++;
        fileTableEnt.inode.flag = 1;
        fileTableEnt.inode.toDisk(fileTableEnt.iNumber);
        return buffer.length;
	}

    private synchronized int append(FileTableEntry fileTableEnt, byte[] buffer) {
//        int toWrite = buffer.length;
        fileTableEnt.seekPtr = fileTableEnt.inode.length;
        int lastOccupiedBlock = fileTableEnt.inode.length / Disk.blockSize;
        int freeSpaceOffsetInLastOccupiedBlock = -1;
        int lastOccupiedIndirectBlock = -1;
        if (lastOccupiedBlock <= fileTableEnt.inode.direct.length) {
            freeSpaceOffsetInLastOccupiedBlock = fileTableEnt.inode.length % Disk.blockSize;
        } else {
            int bytesInIndirectBlocks = fileTableEnt.seekPtr - Disk.blockSize * fileTableEnt.inode.direct.length;
            lastOccupiedIndirectBlock = bytesInIndirectBlocks / (Disk.blockSize - 2);
            freeSpaceOffsetInLastOccupiedBlock = bytesInIndirectBlocks % (Disk.blockSize - 2);
        }

        if (lastOccupiedIndirectBlock == -1) {
            return appendStartingFromDirectBlock(fileTableEnt, buffer, lastOccupiedBlock, freeSpaceOffsetInLastOccupiedBlock);
        } else {
            return appendStartingFromIndirectBlock(fileTableEnt, buffer, lastOccupiedIndirectBlock, freeSpaceOffsetInLastOccupiedBlock);
        }

    }

    int appendStartingFromDirectBlock(FileTableEntry fileTableEnt, byte[] buffer, int lastOccupiedBlock, int offset) {
	    byte[] blockData = new byte[Disk.blockSize];
	    int toWrite = buffer.length;
        int chunksize = Math.min(Disk.blockSize - offset, toWrite);

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

        return srcPosition;
    }

    /**
     *  appendStartingFromIndirectBlock(fileTableEnt, buffer, -1, x) - inode.indirect is not set
     *  appendStartingFromIndirectBlock(fileTableEnt, buffer, 0, x)  - inode.indirect is set, and that block is not full
     *  appendStartingFromIndirectBlock(fileTableEnt, buffer, 1, x)  - regular case
     *
     *  lastOccupiedBlock - i-th indirect block, 0 - inode.indirect, 1 - block referenced by inode.indirect, etc.
     *
     */
    int appendStartingFromIndirectBlock(FileTableEntry fileTableEnt, byte[] buffer, int lastOccupiedBlockId, int offset) {
        byte[] blockData = new byte[Disk.blockSize];
        int chunksize = Math.min(Disk.blockSize - 2 - offset, buffer.length);
        int toWrite = buffer.length;
        int srcPosition = 0;
        short nextIndirect = findIndirectBlockNumberById(fileTableEnt, lastOccupiedBlockId);

        if (nextIndirect == -1) {
            nextIndirect  = (short)superblock.getFreeBlock();
            if (nextIndirect == -1) {
                return -1;
            }
            fileTableEnt.inode.indirect = nextIndirect;
        } else {

            // find the block to fill
//            nextIndirectIdx = fileTableEnt.inode.indirect;
//            for (int i = 0; i < lastOccupiedBlock; i++) {
//                SysLib.rawread(nextIndirectIdx, blockData);
//                nextIndirectIdx = SysLib.bytes2short(blockData, 0);
//            }

            // fill the last block
            if (offset > 0) {
                SysLib.rawread(nextIndirect, blockData);
                System.arraycopy(buffer, srcPosition, blockData, offset + 2, chunksize);

                srcPosition += chunksize;
                toWrite -= chunksize;
                fileTableEnt.seekPtr += chunksize;

                short lastIndirect = nextIndirect;
                nextIndirect = (short) superblock.getFreeBlock();
                if (toWrite > 0 && nextIndirect != -1) {
                    SysLib.short2bytes(nextIndirect, blockData, 0);
                }
                SysLib.rawwrite(lastIndirect, blockData);
            }
        }

        short currentBlock = nextIndirect;

        while (toWrite > 0) {

            assert currentBlock != -1;

            chunksize = Math.min(toWrite, Disk.blockSize-2);
            if (chunksize < Disk.blockSize - 2) {
                java.util.Arrays.fill(blockData, (byte) 0);
            }

            nextIndirect = (short) superblock.getFreeBlock();
            if (nextIndirect == -1) {
                return -1;
            }

            SysLib.short2bytes(nextIndirect, blockData, 0);
            System.arraycopy(buffer, srcPosition, blockData, 2, chunksize);
            SysLib.rawwrite(currentBlock, blockData);

            toWrite -= chunksize;
            fileTableEnt.seekPtr += chunksize;
            currentBlock = nextIndirect;
        }

        return srcPosition;
    }

    short findIndirectBlockNumberById(FileTableEntry ftEntry, int lastOccupiedBlockId) {
        short indirectBlock = ftEntry.inode.indirect;

        short iteration = 0;
        byte[] blockData = new byte[Disk.blockSize];
        while (iteration < lastOccupiedBlockId) {
            SysLib.rawread(indirectBlock, blockData);
            indirectBlock = SysLib.bytes2short(blockData, 0);

            iteration++;
        }

        return indirectBlock;
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

    public byte[] freeIndirectBlocks(FileTableEntry ftEntry)
    {
        if(ftEntry.inode.indirect > 0)
        {
            byte[] blockToReturn = new byte[Disk.blockSize];
            SysLib.rawread((int)ftEntry.inode.indirect, blockToReturn);
            ftEntry.inode.indirect = -1;
            return blockToReturn;
        }
        return null;
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

        if (ftEntry.inode.count != 1) {
            return false;
        }
        byte[] tempData = freeIndirectBlocks(ftEntry);
        for (short position = 0; position < ftEntry.inode.direct.length; position++) {
            if (ftEntry.inode.direct[(short) position] != -1) {
                superblock.returnBlock(ftEntry.inode.direct[(short) position]);
                ftEntry.inode.direct[(short) position] = -1;
            }
        }
        if (tempData != null) {
            short block;
            while ((block = SysLib.bytes2short(tempData, 0)) != -1) {
                superblock.returnBlock((short) block);
            }
        }
        ftEntry.inode.toDisk((short) ftEntry.iNumber);
        return true;


	}

	/*
	Delete a file.
	Wait if it is used by other threads.
	 */
	public synchronized Boolean delete(String filename )
    {
        /*short iNumber = directory.namei(filename);
        if (filetable.deleteInode(iNumber) ) {
            return directory.ifree(iNumber);
        }
        return false;*/
        FileTableEntry ftEntry = open(filename, "w");
        if (directory.ifree(ftEntry.iNumber) && close(ftEntry))
        { //try to free and
            // delete
            return true;
        }
        return false;
    }


	public int seek(FileTableEntry ftEntry, int offset, int whence) { // not seekArgs, but ints or shorts
        if (ftEntry == null) {
            return Kernel.ERROR;
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
                return ftEntry.seekPtr;


            case SEEK_CUR:
                //seekPtr -> current seekPtr + offset
                return seek(ftEntry, ftEntry.seekPtr + offset, SEEK_SET);

            case SEEK_END:
                //seekPtr -> size + offset
                // This case contradicts the requirement to set the ptr to the eof ???????????????????????????
                return seek(ftEntry, ftEntry.inode.length + offset, SEEK_SET);
        }
		return Kernel.ERROR;
	}
}
