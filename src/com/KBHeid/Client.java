package com.KBHeid;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Client {
	private static final String ENVIRONMENT = System.getenv("APPDATA");

	//Someday to be config options?
	private static final String FOLDER = "/.minecraft/mods";
	private static final String SERVER_IP = "199.247.68.80";
	private static final int	SERVER_PORT = 25566;
	private static final String[] IGNORED_PREFIXES = {
			"__",
			"*",
			"ignored_"
	};

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
			if (!isIgnoredFile(f)) {
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
		SharedConnectionManager.startConnection(SERVER_IP, SERVER_PORT);
		System.out.println("Connected");

		updateWindow("Connected, installing files.");

		//sending the number of files
		assert modList != null;
		SharedConnectionManager.sendInt(modList.length);

		// Send all checked files
		System.out.println(modList.length + " Mod(s) Installed: ");
		for (File f : modList) {
			SharedConnectionManager.sendFile(f);
		}

		// Getting number of files that are going to be received
		int numOfFiles = SharedConnectionManager.readInt();
		System.out.println("Number of files being transferred: " + numOfFiles);

		// Start receiving files and writing them
		updateWindow("Downloading", "Preparing to download files");
		for(int i=0; i< numOfFiles; i++){
			SharedConnectionManager.readFile(ENVIRONMENT + FOLDER);
		}

		// Delete files that aren't on the server
		// and generate a message
		String deleteMessage = deleteFiles();

		updateWindow("Finished", deleteMessage);
		SharedConnectionManager.stopConnection();
	}

	private static String deleteFiles() throws IOException {
		//receive number of files that need to be deleted
		int numOfDelFiles = SharedConnectionManager.readInt();

		ArrayList<String> deletedFiles = new ArrayList<>();
		System.out.println("Deleting " + numOfDelFiles + " file(s): " );
		for(int i=0; i< numOfDelFiles; i++){
			String filename = SharedConnectionManager.readString();
			System.out.println("\t" + filename);
			File f = fileMap.get(filename);
			fileMap.remove(filename);
			f.delete();
			deletedFiles.add(filename);
		}

		// Update the window and then stop the connection
		StringBuilder returnBuilder = new StringBuilder("All files downloaded. The following files have been removed:\n");
		for (String s : deletedFiles)
			returnBuilder.append("\t").append(s).append("\n");

		return returnBuilder.toString();
	}

	//Check if a file is ignored in our IGNORED_PREFIXES list.
	private static boolean isIgnoredFile(File f) {
		for (String s : IGNORED_PREFIXES)
			if (!f.getName().startsWith(s))
				return true;
		return false;
	}


	/*private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
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
	}*/
}