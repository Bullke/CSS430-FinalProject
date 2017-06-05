import java.util.*;

/*
Maintains each file in a different directory entry.
 */
public class Directory
{
    private static int MAX_CHARS = 30;  // max characters of each file name
    private static int BLOCK_SIZE = 4;
    private static short ERROR = -1;

    // Directory entries
    private int fsizes[];               // each element stores a different file size.
    private char fnames[][];            // each element stores a different file name.

    public Directory(int maxInumber)
    { // directory constructor
        if (maxInumber <= 0) {
            // Does not allow to create invalid number of files
            SysLib.cout("The number of files should be > 0\n");
            return;
        }
        fsizes = new int[maxInumber];       // maxInumber = max files
        for (int i = 0; i < maxInumber; i++)
        {
            fsizes[i] = 0;                  // all file size initialized to 0
        }
        fnames = new char[maxInumber][MAX_CHARS];
        String root = "/";                  // entry(inode) 0 is "/"
        fsizes[0] = root.length();          // fsizes[0] is the size of "/".
        root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
    }

    /*
    Assumes data[] received directory information from disk.
    Initializes the Directory instance with this data[].
     */
    public void bytes2directory(byte data[])
    {
        int offset = 0;
        for(int position = 0; position < fsizes.length; position++)
        {
            fsizes[position] = SysLib.bytes2int(data, offset);
            offset += BLOCK_SIZE;
        }
        for(int position = 0; position < fnames.length; position++)
        {
            String fname = new String(data, offset, (MAX_CHARS) * 2);
            fname.getChars(0, fsizes[position], fnames[position], 0);
            offset += (MAX_CHARS * 2);
        }
    }

    /*
    Converts and return Directory information into a plain byte array.
    This byte array will be written back to disk.
     */
    public byte[] directory2bytes()
    {
        int offset = 0;
        byte[] data = new byte[(fsizes.length * 4) + fnames.length * MAX_CHARS * 2];
        for(int position = 0; position < fsizes.length; position++)
        {
            SysLib.int2bytes(fsizes[position], data, offset);
            offset += BLOCK_SIZE;
        }
        for(int position = 0; position < fnames.length; position++)
        {
            String newString = new String(fnames[position], 0, fsizes[position]);
            byte[] tempData = newString.getBytes();
            System.arraycopy(tempData, 0, data, offset, tempData.length);
            offset += (MAX_CHARS * 2);
        }
        return data;
    }

    /*
    Allocates a new inode number for a file to be created.
     */

    public short ialloc(String filename)
    {
        for(short position = 0; position < fsizes.length; position++)
        {
            if(fsizes[position] == 0)
            {
                fsizes[position] = Math.min(MAX_CHARS, filename.length());
                filename.getChars(0, fsizes[position], fnames[position], 0);
                return position;
            }
        }
        return ERROR;
    }

    /*
    Deallocates this inumber (inode number) so that
    the corresponding file will be deleted.
     */
    public boolean ifree(short iNumber)
    {
        if(fsizes[iNumber] > 0)
        {
            fsizes[iNumber] = 0;
            return true;
        }
        return false;
    }

    /*
    Returns the inumber corresponding to this filename.
     */
    public short namei(String filename)
    {

        for(short position = 0; position < fsizes.length; position++)
        {
            if(filename.length() == fsizes[position])
            {
                String tempString = new String(fnames[position], 0, fsizes[position]);
                if(filename.equals(tempString))
                {
                    return position;
                }
            }
        }
        return ERROR;
    }
}