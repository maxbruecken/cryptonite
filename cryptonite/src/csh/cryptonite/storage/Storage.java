// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

// Copyright (c) 2012, Christoph Schmidt-Hieber

package csh.cryptonite.storage;

import java.io.File;
import java.io.IOException;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import csh.cryptonite.Cryptonite;
import csh.cryptonite.R;
import csh.cryptonite.SelectionMode;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

public abstract class Storage {

    public static final int STOR_UNDEFINED=-1, STOR_LOCAL=0, STOR_DROPBOX=1;
    public static final String ENCFS_XML_VOLD_REGEX = "\\.encfs";
    public static final String ENCFS_XML_V6_REGEX = "\\.encfs.\\.xml";
    public static final String ENCFS_XML_V7_REGEX = "\\.encfs\\.txt";
    public static final String ENCFS_XML_CURRENT  = ".encfs6.xml";

    public int type;
    public int fdSelectionMode;
    public int selectExportMode;
    public int exportMode;
    public int uploadMode;
    public int waitStringId;
    public String browsePnt;
    
    protected Context mCallingContext;
    protected Context mAppContext;
    protected SherlockFragmentActivity mActivity;
    protected String encFSPath;
    protected String encFSConfigPath;
    
    private UIHandler uiHandler;
    
    public Storage(SherlockFragmentActivity activity) {
        mActivity = activity;
        mCallingContext = mActivity.getBaseContext();
        mAppContext = mActivity.getApplicationContext();
        type = STOR_UNDEFINED;
        fdSelectionMode = SelectionMode.MODE_OPEN_MULTISELECT;
        selectExportMode = Cryptonite.SELECTLOCALEXPORT_MODE;
        exportMode = Cryptonite.LOCALEXPORT_MODE;
        uploadMode = Cryptonite.SELECTLOCALUPLOAD_MODE;
        waitStringId = 0;
        browsePnt = "";
        encFSPath = "";
        encFSConfigPath = "";
        Thread uiThread = new HandlerThread("UIHandler");
        uiThread.start();
        uiHandler = new UIHandler(((HandlerThread) uiThread).getLooper());
    }
    abstract public boolean initEncFS(String srcDir, String initRoot, String configPath);
    
    abstract public boolean encryptEncFSFile(String stripstr, String srcPath);

    abstract public AsyncTask<Void, Long, Boolean> uploadEncFSFile(SherlockFragmentActivity activity, String stripstr);

    abstract public boolean decryptEncFSFile(String encodedPath, String targetPath);

    abstract public Cryptonite.DecodedBuffer decryptEncFSFileToBuffer(String encodedPath);
    
    abstract public boolean exportEncFSFiles(String[] exportPaths, String exportRoot, 
            String destDir);
    
    abstract public boolean createEncFS(String currentReturnPath, String passwordString, 
            File browseRoot, int config);
    
    abstract public boolean deleteFile(String path);
    
    abstract public String encodedExists(String stripstr);
    
    abstract public boolean mkVisibleDecoded(String path, String encFSRoot, String rootPath);
    
    abstract public void mkVisiblePlain(String path, String rootPath);
    
    abstract public boolean mkDirEncrypted(String encodedPath);

    abstract public boolean mkDirPlain(String plainPath);
    
    abstract public boolean exists(String plainPath);

    public String stripStr(String encFSFilePath, String fileRoot, String srcPath) {
        File srcFile = new File(srcPath);
        if (!srcFile.isFile()) {
            handleUIToastRequest(mAppContext.getString(R.string.only_files));
            return "";
        }
        String srcFileName = srcFile.getName();

        /* normalise path names */
        String bRoot = new File(fileRoot).getPath();
        String bPath = new File(encFSFilePath).getPath();

        String stripstr = bPath.substring(bRoot.length()) + "/" + srcFileName;
        if (!stripstr.startsWith("/")) {
            stripstr = "/" + stripstr;
        }
        return stripstr;
    }
    
    /** Create an empty file with a decoded file name
     * 
     * @param encodedPath The full encoded source path
     * @param destRoot Root destination directory path
     * @param isDir true for directories
     * @throws IOException
     */
    public static void decode(String encodedPath, String destRoot, boolean isDir) throws IOException {

        /* Decoded name */
        /* Don't decode encfs?.xml */
        String encfsXmlRegex = "\\.encfs.\\.xml";
        if (new File(encodedPath).getName().matches(encfsXmlRegex)) {
            return;
        }
        
        String decodedPath = Cryptonite.jniDecode(encodedPath);
        
        VirtualFile file = new VirtualFile(destRoot + "/" + decodedPath);
        if (isDir) {
            file.mkdirs();
        } else {
            file.createNewFile();
        }
        
    }

    public static String fileExt(String url) {
        /* file name part: */
        String rawFileName = new File(url).getName();
        
        /* Does the file name have an extension at all? */
        if (rawFileName.lastIndexOf(".") == -1) {
            return "";
        }

        String ext = url.substring(url.lastIndexOf(".") );
        if (ext.indexOf("?")>-1) {
            ext = ext.substring(0,ext.indexOf("?"));
        }
        if (ext.indexOf("%")>-1) {
            ext = ext.substring(0,ext.indexOf("%"));
        }
        return ext;
    }
    
    public static String fileNameTrunk(String url) {
        /* file name part: */
        String rawFileName = new File(url).getName();
        
        /* Does the file name have an extension at all? */
        if (rawFileName.lastIndexOf(".") == -1) {
            return rawFileName;
        }
        
        String trunk = url.substring(0, url.lastIndexOf("."));
        return new File(trunk).getName();
    }
    
    public File getPrivateDir(String label, int mode) {
        /* Tear down and recreate the browse directory to make
         * sure we have appropriate permissions */
        File browseDirF = mAppContext.getDir(label, mode);
        if (browseDirF.exists()) {
            if (!Cryptonite.deleteDir(browseDirF)) {
                handleUIToastRequest(mAppContext.getString(R.string.target_dir_cleanup_failure));
                return null;
            }
        }
        browseDirF = mAppContext.getDir(label, mode);
        return browseDirF;
    }
    
    private final class UIHandler extends Handler
    {
        public static final int DISPLAY_UI_TOAST = 0;

        public UIHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
            case UIHandler.DISPLAY_UI_TOAST:
            {
                Toast t = Toast.makeText(mAppContext, (String)msg.obj, Toast.LENGTH_LONG);
                t.show();
                break;
            }
            default:
                break;
            }
        }
    }

    protected void handleUIToastRequest(int resId) {
        handleUIToastRequest(mAppContext.getString(resId));
    }
    
    protected void handleUIToastRequest(String message)
    {
        Message msg = uiHandler.obtainMessage(UIHandler.DISPLAY_UI_TOAST);
        msg.obj = message;
        uiHandler.sendMessage(msg);
    }

    public String getEncFSPath() {
        return encFSPath;
    }
    
    public void setEncFSPath(String value) {
        encFSPath = value;
    }

    public String getEncFSConfigPath() {
        return encFSConfigPath;
    }
    public void setEncFSConfigPath(String encFSConfigPath) {
        this.encFSConfigPath = encFSConfigPath;
    }

}
