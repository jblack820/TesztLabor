package controller;

//<editor-fold defaultstate="collapsed" desc="IMPORTS">
import app.Main;
import model.Users;
import config.*;
import input.LoadSettings;
import input.LoadTestCenter;
import input.ReadTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import model.ArchivedProjectName;
import model.OsType;
import model.ProductVersionNumber;
import model.ProjectType;
import model.TestCase;
import model.TestCenter;
import model.TestDevice;
import model.TestDevices;
import model.TestDocument;
import model.TestDocumentCreationDTO;
import model.TestProject;
import model.User;
import model.UserRole;
import model.WordDocumentRepo;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import output.SaveDocument;
import output.SaveSettings;
import output.SaveTestCenter;
import util.*;
//</editor-fold>

public class TestCenterController {

//<editor-fold defaultstate="collapsed" desc="PROPERTIES">
    public static boolean isTestCenterFolderLocationNeeded;
    public static boolean isUserCreationNeeded;
    public static User userLoggedIn;
    private TestProject currentTestProject;
    private final TestCenter testCenter;
    private final WordDocumentRepo documentRepo;
    private final ReadTemplate templateService;
    private final SaveDocument documentSave;
    private final SaveTestCenter saveTestCenter;
    private final LoadTestCenter loadTestCenter;
    private final SaveSettings saveSettings;
    private final LoadSettings loadSettings;
    private final Users users;
    private final TestDevices devices;

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CONSTRUCTOR, GETTERS, SETTERS">
    public TestCenterController() {
        this.documentRepo = new WordDocumentRepo();
        this.templateService = new ReadTemplate();
        this.documentSave = new SaveDocument();
        this.testCenter = new TestCenter();
        this.saveTestCenter = new SaveTestCenter();
        this.loadTestCenter = new LoadTestCenter();
        this.saveSettings = new SaveSettings();
        this.loadSettings = new LoadSettings();
        this.users = new Users();
        this.devices = new TestDevices();
    }

    public Users getUsers() {
        return users;
    }

    public static User getUserLoggedIn() {
        return userLoggedIn;
    }

    public static void setUserLoggedIn(User userLoggedIn) {
        TestCenterController.userLoggedIn = userLoggedIn;
    }

    public WordDocumentRepo getDocumentRepo() {
        return documentRepo;
    }

    public ReadTemplate getTemplateService() {
        return templateService;
    }

    public SaveDocument getDocumentSave() {
        return documentSave;
    }

    public TestCenter getTestCenter() {
        return testCenter;
    }

    public TestDevices getDevices() {
        return devices;
    }

    public SaveTestCenter getSaveTestCenter() {
        return saveTestCenter;
    }

    public LoadTestCenter getLoadTestCenter() {
        return loadTestCenter;
    }

    public SaveSettings getSaveSettings() {
        return saveSettings;
    }

    public LoadSettings getLoadSettings() {
        return loadSettings;
    }

    public TestProject getCurrentTestProject() {
        return currentTestProject;
    }

    public void setCurrentTestProject(TestProject testProject) {
        if (null != testProject) {
            currentTestProject = testProject;
        }
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="INIT APPLICATION">
    public void initApplication() {

        if (isTestCenterLocationUnknown()) {
            isTestCenterFolderLocationNeeded = true;
        } else {
            loadTestCenterLocationFromTxt();

            if (!isUserJsonFileExist()) {
                isUserCreationNeeded = true;
            } else {
                loadUsersFromJson();
            }

            if (!isCurrentUserExist()) {
                isUserCreationNeeded = true;
            } else {
                setCurrentUser();
            }

            if (isTestCenterLocationUnknown() && isDevicesJsonExist()) {
                loadDevicesFromJson();
            }
        }
    }

    public boolean isTestCenterLocationUnknown() {
        return null == loadSettings.getTextCenterLocationFromTXT() || loadSettings.getTextCenterLocationFromTXT().equalsIgnoreCase("");
    }

    private void loadTestCenterLocationFromTxt() {
        testCenter.setFolderStructure(new FolderStructure(loadSettings.getTextCenterLocationFromTXT()));

    }

    public void loadUsersFromJson() {
        users.getUserList().addAll(JsonUtils.getUsersFromJson());
    }

    public void setupTestCenterApp(String path) {
        AppUtils.createTestCenterFolderStructure(path);
    }

    public void updateAppSettings(String path) {
        String testCenterFolderLocation = path + AppConfig.TEST_CENTER_FOLDERNAME + "//";
        testCenter.getFolderStructure().setTestCenterLocation(testCenterFolderLocation);
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="SAVE PROJECT SNAPSHOTS">

    public void saveSnapshotOfAProject(TestProject testProject) {
        testProject.createSnapshot();
    }

    public void saveSnapshotsOfALLProjects() {
        for (TestProject testProject : testCenter.getActiveTestProjects()) {
            saveSnapshotOfAProject(testProject);

        }
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE TEST CENTER OBJECT">

    public void scanDocumentsAndCreateProjectObjects() {
        System.out.println("First");
        createArchivedProjectNamesList();
        System.out.println("Starting creating project objects...");
        createProjectObjects(getActiveProjectFolderNames());
        System.out.println("Creating project objects ENDED");
        createTestDocumentObjects();
        System.out.println("Creating test document objects ENDED");
        createTestCaseObjects();
        updateProjectVersionNumbers();
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE PROJECT OBJECTS">
    public void createProjectObjects(File[] projectFolders) {
        testCenter.getActiveTestProjects().clear();

        for (File projectFolder : projectFolders) {
            if (isJsonPresentInFolder(getProjectFolderPath(projectFolder))) {
                System.out.println("Scanning Current project: " + projectFolder.getName());
                addProjectToTestCenter(JsonUtils.getTestProjectFromJSON(getProjectFolderPath(projectFolder)));
            } else {
                addProjectToTestCenter(createTestProjectObjectScanningFolder(projectFolder));
            }
        }
    }

    private TestProject createTestProjectObjectScanningFolder(File projectFolder) {
        FileTime fileTime = getFolderCreationTime(projectFolder);

        TestProject testProject = new TestProject(
                projectFolder.getName(),
                fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                null);
        testProject.setFolderName(projectFolder.getName());

        return testProject;
    }

    private String getProjectFolderPath(File projectFolder) {
        return testCenter.getFolderStructure().getActiveProjectsLocation() + projectFolder.getName() + File.separator;
    }

    private FileTime getFolderCreationTime(File projectFolder) {
        BasicFileAttributes attr = null;
        FileTime fileTime = null;

        try {
            attr = Files.readAttributes(projectFolder.toPath(), BasicFileAttributes.class);
            fileTime = attr.creationTime();
        } catch (IOException ex) {
            Logger.getLogger(TestCenterController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fileTime;
    }

    private void addProjectToTestCenter(TestProject testProject) {
        testCenter.getActiveTestProjects().add(testProject);
    }

    public File[] getActiveProjectFolderNames() {
        return new File(
                testCenter
                        .getFolderStructure()
                        .getActiveProjectsLocation())
                .listFiles(File::isDirectory);
    }

    private boolean isJsonPresentInFolder(String path) {
        return new File(path + AppConfig.PROJECT_PROPERTIES_JSON_FILENAME).exists();
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE TEST DOCUMENT OBJECTS">
    public File[] getWordFilesFromProjectFolder(TestProject testProject) {
        String path = getTestDocumentsFolder(testProject);
        File[] filesArray = new File(path).listFiles();
        File[] docmFilesArray = Arrays
                .stream(filesArray)
                .filter(p -> p.isFile())
                .filter(p -> !p.isHidden())
                .filter(p -> Utils.getFileExtension(p.getPath()).get().equalsIgnoreCase("docm"))
                .toArray(File[]::new);
        return docmFilesArray;
    }

    public File[] getWordFilesFromFolder(String folderPath) {
        //find files in project folder and filters them by extension "*.docm"
        File[] filesArray = new File(folderPath).listFiles();

        File[] docmFilesArray = Arrays
                .stream(filesArray)
                .filter(p -> p.isFile())
                .filter(p -> Utils.getFileExtension(p.getPath()).get().equalsIgnoreCase("docm"))
                .toArray(File[]::new);
        return docmFilesArray;
    }

    public List<TestDocument> createTestDocumentObjectsFromWordFiles(File[] wordFiles) {
        List<TestDocument> moduleList = new ArrayList<>();
        for (File wordFile : wordFiles) {
            String moduleName = getTestDocumentNameFromDocument(wordFile);
            String documentCodeName = getTestDocumentCodeNameFromDocument(wordFile);
            TestDocument testDocument = new TestDocument(moduleName, wordFile, documentCodeName);
            moduleList.add(testDocument);
        }
        return moduleList;
    }

    public String getTestDocumentNameFromDocument(File file) {
        String answer = null;
        XWPFDocument wordFile = Utils.getWordDocument(file);

        if (null != wordFile) {
            if (wordFile.getTables().get(0).getRow(0).getCell(0).getText().contains("TESZT")) {
                answer = wordFile.getTables().get(0).getRow(3).getCell(1).getText();
            } else {
                answer = wordFile.getTables().get(1).getRow(3).getCell(1).getText();
            }
        }
        return answer;
    }

    public String getTestDocumentCodeNameFromDocument(File file) {
        String fullId = null;
        XWPFDocument wordFile = Utils.getWordDocument(file);

        if (null != wordFile) {
            if (wordFile.getTables().get(0).getRow(0).getCell(0).getText().contains("TESZT")) {
                fullId = wordFile.getTables().get(0).getRow(1).getCell(0).getText();
            } else {
                fullId = wordFile.getTables().get(1).getRow(3).getCell(0).getText();
            }
        }
        String[] idArray = fullId.split("-");

        return idArray[idArray.length - 2];
    }

    public void createTestDocumentObjects() {
        for (TestProject currentProject : testCenter.getActiveTestProjects()) {

            File[] wordDocumentList = getWordFilesFromProjectFolder(currentProject);
            if (wordDocumentList.length > 0) {
                System.out.println("docs: "+wordDocumentList.length);
                currentProject.getTestDocuments().addAll(createTestDocumentObjectsFromWordFiles(wordDocumentList));
            }
        }
    }

    public void rescanSingleProject(TestProject testProject) {
        testProject.clearTestDocumentObjects();
        scanAndAddTestModules(testProject);
        createTestCaseObjectsForProject(testProject);
    }

    public TestDocument createTestDocumentObjectForProject(TestProject testProject, String fileName) {
        File wordFile = getWordFileByFileName(testProject, fileName);
        String testDocumentName = getTestDocumentNameFromDocument(wordFile);
        String documentCodeName = getTestDocumentCodeNameFromDocument(wordFile);
        return new TestDocument(testDocumentName, wordFile, documentCodeName);

    }

    private File getWordFileByFileName(TestProject tp, String filename) {
        return Arrays
                .stream(getWordFilesFromProjectFolder(tp))
                .filter(p -> p.getName().equalsIgnoreCase(filename))
                .findFirst().get();
    }

    public void addNewTestDocumentToProject(TestProject testProject, TestDocument newTestDocument) {
        testProject.getTestDocuments().add(newTestDocument);
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE TEST CASE OBJECTS">
    public void createTestCaseObjects() {
        for (TestProject project : testCenter.getActiveTestProjects()) {
            for (TestDocument module : project.getTestDocuments()) {
                createTestCaseObjectsBasedOnTestDocument(module);
            }
        }
    }

    public void createTestCaseObjectsBasedOnTestDocument(TestDocument testDocument) {
        XWPFDocument wordDocument = Utils.getWordDocument(testDocument.getWordFile());
        List<TestCase> testCases = Utils.extractTestCaseObjectsFromXWPFDocument(wordDocument);
        testDocument.getTEST_CASES().addAll(testCases);
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE PROJECT BUG OBJECTS">
    private void createProjectBugObjects(TestProject tp) {

        List<TestCase> failedTestcases = tp.getFailedTestcases();
        for (TestCase tc : failedTestcases) {
            //tc.getDefectLog() = new
        }

    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="RE-SCAN PROJECT VERSION NUMBERS">

    public void updateProjectVersionNumbers() {
        for (TestProject tp : testCenter.getActiveTestProjects()) {
            for (TestCase tc : tp.getAllTestcases()) {
                String versionNumber = tc.getProductVersionNumber();
                if (!tp.isVersionNumberAlreadyRegistered(versionNumber)) {
                    tp.getProductVersionNumbers().add(new ProductVersionNumber(versionNumber, tc.getDateOfTest()));
                }
            }
        }
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE TEST DOCUMENT WORD FILE">
    public void createTestDocument(TestDocumentCreationDTO formData, TestProject project) {
        documentRepo.clear();
        //loop to create multiple pages
        for (int currentPageID = 1; currentPageID <= formData.getNumberOfPagesRequired(); currentPageID++) {
            //1. get getDefectLogTemplate
            XWPFDocument document = templateService.getTemplate(AppConfig.TEST_DOC_TEMPLATE_NAME);
            //2. fill in current Page
            TestDocumentCreation.fillTestDocument(document, currentTestProject, formData, currentPageID);
            //3. add to repo
            documentRepo.addDocument(document);
        }

        //4. save file to disk
        try {
            documentSave.saveMultiPageDocument(documentRepo.getDOCUMENTS(), Utils.getTestDocumentOutputStream(formData, project));
        } catch (IOException ex) {
            Logger.getLogger(TestCenterController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void clearTestDocumentRepo() {
        documentRepo.clear();
    }

    public void saveTestDocument(TestDocumentCreationDTO formData, String path) {
        documentRepo.clear();
        for (int currentPageIndex = 1; currentPageIndex <= formData.getNumberOfPagesRequired(); currentPageIndex++) {
            XWPFDocument document = templateService.getTemplate(AppConfig.TEST_DOC_TEMPLATE_NAME);
            TestDocumentCreation.fillTestDocument(document, currentTestProject, formData, currentPageIndex);
            documentRepo.addDocument(document);
        }

        try {
            documentSave.saveMultiPageDocument(documentRepo.getDOCUMENTS(), Utils.getOutputStream(formData, path));
        } catch (IOException ex) {
            Logger.getLogger(TestCenterController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void saveTestDocument(TestDocumentCreationDTO formData, String path, String wordFileName) {
        documentRepo.clear();
        for (int currentPageIndex = 1; currentPageIndex <= formData.getNumberOfPagesRequired(); currentPageIndex++) {
            XWPFDocument document = templateService.getTemplate(AppConfig.TEST_DOC_TEMPLATE_NAME);
            TestDocumentCreation.fillTestDocument(document, currentTestProject, formData, currentPageIndex);
            documentRepo.addDocument(document);
        }

        try {
            documentSave.saveMultiPageDocument(documentRepo.getDOCUMENTS(), Utils.getTestDocumentOutputStream(currentTestProject, wordFileName));
        } catch (IOException ex) {
            Logger.getLogger(TestCenterController.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("saveTestDocument task finished " + getTime());
    }

    private String getTime() {
        long yourmilliseconds = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        Date resultdate = new Date(yourmilliseconds);
        return sdf.format(resultdate);
    }

    //</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE DEFECT LOG WORD FILE">
    public void createAndSaveDefectLogs(List<TestCase> failedTestCases) {

        failedTestCases.forEach(failedTestCase -> {
            XWPFDocument currentDefectLogTemplate = templateService.getTemplate(AppConfig.DEFECT_LOG_TEMPLATE_NAME);
            DefectLogUtils.FillDefectLog(currentDefectLogTemplate, failedTestCase);
            File targetFolder = makeFolderForDefectlog(currentTestProject, failedTestCase);
            saveDefectLog(targetFolder, failedTestCase, currentDefectLogTemplate);
        });

        // **!!!!!!**
        //notifyUser();
    }

    private File makeFolderForDefectlog(TestProject project, TestCase failedTestCase) {
        File targetDir = new File(
                project.getActiveProjectFolderPath() + "//"
                + AppConfig.DEFECT_LOGS_FOLDERNAME + "//"
                + "HJ-" + failedTestCase.getTestCaseId());

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        return targetDir;
    }

    private void saveDefectLog(File targetDir, TestCase failedTestCase, XWPFDocument currentDocument) {
        String filename = "HJ-" + failedTestCase.getTestCaseId();
        documentSave.saveSingelPageDocument(
                currentDocument,
                Utils.getDefectLogOutputStream(filename, targetDir));
    }

    //</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CREATE NEW PROJECT">
    public void createNewProject(
            String projectName,
            String projectShortName,
            LocalDate dateStarted,
            LocalDate dateDeadline,
            String projectType,
            String versionNumber,
            String appLocation,
            String browser) {

        TestProject newTestProjectObject = createTestProjectObject(projectName, projectShortName, dateStarted, dateDeadline);
        newTestProjectObject.setProjectType(ProjectType.getProjectTypeBasedOnName(projectType));
        newTestProjectObject.setFolderName(Utils.getFileSystemFriendyName(projectName));

        if (null != versionNumber && !versionNumber.equalsIgnoreCase("")) {

            newTestProjectObject.addNewVersionNumber(versionNumber);
        }
        if (null != appLocation && !appLocation.equalsIgnoreCase("")) {
            newTestProjectObject.setAppLocation(appLocation);
        }
        if (null != browser && !browser.equalsIgnoreCase("")) {
            newTestProjectObject.setBrowserTestedOn(browser);
        }

        addProjectObjectToTestCenter(newTestProjectObject);
        createProjectFolderInFileSystem(projectName);
        createJsonConfigFileInFolder(newTestProjectObject);
        createSubFolders(newTestProjectObject);

    }

    private void createProjectFolderInFileSystem(String projectName) {

        //String projectNameWithTimeStamp = Utils.addTimeStampToStringAtTheBeginning(projectName);
        AppUtils.createFolder(
                testCenter.getFolderStructure().getActiveProjectsLocation(),
                Utils.getFileSystemFriendyName(projectName));
    }

    private void createJsonConfigFileInFolder(TestProject tp) {
        JsonUtils.saveTestProjectJSON(tp);
    }

    private TestProject createTestProjectObject(String projectName, String projectShortName, LocalDate dateStarted, LocalDate dateDeadline) {
        TestProject tp = new TestProject(projectName, projectShortName, dateStarted, dateDeadline);
        tp.setFolderName(Utils.getFileSystemFriendyName(projectName));
        return tp;
    }

    private void createSubFolders(TestProject tp) {
        AppUtils.createFolder(tp.getActiveProjectFolderPath(), config.AppConfig.TEST_DOCUMENTS_FOLDERNAME);
        AppUtils.createFolder(tp.getActiveProjectFolderPath(), config.AppConfig.DEFECT_LOGS_FOLDERNAME);
    }

    private void addProjectObjectToTestCenter(TestProject newTestProjectObject) {
        addProjectToTestCenter(newTestProjectObject);
    }

    private String getTestDocumentsFolder(TestProject testProject) {
        return testProject.getActiveProjectFolderPath() + config.AppConfig.TEST_DOCUMENTS_FOLDERNAME + File.separator;
    }

    private void scanAndAddTestModules(TestProject currentProject) {
        File[] wordDocumentList = getWordFilesFromProjectFolder(currentProject);
        if (wordDocumentList.length > 0) {
            currentProject.getTestDocuments().addAll(createTestDocumentObjectsFromWordFiles(wordDocumentList));
        }
    }

    private void createTestCaseObjectsForProject(TestProject currentProject) {
        for (TestDocument testDocument : currentProject.getTestDocuments()) {
            createTestCaseObjectsBasedOnTestDocument(testDocument);
        }
    }

    private void convertKMAAOldDocumentsToNew() {
        File[] wordFiles = getWordFilesFromFolder("C:\\TestCenter\\Active\\KMA_ARCHIVUM\\TestDocuments");

        for (File wordFile : wordFiles) {
            InputStream is = null;
            try {
                is = new FileInputStream(wordFile);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TestCenterController.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                ConvertOldDocument.convertOldDocumentToNew(new XWPFDocument(is));

            } catch (IOException ex) {
                Logger.getLogger(TestCenterController.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    //</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CLONE TEST DODUMENTS TO ANOTHER PROJECT NAME">
    public void initProjectCloning(String testerName, String sourceProjectName, String targetProjectName) {
        CloneUtils.cloneAllTestDocumentToAnotherProject(
                getTestCenter().getTestProjectByName(sourceProjectName),
                getTestCenter().getTestProjectByName(targetProjectName),
                testerName);
    }

    public void initSingleDocumentCloning(String sourceProjectName, String testDocumentName, String targetProjectName, String testerName) {
        TestProject sourceProject = getTestCenter().getTestProjectByName(sourceProjectName);
        CloneUtils.cloneSingleDocumentToAnotherProject(
                sourceProject,
                sourceProject.getTestDocumentByName(testDocumentName).getWordFile(),
                getTestCenter().getTestProjectByName(targetProjectName),
                testerName);
    }

//</editor-fold>    
//<editor-fold defaultstate="collapsed" desc="TEST DEVICES">
    public void createNewDevice(String name, String brand, String model, String osType) {
        addDeviceObject(name, brand, model, osType);
        saveDevicesJson();
    }

    private void loadDevicesFromJson() {
        devices.loadDevicesFromJSON();
    }

    private void addDeviceObject(String name, String brand, String model, String osType) {
        TestDevice device = new TestDevice(name, brand, model, OsType.getOsTypeByName(osType));

        devices.addDevice(device);
    }

    private void saveDevicesJson() {
        JsonUtils.saveDevicesToJSON(devices.getDevicesList());

    }

    public void removeTestDeviceFromJSON(TestDevice td) {
        JsonUtils.saveDevicesToJSON(
                devices.getDevicesList()
                        .stream()
                        .filter(p -> !p.equals(td))
                        .collect(Collectors.toList()));
    }

    private boolean isDevicesJsonExist() {
        return new File(app.Main.controller
                .getTestCenter()
                .getFolderStructure()
                .getTestCenterLocation()
                + AppConfig.TEST_DEVICES_JSON_FILENAME)
                .exists();
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="USER">

    public void setCurrentUser() {
        userLoggedIn = getUsers().getUserByUserKey(System.getProperty("user.home"));

    }

    public void createNewLoggedInUser(String userKey, String fullName, List<UserRole> roles) {
        addUserObject(userKey, fullName, roles);
        setCurrentUser();
        saveUsersJsonFile();
        isUserCreationNeeded = false;
    }

    public void createNewUser(String userKey, String fullName, List<UserRole> roles) {
        addUserObject(userKey, fullName, roles);
        saveUsersJsonFile();
    }

    public void reScanUsersJSON() {
        users.updateUsers(JsonUtils.getUsersFromJson());
    }

    public void reScanDevicesJSON() {
        devices.loadDevicesFromJSON();
    }

    public void updateUsersJsonFile() {
        saveUsersJsonFile();
    }

    private void addUserObject(String userKey, String fullName, List<UserRole> roles) {
        User u = new User(userKey, fullName, roles);
        getUsers().addUser(u);
    }

    private void saveUsersJsonFile() {

        JsonUtils.saveSetofUsersToJSON(users.getUserList());
    }

    private boolean isUserJsonFileExist() {
        return new File(app.Main.controller
                .getTestCenter()
                .getFolderStructure()
                .getTestCenterLocation()
                + AppConfig.USER_LIST_JSON_FILE_NAME)
                .exists();
    }
    
        private boolean isCurrentUserExist() {
        return isUserJsonFileExist() && getUsers().isUserExist(System.getProperty("user.home"));
    }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="ARCHIVE PROJECT UTILS">
    public void archiveProjects(List<TestProject> projects) {
        if (null != projects && !projects.isEmpty()) {
            projects.stream().forEach(ArchiveProjectUtils::moveActiveProjectToArchivedFolder);
        }
    }
    
    public void reActivateProjects(List<String> folderNames) {
        if (null != folderNames && !folderNames.isEmpty()) {
            folderNames.stream().forEach(ArchiveProjectUtils::moveArchivedProjectToActiveFolder);
        }
    }
    
    
    
    public void reScanArchivedProjects() {
        createArchivedProjectNamesList();
    }
    
    private void createArchivedProjectNamesList() {
        Main.controller.getTestCenter().getARCHIVED_PROJECT_NAMES().clear();
        File directoryPath = new File(testCenter.getFolderStructure().getArchivedProjectsLocation());
        File archivedProjectFolders[] = directoryPath.listFiles();

        List<ArchivedProjectName> list = new ArrayList<>();

        List<String> folderNames = Arrays.asList(archivedProjectFolders)
                .stream()
                .filter(p -> p.isDirectory())
                .map(p -> p.getName())
                .collect(Collectors.toList());

        for (String folderName : folderNames) {
            
            String projectName = getArchivedProjectNameBasedOnFolderName(folderName);
            ArchivedProjectName nameObject = new ArchivedProjectName(folderName, projectName);
            list.add(nameObject);
            
        }
        Main.controller.getTestCenter().getARCHIVED_PROJECT_NAMES().addAll(list);
        
    }
    
    private String getArchivedProjectNameBasedOnFolderName(String folderName) {
        String path
                = Main.controller
                        .getTestCenter()
                        .getFolderStructure()
                        .getArchivedProjectsLocation()
                + folderName;
        return JsonUtils.getProjectNameFromProjectJson(path);
    }
//</editor-fold>
}
