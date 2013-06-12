/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sic.software.kpp;

import hudson.XmlFile;
import hudson.model.Hudson;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import sic.software.kpp.model.KPPKeychain;

/**
 *
 * @author michaelbar
 */
public class KPPManagement {
    private final static Logger LOGGER = Logger.getLogger(KPPManagement.class.getName());
    private final static String CLASS_NAME = KPPManagement.class.getName();
    
    private final static String CONFIG_XML = "sic.software.kpp.KPPManagement.xml";
    private final static File KPP_UPLOAD_DIRECTORY = new File(Hudson.getInstance().getRootDir() + File.separator + "kpp_upload");
    
    private static KPPManagement management;
    private List<KPPKeychain> keychains;
    
    
    /**
     * Constructor
     */
    public KPPManagement() {
        
    }
    
    public static KPPManagement getInstance() {
        if (management == null) {
            management = init();
        }
        management.update();
        return management;
    }
    
    public void doUploadKeychain(StaplerRequest req, StaplerResponse rsp) throws
            ServletException,
            IOException,
            NoSuchAlgorithmException {
        
        FileItem file = req.getFileItem("keychain.file");
        if (file == null || file.getSize() == 0) {
            throw new ServletException("no file selected");
        }
        
        // save uploaded file
        byte[] fileData = file.get();
        File toUploadFile = new File(KPP_UPLOAD_DIRECTORY, file.getName());
        OutputStream os = new FileOutputStream(toUploadFile);
        try {
            os.write(fileData);
        } finally {
            os.close();
        }
        
        rsp.sendRedirect2("../manageZpp/"); //we stay on page
    }
    
    public void doSave(StaplerRequest req, StaplerResponse rsp) throws
            ServletException,
            IOException,
            NoSuchAlgorithmException {
        
    }
    
    public static void save(KPPManagement config) throws IOException {
        LOGGER.entering(CLASS_NAME, "save");
        getConfigXML().write(config);
        LOGGER.exiting(CLASS_NAME, "save");
    }
    
    private static KPPManagement init() {
        
        // create upload directory for keychains and provisioning profiles
        if (!KPP_UPLOAD_DIRECTORY.exists()) {
            KPP_UPLOAD_DIRECTORY.mkdir();
        }
        
        LOGGER.entering(CLASS_NAME, "load");
        try {
            return (KPPManagement)getConfigXML().read();
        }
        catch(FileNotFoundException e) {
            LOGGER.log(Level.WARNING, "No configuration found.");
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load configuration from " + CONFIG_XML, e);
        }
        
        return new KPPManagement();
    }
    
    private void clear() {
        getKeychains().clear();
    }
    
    private void update() {
        clear();
        
        // update stored keychains
        File[] keychainFiles = KPP_UPLOAD_DIRECTORY.listFiles(new KeychainFileNameFilter());
        for(File keychainFile : keychainFiles) {
            KPPKeychain keychain = new KPPKeychain(keychainFile.getName());
            addKeychain(keychain);
        }
    }
    
    private static XmlFile getConfigXML() {
        return new XmlFile(new File(Hudson.getInstance().getRootDir(), CONFIG_XML));
    }
    
    public List<KPPKeychain> getKeychains() {
        if (keychains == null) {
            keychains = new ArrayList<KPPKeychain>();
        }
        return keychains;
    }
    
    private void addKeychain(KPPKeychain keychain) {
        // keychain must hava a general name
        if(StringUtils.isBlank(keychain.getFileName())) {
            return;
        }
        getKeychains().add(keychain);
    }
    
    private class KeychainFileNameFilter implements FilenameFilter {

        public boolean accept(File file, String name) {
            boolean ret = false;
            if (file.isDirectory() && name.endsWith(".keychain")) { // keychains are directories
                ret = true;
            }
            return ret;
        }
        
    }
    
}