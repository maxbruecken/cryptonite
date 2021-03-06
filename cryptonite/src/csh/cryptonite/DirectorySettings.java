package csh.cryptonite;

import java.io.File;

import android.os.Environment;

public enum DirectorySettings {
    INSTANCE;

    public static final String OPENPNT = "open";
    public static final String BROWSEPNT = "browse";
    public static final String DROPBOXPNT = "dropbox";
    public static final String READPNT = "read";

    public File openDir, readDir;
    
    public String binDirPath;
    public String encFSBin;

    public String currentBrowsePath;
    public String currentBrowseStartPath;
    
    public String mntDir;

    public boolean hasBin() {
        return new File(encFSBin).exists();
    }
    
    public static File getGlobalExternalStorageDirectory() {
        if (!ShellUtils.isAndroid42()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return new File("/sdcard");
        }
    }
}
