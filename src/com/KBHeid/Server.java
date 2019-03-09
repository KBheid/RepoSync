package com.KBHeid;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {

	private static final String ENVIRONMENT = System.getenv("APPDATA");
	private static final String FOLDER = "/.minecraft/mods";
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

	private static File[] listOfFiles;
	private static ServerSocket serverSocket;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		File folder = new File(ENVIRONMENT + FOLDER);
		listOfFiles = folder.listFiles();

		serverSocket = new ServerSocket(PORT);

		//Get the first connection
		while (true) {
			connect();
		}
	}

	private static void connect() throws IOException {
    	System.out.println("Awaiting client connection...");
		ArrayList<String> unmatchedClientFiles = new ArrayList<>();

		HashMap<String, File> fileMap = new HashMap<>();

		assert listOfFiles != null;
		for (File f : listOfFiles) {
			fileMap.put(f.getName(), f);
		}

		Socket clientSocket = serverSocket.accept();
		System.out.println("\n========  Connection from " + clientSocket.getRemoteSocketAddress() + "  ========\n");

		DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

		int clientFilesCount = in.readInt();

		//Get all file names. Remove ones that match in our map
		// All files that don't match, add to a list of files we want to remove from client
		System.out.println("Client has files: ");
		for (int i=0; i<clientFilesCount; i++) {
			String fileName = readString(in);
			System.out.println("\t" + fileName);

			if (fileMap.containsKey(fileName)) {
				fileMap.remove(fileName);
			}
			else {
				unmatchedClientFiles.add(fileName);
			}
		}

		//Send number of files that the client needs to add
		out.write(fileMap.size());
		out.flush();

		System.out.println("Files to send: ");
		for (String fn : fileMap.keySet()) {
			System.out.println("\t" + fn);
		}

		//Send all files in the map
		for (String fn : fileMap.keySet()) {
			System.out.println("Sending file: " + fn);
			sendFile(out, fileMap.get(fn));
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
	}

	/*
	private static String getFileChecksum(MessageDigest digest, File file) throws IOException
	{
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
	*/
}
