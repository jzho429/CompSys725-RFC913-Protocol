/**
 * Author: Jason Zhou
 * 
 * 
 **/

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;


class TCPServer { 
    //Variables
    private static String command;
    private static String cmd;
    private static String args;
    private static String currentUser;
    private static String currentAcct;
    private static String currentPass;
    private static String LoggedInUser = "";
    private static String message;
    private static String fileType = "b";

    private static boolean validAcct = false;
    private static boolean loggedOn = false;
    private static boolean isConnected = false;
    private static boolean isValid = false;
    private static BufferedReader inFromClient;
    private static DataOutputStream outToClient;
    private static BufferedInputStream dataInFromClient;
    private static DataOutputStream dataOutToClient;
    private static Socket connectionSocket;
    private static final File defaultDir = FileSystems.getDefault().getPath("").toFile().getAbsoluteFile();
    private static File currentDir = defaultDir;
    
    private void sendToClient(String message) throws IOException {
        try{
            outToClient.writeBytes(message + '\0');
        }catch(IOException e){
            System.out.println("Error sending message to client");
            System.exit(0);
        }
    }

    private boolean checkLogIn(String args) throws Exception{
        BufferedReader  reader = new BufferedReader(new FileReader("users.txt"));
        currentUser = "";
        currentAcct = "";
        currentPass = "";
        String line = null;
        try{
            while (true){
                line = reader.readLine();
                if (line != null){
                    String[] parts = line.split("\\,", 3);
                    currentUser = parts[0];
                    currentAcct = parts[1];
                    currentPass = parts[2];
                    if (currentUser.equals(args)){
                        reader.close();
                        return true;
                    }
                }else{
                    break;
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }

    private void USER(String args) throws Exception{
        boolean checkExists = checkLogIn(args);
        System.out.println("user() args = " + args);
        if (checkExists){
            if (LoggedInUser.equals(args)){
                message = "!" + args + " User already logged in";
            }else if(currentUser.equals("admin")){ //Admin bypass
                System.out.println("Adminbypass");
                message = "!"+ currentUser + " logged in";
                sendToClient(message);
                validAcct = true;
                LoggedInUser = currentUser;
                return;
            }else{
                message = "+User-id valid, send account and password";
            }
        }else {
            message = "-Invalid user-id, try again";
        }
        sendToClient(message); 
    }

    private void ACCT(String args) throws Exception{
        if (currentAcct.equals(args)){
            validAcct = true;
            message = "+Account valid, send password";
        }else{
            validAcct = false;
            message = "-Invalid account, try again";
        }
        sendToClient(message);
    }

    private void PASS(String args) throws Exception{

        if (currentPass.equals(args)){
            if (validAcct){
                message = "+Logged in";
                LoggedInUser = currentUser;
            }else{
                message = "+Send account";
            }
        }else{
            message = "-Wrong password, try again";
        }
        sendToClient(message);
    }

    private void TYPE() throws Exception{
        if (args.equalsIgnoreCase("a")){
            fileType = "a";
            message = "+Using Ascii mode";
        }else if (args.equalsIgnoreCase("b")){
            fileType = "b";
            message = "+Using Binary mode";
        }else if (args.equalsIgnoreCase("c")){
            fileType = "c";
            message = "+Using Continuous mode";
        }else {
            message = "-Unrecognised TYPE command";
        }
        sendToClient(message);
    }

    private void LIST() throws Exception{
        String listFormat = "";
        String dir = "";
        SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy kk:mm:ss");
        File path = defaultDir;
        try{
            String[] parts = args.split("\\ ", 2);
            listFormat = parts[0];
            dir = parts[1];
            if (dir.equals("")){
                path = defaultDir;
            } else {
                path = new File(path.toString() + "/" + dir);
            }
        } catch (ArrayIndexOutOfBoundsException e){
            listFormat = args;
            path = currentDir;
        }
        File files[] = path.listFiles();
        String output = String.format("+%-30s %-20s %10s %20s \r\n", "Name", "Date Modified", "Size", "Owner");
        if (!(listFormat.equalsIgnoreCase("V")||listFormat.equalsIgnoreCase("F"))){
            output = "Invalid Format";
            sendToClient(output);
            return;
        }

        for (File file : files){
            String fileName = file.getName();
            if (file.isDirectory()) {
                fileName = fileName.concat("/");
            }
            if (listFormat.equalsIgnoreCase("V")) {
                long timeModified = file.lastModified();
                String dateModified = date.format(timeModified);
                String size;
                if (file.isDirectory()){
                    size = "-";
                }else {
                    size = String.valueOf(file.length());
                }
                String owner = Files.getFileAttributeView(file.toPath(),FileOwnerAttributeView.class).getOwner()
                        .getName();
                output = output.concat(String.format("+%-30s %-20s %10s %20s \r\n", fileName, dateModified, size, owner));
            } else {
                output = output.concat(fileName + "\r\n");
            }
        }
        sendToClient(output);
    }

    private void CDIR() throws Exception{
        String dir = args;
        if (dir.charAt(0) != '~'){
            dir = dir.replaceAll("~", "/");
            currentDir = defaultDir;
        }
        if (dir.charAt(0) != '/'){
            dir = String.format("/%s",dir);
        }
        if (dir.charAt(dir.length()-1) != '/'){
            dir = dir.concat("/");
        }

        File dirPath = new File(dir.toString().concat(dir)).toPath().normalize().toFile();
        if (!dirPath.isDirectory()) {
			message = ("-Directory does not exist");
            sendToClient(message);
            return;
		}
        if (dirPath.compareTo(defaultDir.getAbsoluteFile()) < 0){
            message = ("-Permission denied");
            sendToClient(message);
            return;
        }
        message = "!Changed working dir to " + dirPath.toString();
        currentDir = dirPath;
        sendToClient(message);
    }

    private void KILL() throws Exception{
        Path file = new File(currentDir.toString() + "/" + args).toPath();
        try{
            Files.delete(file);
            message = "+File deleted";
        }catch (NoSuchFileException e){
            message = "-File does not exist";
        }catch (IOException e){
            message = "-Failed";
        }
        sendToClient(message);
    }
    
    private void NAME() throws Exception{
        File fileOld = new File(currentDir.toString() + "/" + args);
        File fileNew;
        if (fileOld.isFile()){
            message = "+File exists, send new name";
            sendToClient(message);
            String newName = TOBE();

            if (newName != ""){
                fileNew = new File(currentDir.toString() + "/" + newName);
                if (fileNew.exists()){
                    message = "-File name already exists";
                }else{
                    message = "+File renamed";
                    fileOld.renameTo(fileNew);
                }
            }else {
                message = "-TOBE <new-file-spec> failed";
            }
            
        }else {
            message = "-File does not exist";
        }

        sendToClient(message);
    }
    private String TOBE() throws Exception{
        String command = inFromClient.readLine();
        String newName = "";
        if (command != null){
            String[] parts = command.split("\\ ", 2);
            command = parts[0];
            newName = parts[1];
        }
        if (command.equalsIgnoreCase("TOBE")){
            return newName;
        }else {
            return "";
        }
    }

    private void DONE() throws IOException{
        isConnected = false;
        message = "+closing connection";
        sendToClient(message);
        connectionSocket.close();
    }

    private void RETR() throws Exception{
        String fileName = args;
        File file = new File(currentDir.toString() + "/" + fileName);
        if (file.isFile()){
            long fileSize = file.length();
            message = Long.toString(fileSize);
            sendToClient(message);
            String command = inFromClient.readLine();
            if (command.equalsIgnoreCase("SEND")){
                byte[] bytes = new byte[(int) file.length()];
                try {
                    FileInputStream fileInStream = new FileInputStream(file);
                    BufferedInputStream bufferedInStream = new BufferedInputStream(fileInStream);
                    int content = 0;
                    while ((content = bufferedInStream.read(bytes)) >= 0){
                        dataOutToClient.write(bytes, 0, content);
                    }
                    fileInStream.close();
                    bufferedInStream.close();
                    dataOutToClient.flush();
                } catch (FileNotFoundException e) {
                    System.out.println("FileNotFoundException");
                } catch (IOException e) {
                    System.out.println("Error reading file");
                }
            }else if (command.equalsIgnoreCase("STOP")){
                message = "+ok, RETR aborted";
                sendToClient(message);
                return;
            } else {
                message = "-Invalid command";
                sendToClient(message);
                return;
            }
        }else{
            message = "-File does not exist";
            sendToClient(message);
            return;
        }
    }

    private void STOR() throws Exception{
        String fileName = args;
        String mode = "";
        Boolean overwrite = false;
        try{
            if (args != null){
                String[] parts = args.split("\\ ", 2);
                mode = parts[0];
                fileName = parts[1];
            }
        }catch(StringIndexOutOfBoundsException e){
            message = "-Invalid command";
            sendToClient(message);
            return;
        }
        File file = new File(currentDir.toString() + "/" + fileName);
        if (mode.equalsIgnoreCase("NEW")){
            if (file.isFile()){
                message = "-File exists, but system doesn't support generations";
                return;
            }else{
                message = "+File does not exist, will create new file";
            }
        }else if(mode.equalsIgnoreCase("OLD")){
            if (file.isFile()){
                overwrite = true;
                message = "+Will write over old file";
            }else{
                message = "+Will create new file";
            }
        }else if(mode.equalsIgnoreCase("APP")){
            if (file.isFile()){
                message = "+Will append to file";
            }else{
                message = "+Will create new file";
            }
        }else {
            message = "-Invalid mode";
        }
        sendToClient(message);

        if (SIZE()){
            long fileSize = Long.parseLong(args);
            System.out.println(fileSize);
            try{
                long availableSpace = Files.getFileStore(currentDir.toPath().toRealPath()).getUsableSpace();
                if (fileSize > availableSpace){
                    message = "-Not enough room, don't send it";
                    return;
                }else {
                    message = "+ok, waiting for file";
                }
            }catch (IOException e){
                message = "-Error getting available space";
                sendToClient(message);
                return;
            }
            sendToClient(message);
            try{
                file.getParentFile().mkdirs();
                file.createNewFile();
                FileOutputStream fileOutStream = new FileOutputStream(file, overwrite);
                BufferedOutputStream bufferedOutStream = new BufferedOutputStream(fileOutStream);
                for (int i=0; i < fileSize; i++){
                    bufferedOutStream.write(dataInFromClient.read());
                }
                fileOutStream.close();
                bufferedOutStream.close();
                message = "+Saved" + " fileName";
            }catch(IOException e){
                message = "-Error saving file";
            }
        }
        sendToClient(message);
    }

    private boolean SIZE() throws Exception{
        command = inFromClient.readLine();
        try{
            if (command != null){
                String[] parts = command.split("\\ ", 2);
                cmd = parts[0];
                args = parts[1];
            }
        }catch (ArrayIndexOutOfBoundsException e){
            message = "-Invalid SIZE command";
            sendToClient(message);
            return false;
        }
        if (cmd.equalsIgnoreCase("SIZE")){
            return true;
        }else {
            return false;
        }    
    }

    //Check if command is valid
    //<command> : = <cmd> [<SPACE> <args>] <NULL>
    private void checkCommand() throws IOException {
        System.out.println("Trying to read ");
        String command = inFromClient.readLine(); 
        System.out.println("Command: " + command);
        try {
            String[] commandArray = command.split("\\ ",2);
            cmd = commandArray[0].toUpperCase();
            args = commandArray[1];    
        } catch (ArrayIndexOutOfBoundsException e){
            cmd = command.toUpperCase();
        }
        try{
            if (cmd != null){
                switch(cmd){
                    case "DONE":
                    System.out.println("done()");
                        DONE();
                        break;
                    case "USER":
                        System.out.println("user()");
                        USER(args);
                        break;
                    case "ACCT":
                        System.out.println("acct()");
                        ACCT(args);
                        break;
                    case "PASS":
                        System.out.println("pass()");  
                        PASS(args);
                        break;
                    default:
                        if (LoggedInUser == currentUser){
                            switch(cmd){
                                case "TYPE":
                                System.out.println("type()");
                                    TYPE();
                                    break;
                                case "LIST":
                                System.out.println("list()");
                                    LIST();
                                    break;
                                case "CDIR":
                                System.out.println("cdir()");
                                    CDIR();
                                    break;
                                case "KILL":
                                System.out.println("kill()");
                                    KILL();
                                    break;
                                case "NAME":
                                System.out.println("name()");
                                    NAME();
                                    break;
                                case "RETR":
                                System.out.println("retr()");
                                    RETR();
                                    break;
                                case "STOR":
                                System.out.println("stor()");
                                    STOR();
                                    break;
                                default:
                                    sendToClient("- invalid command, try again");
                                    checkCommand();
                            }
                        }else {
                            sendToClient("- you are not logged in");
                            checkCommand();
                        }
                }
            }
        }catch(Exception e){
            System.out.println("Error: " + e);
            message = "-An error occured";
            sendToClient(message);
        }
    }



    public static void main(String argv[]) throws Exception { 
        TCPServer server = new TCPServer();
        ServerSocket welcomeSocket = new ServerSocket(6789); 
        while(true) { 
            try {
                while (true){
                    if (!isConnected){
                        System.out.println("Waiting for client to connect");
                        connectionSocket = welcomeSocket.accept(); 
                        System.out.println("Connection accepted");
                        inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
                        outToClient = new DataOutputStream(connectionSocket.getOutputStream()); 
                        dataOutToClient = new DataOutputStream(connectionSocket.getOutputStream());
                        dataInFromClient = new BufferedInputStream(connectionSocket.getInputStream());
                        isConnected = true;
                    }
                    server.checkCommand();
                }
            }
            catch (IOException e){
                welcomeSocket.close();
                System.out.print("Connection failed");
                System.exit(0);
            }
        }
    }
}
