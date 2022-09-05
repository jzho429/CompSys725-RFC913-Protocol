package client;


import java.io.*; 
import java.net.*; 
import java.nio.file.*;

class TCPClient { 
    private static String command;
    private static String cmd;
    private static String currentUser;
    private static String currentDir;

    private static long fileSize;
    
    private static Boolean isConnected = false;

    private static File storFile;
    private static Socket clientSocket;
    private static BufferedReader inFromUser;
    private static DataOutputStream outToServer;
    private static BufferedReader inFromServer;
    private static DataOutputStream fileOutToServer;
    private static BufferedInputStream fileInFromClient;

    public void initConnection() throws IOException{
        inFromUser = new BufferedReader(new InputStreamReader(System.in));
        try{
            clientSocket = new Socket("localhost", 6789);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            fileOutToServer = new DataOutputStream(clientSocket.getOutputStream());
            fileInFromClient = new BufferedInputStream(clientSocket.getInputStream());
            System.out.println("connection to server is established");
            isConnected = true;
        }catch (Exception e){
            System.out.println("Connection to server failed");
            System.exit(0);
            clientSocket.close();
            isConnected = false;
        }        
    }
    public void receiveFile(File file, long fileSize, boolean overwrite) throws Exception{
        try{
            file.getParentFile().mkdirs();
            file.createNewFile();
            FileOutputStream fileOutStream = new FileOutputStream(file, overwrite);
            BufferedOutputStream bufferedOutStream = new BufferedOutputStream(fileOutStream);
            for (int i=0; i<fileSize; i++){
                bufferedOutStream.write(fileInFromClient.read());
            }
            bufferedOutStream.close();
			fileOutStream.close();
            System.out.println("Saved to /" + currentUser);
        }catch (ConnectException e) {
			System.out.println("server is offline");
			System.exit(0);
		}catch (SocketException e) {
			System.out.println("server went offline");
			System.exit(0);
		}	
    }
    public void sendFile(File file){
        byte[] bytes = new byte[(int) file.length()];
        try{
            FileInputStream fileInStream = new FileInputStream(file);
			BufferedInputStream bufferedInStream = new BufferedInputStream(fileInStream);
			int content = 0;
			while ((content = bufferedInStream.read(bytes)) >= 0) {
				fileOutToServer.write(bytes, 0, content);
			}
            fileInStream.close();
			bufferedInStream.close();
			fileOutToServer.flush();
			System.out.println("File sent");

        }catch (FileNotFoundException e) {
			System.out.println("sendFile FileNotFoundException");
		} 
		catch (IOException e) {
			System.out.println("sendFile IOException");
		}	
    }

    public void checkCommand() throws Exception{
        try {
            System.out.print("Command: ");
            command = inFromUser.readLine();
            if (command != null){
                try{
                    cmd = command.substring(0,4);
                    cmd = cmd.toUpperCase();
                }catch(StringIndexOutOfBoundsException e){
                    System.out.println("Invalid command");
                    checkCommand();
                }
                if (cmd.equals("USER")){
                    outToServer.writeBytes(command + "\n");
                    String[] parts = command.split("\\ ", 2);
                    currentUser = parts[1];
                    currentDir = FileSystems.getDefault().getPath(currentUser).toFile().getAbsoluteFile().toString();
                }
                else if (cmd.equals("SEND")){
                    outToServer.writeBytes(command + "\n");
                    System.out.println("Save file as: ");
                    String fileName = inFromUser.readLine();
                    File file = new File(currentDir.toString() + "/" + fileName);
                    receiveFile(file, fileSize, true);
                    checkCommand();
                }
                else if (cmd.equals("STOR")){
                    outToServer.writeBytes(command + "\n");
                    System.out.println("Enter new file name: ");
                    String fileName = inFromUser.readLine();
                    storFile = new File(currentDir.toString() + "/" + fileName);
                    System.out.println(storFile.toString());
                    try {
                        FileInputStream fileInStream = new FileInputStream(storFile);
                        BufferedInputStream bufferedInStream = new BufferedInputStream(fileInStream);
                        System.out.println("Size: " + bufferedInStream.available());
                        fileInStream.close();
                        bufferedInStream.close();
                    } catch (FileNotFoundException e) {
                        System.out.println("File not found");
                        checkCommand();
                    }
                }
                else{
                    outToServer.writeBytes(command + "\n");
                }
            }
        }catch(Exception e){
            System.out.println("Invalid command");
            e.printStackTrace();
        }
    }

    public void processServerResponse() throws IOException{
        String response = "";
        int character;
        while (true){
            character = inFromServer.read();
            if (character == 0){
                break;
            }
            response = response.concat(Character.toString((char)character));
        }
        if (response.equalsIgnoreCase("+ok, waiting for file")) {
            sendFile(storFile);
            System.out.println("Sending file");
        }
        if (response.charAt(0) == '-' || response.charAt(0) == '+' || response.charAt(0) == '!') {
            System.out.println(response);
        } else {
            fileSize = Long.parseLong(response.replaceAll("\\s", ""));
            System.out.println("File size: " + fileSize);
        }
    }
    
    public static void main(String argv[]) throws Exception 
    { 
        TCPClient client = new TCPClient();
        try{
            client.initConnection();
            if(isConnected){
                while(true){
                    client.checkCommand();
                    client.processServerResponse();
                }
            }
        }catch (ConnectException e) {
			System.out.println("server is offline");
			System.exit(0);
		}
		catch (SocketException e) {
			System.out.println("server went offline");	
			System.exit(0);
		}		
    } 
} 