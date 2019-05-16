package com.KBHeid;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.HashMap;

public class Client {
	private static final String ENVIRONMENT = ""; // for minecraft use: System.getenv("APPDATA");
	private static final String FOLDER = "";
	private static final String SERVER_IP = "199.247.68.80";
	private static final int	SERVER_PORT = 25566;

	private static Socket conn;
	private static DataOutputStream out;
	private static DataInputStream in;

	/* =============================================
	 * =======        Window methods        ========
	 * =============================================
	 */
	private static void createWindow() {
		pane = new JFrame();
		pane.setVisible(true);
		pane.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		pane.setLocationRelativeTo(null);
		pane.pack();
	}

	private static void updateWindowToStringPrompt() {

	}

	private static void updateWindow(String title, String text) {
		pane.getContentPane().removeAll();
		pane.setTitle(title);
		JLabel label = new JLabel(text);
		pane.add(label);
		pane.pack();
		SwingUtilities.updateComponentTreeUI(pane);
	}

	private static void updateWindow(String text) {
		pane.getContentPane().removeAll();
		JLabel textField = new JLabel(text);
		pane.add(textField);
		pane.pack();
		SwingUtilities.updateComponentTreeUI(pane);
	}

	/* =============================================
	 * =======      Connection methods      ========
	 * =============================================
	 */

	private static void startConnection(String ip, int port) throws IOException {
		conn = new Socket(ip, port);
		in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
		out = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
	}

	private static void stopConnection() throws IOException {
		in.close();
		out.close();
		conn.close();
		System.out.println("Connection Finished");
		System.out.println("========================");
	}

	private static void sendString(DataOutputStream out, String str) throws IOException{
		out.writeUTF(str);
		out.flush();
	}

	private static void readFile(DataInputStream in) throws IOException {
		//getting the name and length of the file being sent
		String fileName = readString(in);
		System.out.println("\t" + fileName);
		int lenOfFile = in.readInt();
		byte[] fileBytes = new byte[8192];

		//setting up a file writer
		FileOutputStream fileWriter = new FileOutputStream(ENVIRONMENT + FOLDER + "/" + fileName);

		//getting file from server
		int count;
		while (lenOfFile > 0 && (count = in.read(fileBytes, 0, Math.min(fileBytes.length, lenOfFile))) != -1) {
			fileWriter.write(fileBytes,0,count);
			lenOfFile -= count;
		}
	}

	/* =============================================
	 * =======         Main methods         ========
	 * =============================================
	 */
	private static File[] modList;
	private static HashMap<String, File> fileMap = new HashMap<>();

	private static JFrame pane;

	public static void main(String[] args) throws IOException, InterruptedException {
		//navigating to where the mod files live
		String path = ENVIRONMENT + FOLDER;

		createWindow();
		updateWindow("Fetching files", "Getting your mods");

		File folder = new File(path);
		modList = folder.listFiles();

		//making and populating a hashmap of files
		fileMap = new HashMap<>();

		assert modList != null;
		for (File f : modList) {
			// Ignore files that start with __
			if (!f.getName().startsWith("__")) {
				fileMap.put(f.getName(), f);
			}
		}

		updateWindow("Connecting...", "Currently connecting to server...");

		// Try to connect -- if there is already a connection, wait 3 seconds before reattempting.
		boolean notConnected = true;
		while (notConnected) {
			try {
				notConnected = false;
				updateWindow("Attempting connection...");
				connect();
			} catch (Exception e) {
				notConnected = true;
				updateWindow("System in use, retrying connection...");
				Thread.sleep(3000);
			}
		}
	}

	private static void connect() throws IOException {
		System.out.println("========================");
		startConnection(SERVER_IP, SERVER_PORT);
		System.out.println("Connected");

		updateWindow("Connected, installing files.");

		//sending the number of files
		assert modList != null;
		out.writeInt(modList.length);
		out.flush();

		System.out.println(modList.length + " Mod(s) Installed: ");
		for (File f : modList) {
			//showing what mods they currently have
			String filename = f.getName();

			//sending file name
			sendString(out, filename);
			System.out.println("\t" + f.getName());
		}

		//getting number of files that are going to be sent
		int numOfFiles;
		numOfFiles = in.read();
		System.out.println("Number of files being transferred: " + numOfFiles);

		//start receiving files and writing them
		for(int i=0; i< numOfFiles; i++){
			readFile(in);
		}

		//receive number of files that need to be deleted
		int numOfDelFiles = in.readInt();

		//delete files that aren't on the server
		System.out.println("Deleting " + numOfDelFiles + " file(s): " );
		for(int i=0; i< numOfDelFiles; i++){
			String s = readString(in);
			System.out.println("\t" + s);
			File f = fileMap.get(s);
			fileMap.remove(s);
			f.delete();
		}

		// Update the window and then stop the connection
		updateWindow("Finished", "All files downloaded");
		stopConnection();
	}

	private static String readString(DataInputStream in) throws IOException {
		return in.readUTF();
	}


	private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
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