import java.util.Vector;

public class Cache {
    // Entry class for Cache
    private class Entry {
        public static final int INVALID_FRAME = -1;
        public int frame;           // disk block number
        public boolean reference;   // true if accessed, false for victim
        public boolean modify;      // true if written to disk, false if not

        // Default constructor
        public Entry( ) {
            frame = INVALID_FRAME; // Set to -1 per assignment instruction
            reference = false;
            modify = false;
        }
    }

    // Cache variables
    private Vector<byte[]> pages;
    private Entry[] pageTable;
    public final int NO_PAGES_FREE = -2;
    private int blockSize;
    private int victim;

    // Default constructor
    public Cache( int blockSize, int cacheBlocks ) {
        // Build cache
        this.blockSize = blockSize;
        pages = new Vector<byte[]>();
        pageTable = new Entry[cacheBlocks];
        // due to FIFO, set last frame as victim
        victim = ( cacheBlocks - 1 );

        // Populate
        for ( int i = 0; i < cacheBlocks; i++ ) {
            pages.add( new byte[blockSize] );
            pageTable[i] = new Entry( );
        }
    }

    /*
     * Scan all the cache entries in sequential order.
     * If the corresponding entry has been found in the cache,
     * read the contents from the cache. Otherwise, find a free block entry in
     * the cache and read the data from the disk to this cache block. If the
     * cache is full and there is not a free cache block, find a victim using
     * the enhanced second-chance algroithm. If this victim is dirty, write
     * its contents to the disk and thereafter read new data
     * from the disk into this cache block. Sets the reference bit upon completion.
     *
     * @param int blockID       Identifies the block to be searched for.
     * @param byte buffer[]     The buffer to be read to.
     */
    public synchronized boolean read( int blockID, byte buffer[] ) {
        if ( blockID < 0 ) {
            SysLib.cout( "[ERROR] Bad Block ID. Block ID Should Be > 0.\n" );
            return false;
        }

        // Scan cache entries for corresponding page
        for ( int i = 0; i < pageTable.length; i++ ) {
            if ( pageTable[i].frame == blockID ) {
                // Read contents to buffer, set reference bit
                System.arraycopy( pages.elementAt(i), 0, buffer, 0, blockSize);
                pageTable[i].reference = true;
                return true;
            }
        }

        // Cache entry not found, page fault, find free page
        int pageIndex = findFreePage( );

        if ( pageIndex == NO_PAGES_FREE ) {
            // Cache full, find victim page
            pageIndex = getVictimPage( );
        }

        // Write data to disk if modify bit is set
        writeBack( pageIndex );

        // Read requested block
        SysLib.rawread( blockID, buffer );

        // Cache the block
        writeToCache( "read", blockID, buffer, pageIndex );

        return true;
    }

    /*
     * Scan the cache sequentially for the appropriate block.
     * If the corresponding entry has been found in the cache, write
     * new data to this cache block. Otherwise, find a free block entry in the
     * cache and write the data to this cache Mark its cache entry as dirty. If
     * a free cache block cannot be found, then find a victim using the
     * enhanced second-chance algorithm. If this victim is dirty,
     * write back its contents to the disk and thereafter write new data into
     * this cache block. Sets the reference and modify bit upon completion.
     *
     * @param int blockID       Identifies the block to be searched for.
     * @param byte buffer[]     The buffer to be read to.
     */
    public synchronized boolean write( int blockID, byte buffer[] ) {
        if ( blockID < 0 ) {
            SysLib.cout( "[ERROR] Bad Block ID. Block ID Should Be > 0.\n" );
            return false;
        }

        // Scan Entry array for page
        for ( int i = 0; i < pageTable.length; i++ ) {
            if ( pageTable[i].frame == blockID ) {
                // Write contents to buffer, set modify bit
                writeToCache( "write", blockID, buffer, i);
                return true;
            }
        }

        // Page fault, find free page
        int pageIndex = findFreePage( );

        if ( pageIndex == NO_PAGES_FREE ) {
            // Cache full, find victim page
            pageIndex = getVictimPage( );
        }

        // Write data to disk if modify bit is true
        writeBack( pageIndex );

        // Cache the block
        writeToCache( "write", blockID, buffer, pageIndex );

        return true;
    }

    /*
     * Writes all modified blocks to Disk.
     */
    public synchronized void sync( ) {
        for ( int i = 0; i < pageTable.length; i++ )
            writeBack( i );
        SysLib.sync( );
    }

    /*
     * Writes all modified blocks to Disk and invalidates all cache blocks
     */
    public synchronized void flush( ) {
        for ( int i = 0; i < pageTable.length; i++ ) {
            writeBack( i );
            pageTable[i].reference = false;
            pageTable[i].frame = Entry.INVALID_FRAME;
        }
        SysLib.sync( );
    }

    /* Finds a free page if one exists.
     *
     * @return    index the index of a free page
     *            if it was found successfully.
     *            NO_PAGES_FREE otherwise (value = -2).
     */
    private int findFreePage( ) {
        for ( int index = 0; index < pageTable.length; index++ ) {
            if ( pageTable[index].frame == Entry.INVALID_FRAME )
                return index;
        }
        return NO_PAGES_FREE;
    }

    /* Finds a victim page in accordance to enchanced-second chance algoritm.
     *
     * @return    victim the victim page to be replaced.
     *            NO_PAGES_FREE otherwise (value = -2).
     */
    private int getVictimPage( ) {

        int start = victim;
        while ( true ) {
            victim  = ( ( ++victim ) % pageTable.length );
            if ( !pageTable[victim].reference && !pageTable[victim].modify ) {
                return victim;
            }
            pageTable[victim].reference = false;

            // Didn't find a (0, 0), need to sync to find the previous (1, 0)s
            if ( victim == start )
                sync( );
        }

        /*int start = victim;
        boolean onceThrough = false;
        boolean twiceThrough = false;
        int count = 0;
        while ( true ) {
            if ( onceThrough ) {
                victim  = ( ( ++victim ) % pageTable.length );
                if ( !pageTable[victim].reference && pageTable[victim].modify ) {
                    return victim;
                }
                pageTable[victim].reference = false;
            }
            if ( twiceThrough ) {
                victim  = ( ( ++victim ) % pageTable.length );
                if ( !pageTable[victim].reference ) {
                    return victim;
                }
            }
            else {
                victim  = ( ( ++victim ) % pageTable.length );
                if ( !pageTable[victim].reference && !pageTable[victim].modify ) {
                    return victim;
                }
                pageTable[victim].reference = false;
            }
            // Didn't find a (0, 0), need to find the previous (1, 0)s
            if ( victim == start ) {
                count++;
                if ( count == 1 )
                    onceThrough = true;
                if ( count == 2 ) {
                    onceThrough = false;
                    twiceThrough = true;
                }
            }
        }*/
    }

    /* Writes victim page to disk if it has a modify bit (dirty) set to true.
     *
     * @param victimePage   the page to be written to disk if its modify bit is
     *                      set to true.
     */
    private void writeBack( int victimPage ) {
        if ( pageTable[victimPage].frame != Entry.INVALID_FRAME &&
             pageTable[victimPage].modify ) {
                 SysLib.rawwrite( pageTable[victimPage].frame,
                                  pages.elementAt( victimPage ) );
                 pageTable[victimPage].modify = false;
             }
    }

    /* Writes buffer to cache.
     *
     * @param blockID
     * @param buffer
     * @param pageIndex
     */
    private void writeToCache( String type, int blockID, byte[] buffer,
                               int pageIndex ) {
        byte[] newPage = new byte[blockSize];
        System.arraycopy( buffer, 0, newPage, 0, blockSize );
        pages.set( pageIndex, newPage );
        pageTable[pageIndex].reference = true;
        pageTable[pageIndex].frame = blockID;
        if ( type.equals( "write" ) )
            pageTable[pageIndex].modify = true;
    }
}
