package KBHeid;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {

	private static final String ENVIRONMENT = ""; // for minecraft use: System.getenv("APPDATA");
	private static final String FOLDER = "C:/Users/dstro/Desktop/ServerTestFolder/";
	private static final int 	PORT = 25566;

	//Helpers
	private static String readString(DataInputStream in) throws IOException {
		return in.readUTF();
	}

	private static void sendString(DataOutputStream out, String str) throws IOException {
		out.writeUTF(str);
	}

	private static void sendFile(DataOutputStream out, File file) throws  IOException {
		sendString(out, file.getName());
		int fileBytes = (int) file.length();
		out.writeInt(fileBytes);

		InputStream in = new FileInputStream(file);
		byte[] bytes = new byte[8192];
		int count;
		while (fileBytes > 0 && (count = in.read(bytes, 0, Math.min(bytes.length, fileBytes))) != -1) {
			out.write(bytes,0, count);
			fileBytes -= count;
		}
		out.flush();
	}

	private static void getFiles(File folder) {
		ArrayList<File> listOfFiles = new ArrayList<>();
		//listOfFileChecksums = new ArrayList<>();
		//TODO: Will this update if the user puts a new file in??
		for (File f : folder.listFiles()) {
			//String fCheck = getFileChecksum(f);
			//listOfFileChecksums.add(fCheck);
			if(f.isDirectory()){
				getFiles(f);
			}

			//add file to checksum hashmap: filename to filechecksum
			//TODO: this might need to move to a different spot - if so, something else initialized here
			try {
				listOfChecks.put(getFileChecksum(f), f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			listOfFiles.add(f);
		}
	}

	//private static ArrayList<String> listOfFileChecksums;
	private static ServerSocket serverSocket;
	//private static HashMap<String, File> fileMap = new HashMap<>();
	private static HashMap<String, File> listOfChecks = new HashMap<>();

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		serverSocket = new ServerSocket(PORT);

		//Get the first connection
		connect();
	}

	private static void connect() throws IOException {

		/*assert listOfFiles != null;
		for (File f : listOfFiles) {
			fileMap.put(f.getName(), f);
		}*/

		while (true) {
			System.out.println("Awaiting client connection...");
			Socket clientSocket = serverSocket.accept();
			Thread t = new Thread(new Connection(clientSocket));
			t.start();
		}
	}

	private static class Connection implements Runnable {

    	Socket clientSocket;

    	Connection(Socket s){
			clientSocket = s;
		}

		@Override
		public void run() {
			File folder = new File(ENVIRONMENT + FOLDER);
			getFiles(folder);

			ArrayList<String> unmatchedClientFiles = new ArrayList<>();

			System.out.println("\n========  Connection from " + clientSocket.getRemoteSocketAddress() + "  ========\n");

			DataInputStream in;
			try {
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
				DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

				int clientFilesCount = in.readInt();

				//Get all file names. Remove ones that match in our map
				// All files that don't match, add to a list of files we want to remove from client
				//TODO: USE checksums instead of filenames here!!! - Might be Done?
				System.out.println("Client has files: ");
				for (int i = 0; i < clientFilesCount; i++) {
					String checkSum = readString(in);

					//System.out.println("\t" + fileName);
					/*
					if (fileMap.containsKey(fileName)) {
						fileMap.remove(fileName);
					} else {
						unmatchedClientFiles.add(fileName);
					}*/

					//Check checksums for matching items
					if (listOfChecks.containsKey(checkSum)) {
						listOfChecks.remove(checkSum);
					} else {
						unmatchedClientFiles.add(checkSum);
					}
				}

				//Send number of files that the client needs to add
				//out.write(fileMap.size());
				out.writeInt(listOfChecks.size());
				out.flush();

				//this will be broken!! - not sure if we should print checksums
				/*System.out.println("Files to send: ");
				for (String fn : fileMap.keySet()) {
					System.out.println("\t" + fn);
				}*/

				//Send all files in the map
				for (String fn : listOfChecks.keySet()) {
					System.out.println("Sending file: " + fn);
					sendFile(out, listOfChecks.get(fn));
				}

				//Send number of files to delete
				out.writeInt(unmatchedClientFiles.size());

				//Send all files to delete
				System.out.println("Client is deleting files: ");
				for (String fn : unmatchedClientFiles) {
					System.out.println("\t" + fn);
					out.writeUTF(fn);
				}

				out.close();
				in.close();
				clientSocket.close();
				System.out.println("\n======== Connection finished ========\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String getFileChecksum(File file) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		//Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		//Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		//Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}

		//close the stream; We don't need it now.
		fis.close();

		//Get the hash's bytes
		byte[] bytes = digest.digest();

		//This bytes[] has bytes in decimal format;
		//Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (byte aByte : bytes) {
			sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
		}

		//return complete hash
		return sb.toString();
	}
}
