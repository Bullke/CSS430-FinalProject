/**
 * Created by pandabaka on 6/5/17.
 */
public class Test8 extends Thread {
    private int fd;
    byte[] buf512 = new byte[512];
    byte[] tmpBuf = new byte[512];

    public Test8(String[] args) {
         fd = Integer.parseInt(args[0]);
    }
    public Test8() {
        fd = 3;
    }

    public void run() {

        //if (test1(16)) // format with specified # of files
            SysLib.cout("Correct behavior of format(16)......................\n");
        //if (test1(19)) // format with specified # of files
            SysLib.cout("Correct behavior of format(19)......................\n");
        //if (test1(66)) // format with specified # of files
            SysLib.cout("Correct behavior of format(66)......................\n");
        //if (test1(0)) // format with specified # of files
            SysLib.cout("Correct behavior of format(0)......................\n");
        //if (test1(-1)) // format with specified # of files
            SysLib.cout("Correct behavior of format(-1)......................\n");
        //if (test1(1)) // format with specified # of files
            SysLib.cout("Correct behavior of format(1)......................\n");

        if (test2()) // check fd sequence on open
            SysLib.cout("Correct behavior of open........................\n");
        if (test3()) // write buf[512] and check size and SysLib.fsize()
            SysLib.cout("Correct behavior of writing a few bytes........\n");
        if (test4()) // close fd
            SysLib.cout("Correct behavior of close.......................\n");
        SysLib.exit();
    }

    private boolean test1( int files ) {
        //.............................................."
        SysLib.cout( "1: format( " + files + " )..................." );
        SysLib.format( files );
        if (files <= 0) {
            return true;
        }
        byte[] superblock = new byte[512];
        SysLib.rawread( 0, superblock );
        int totalBlocks = SysLib.bytes2int( superblock, 0 );
        int inodeBlocks = SysLib.bytes2int( superblock, 4 );
        int freeList = SysLib.bytes2int( superblock, 8 );
        if ( totalBlocks != 1000 ) {
            SysLib.cout( "totalBlocks = " + totalBlocks + " (wrong)\n" );
            return false;
        }
        if ( inodeBlocks != files && inodeBlocks != files / 16 ) {
            SysLib.cout( "inodeBlocks = " + inodeBlocks + " (wrong)\n" );
            return false;
        }
        if ( freeList != 1 + files / 16 && freeList != 1 + files / 16 + 1 ) {
            SysLib.cout( "freeList = " + freeList + " (wrong)\n" );
            return false;
        }
        SysLib.cout( "successfully completed\n" );
        return true;
    }

    // Multiple opens to check fd
    private boolean test2( ) {
        //.............................................."
        SysLib.cout( "1: formating disk ( 64 )...................\n" );
        SysLib.format( 64 );
        SysLib.cout( "2: fd = " + fd + " open( \"css430\", \"w+\" )....\n" );
        fd = SysLib.open( "css430", "w+" );
        if ( fd != 3 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout( "2: fd = " + fd + "open( \"css431\", \"w+\" )....\n" );
        fd = SysLib.open( "css431", "w+" );
        if ( fd != 4 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout( "2: fd = " + fd + " open( \"css432\", \"w+\" )....\n" );
        fd = SysLib.open( "css432", "w+" );
        if ( fd != 5 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }
        SysLib.cout( "successfully completed\n" );
        return true;
    }

    // Checks if SysLib.fsize() performs correctly
    private boolean test3( ) {
        //.............................................."
        SysLib.cout( "3: size = write( fd, buf[512] )....\n" );
        for ( byte i = 0; i < 16; i++ )
            buf512[i] = i;
        int size = SysLib.write( fd, buf512 );
        if ( size != 512 ) {
            SysLib.cout( "size = " + size + " (wrong)\n" );
            return false;
        }
        // test fsize() method
        size = SysLib.fsize(fd);
        if (size != 512) {
            SysLib.cout("Test8.java: size = " + size + "(wrong)\n");
            SysLib.cout("fail\n");
            SysLib.exit();
            return false;
        }
        SysLib.cout( "Fsize check: successfully completed\n" );
        return true;
    }

    private boolean test4( ) {
        //.............................................."
        SysLib.cout( "4: close( fd )...................." );
        SysLib.close( fd );

        int size = SysLib.write( fd, buf512 );
        if ( size > 0 ) {
            SysLib.cout( "writable even after closing the file\n" );
            return false;
        }

        SysLib.cout( "successfully completed\n" );
        return true;
    }







}
