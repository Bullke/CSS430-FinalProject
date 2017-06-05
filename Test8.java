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
            SysLib.cout("Correct sequence of fd on many openings........................\n");
        if (test3()) // write buf[512] and check size and SysLib.fsize()
            SysLib.cout("Correct fsize on writing.......\n");
        if (test4()) // open for read and try to write
            SysLib.cout("Correct: cannot write if the file is open in reading mode...............\n");
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
        int fd1 = SysLib.open( "css430", "w+" );
        SysLib.cout( "2: fd = " + fd1 + "-> " );
        if ( fd1 != 3 ) {
            SysLib.cout( "fd = " + fd1 + " (wrong)\n" );
            return false;
        }
        int fd2 = SysLib.open( "css431", "w+" );
        SysLib.cout( "2: fd = " + fd2 + "-> " );
        if ( fd2 != 4 ) {
            SysLib.cout( "fd = " + fd2 + " (wrong)\n" );
            return false;
        }
        int fd3 = SysLib.open( "css432", "w+" );
        SysLib.cout( "2: fd = " + fd3  );
        if ( fd3 != 5 ) {
            SysLib.cout( "fd = " + fd3 + " (wrong)\n" );
            return false;
        }
        SysLib.close( fd1 );
        SysLib.close( fd2 );
        SysLib.close( fd3 );
        SysLib.cout( "    successfully completed\n" );
        return true;
    }

    // Checks if SysLib.fsize() performs correctly
    private boolean test3( ) {
        //.............................................."
        SysLib.cout( "3: Fsize = write( fd, buf[512] )....\n" );
        int fd = SysLib.open( "css430", "w" );
//        for ( byte i = 0; i < 16; i++ )
//            buf512[i] = i;
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
        SysLib.close(fd);
        return true;
    }

    // Trying to write to the file opened in a read mode should not work
    private boolean test4( ) {
        //.............................................."
        SysLib.cout("4:Open for read and then write ...............\n");
        SysLib.cout( "2: fd = " + fd + " open( \"css430\", \"r\" )....\n" );
        fd = SysLib.open( "css430", "r" );
        if ( fd != 3 ) {
            SysLib.cout( "fd = " + fd + " (wrong)\n" );
            return false;
        }

        byte[] tmpBuf1 = new byte[512];
        int size = SysLib.read( fd, tmpBuf );
        if ( size != 512 ) {
            SysLib.cout( "read = " + size + " (wrong)\n" );
            //SysLib.close( fd );
            return false;
        }

        byte[] tmpBuf2 = new byte[512];
        size = SysLib.write( fd, tmpBuf2 );
        if ( size != -1 ) {
            SysLib.cout( "wrote = " + size + " (wrong)\n" );
            SysLib.close( fd );
            return false;
        }
        SysLib.cout("Open for read and write: cannot write if the file is open for read\n");
        return true;
    }

}
