import java.util.*;

public class Directory
{
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsizes[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory(int maxInumber)
    { // directory constructor
       fsizes = new int[maxInumber];     // maxInumber = max files
       for (int i = 0; i < maxInumber; i++)
       {
          fsize[i] = 0;                 // all file size initialized to 0
       }
       fnames = new char[maxInumber][maxChars];
       String root = "/";                // entry(inode) 0 is "/"
       fsizes[0] = root.length();        // fsize[0] is the size of "/".
       root.getChars(0, fsizes[0], fnames[0], 0); // fnames[0] includes "/"
    }
    public int bytes2directory(byte data[])
    {
       // assumes data[] received directory information from disk
       // initializes the Directory instance with this data[]
   	   int offset = 0;
   	   for(int position = 0; position < fsizes.length; position++ offset += 4)
   	   {
   	   	  fsizes[position] = SysLib.bytes2int(data, offset);
   	   }
   	   for(int position = 0; position < fnames.length; position++, offset += (maxChars * 2))
   	   {
   	   	  String fname = new String(data, offset, (maxChars) * 2);
   	   	  fname.getChars(0, fsizes[position], fnames[position], 0);
   	   }
    }
    public byte[] directory2bytes()
    {
       // converts and return Directory information into a plain byte array
       // this byte array will be written back to disk
       // note: only meaningfull directory information should be converted
       // into bytes.
    }
    public short ialloc(String filename)
    {
       // filename is the one of a file to be created.
       // allocates a new inode number for this filename
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
    }
}