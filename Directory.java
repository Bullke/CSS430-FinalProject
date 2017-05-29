import java.util.*;

public class Directory
{
    private static int MAX_CHARS = 30; // max characters of each file name
    private static int BLOCK_SIZE = 4;
    private static short ERROR = -1;

    // Directory entries
    private int fsizes[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory(int maxInumber)
    { // directory constructor
        fsizes = new int[maxInumber];     // maxInumber = max files
        for (int i = 0; i < maxInumber; i++)
        {
           fsizes[i] = 0;                 // all file size initialized to 0
        }
        fnames = new char[maxInumber][MAX_CHARS];
        String root = "/";                // entry(inode) 0 is "/"
        fsizes[0] = root.length();        // fsizes[0] is the size of "/".
        root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
    }
    public void bytes2directory(byte data[])
    {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
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
    public byte[] directory2bytes()
    {
       	// converts and return Directory information into a plain byte array
       	// this byte array will be written back to disk
       	// note: only meaningfull directory information should be converted
       	// into bytes.
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
    public short ialloc(String filename)
    {
       	// filename is the one of a file to be created.
       	// allocates a new inode number for this filename
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
    public boolean ifree(short iNumber)
    {
       	// deallocates this inumber (inode number)
       	// the corresponding file will be deleted.
    	if(fsizes[iNumber] > 0)
    	{
    		fsizes[iNumber] = 0;
    		return true;
    	}
    	return false;
    }
    public short namei(String filename)
    {
        // returns the inumber corresponding to this filename
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