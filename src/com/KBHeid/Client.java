package KBHeid;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class Client {
	private static String SERVER_IP;
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
		int lenOfFile = in.readInt();
		byte[] fileBytes = new byte[8192];

		File file = new File(folder + "/" + fileName);
		file.getParentFile().mkdirs();
		//setting up a file writer
		FileOutputStream fileWriter = new FileOutputStream(file);

		//getting file from server
		int count;
		while (lenOfFile > 0 && (count = in.read(fileBytes, 0, Math.min(fileBytes.length, lenOfFile))) != -1) {
			fileWriter.write(fileBytes,0,count);
			lenOfFile -= count;
		}
	}

	private static File getDirToSync(){
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(".."));
		chooser.setDialogTitle("Choose a Directory to sync");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		return null;
	}

	private static String getIPCon(){
		JFrame frame = new JFrame("IP Input");
		String IP = JOptionPane.showInputDialog(frame, "Enter Server IP");
		frame.dispose();
		return IP;
	}

	/* =============================================
	 * =======         Main methods         ========
	 * =============================================
	 */
	private static HashMap<String, File> checksumMap = new HashMap<>();
	private static File folder;

	private static JFrame pane;

	//TODO: Set up functionality to run in background(continuously)
	//TODO: Set up functionality for syncing things as directories
	public static void main(String[] args) throws IOException, InterruptedException {
		//get what directory user wants synced
		folder = getDirToSync();

		//prompt user for serverIP
		SERVER_IP = getIPCon();
		if(SERVER_IP.equals("")){
			System.out.println("No IP Entered");
			return;
		}

		createWindow();

		updateWindow("Connecting...", "Attempting connection...");

		connect();

	}

	private static void sendFilesInDir(File dir, String dirPath) {
		ArrayList<File> subDirs = new ArrayList<>();

		for (File f : dir.listFiles()) {
			if(f.isDirectory()){
				subDirs.add(f);
			}
			else{
				try {
					String fChecksum = getFileChecksum(f);
					checksumMap.put(fChecksum, f);
					out.writeUTF("FILE");
					out.writeUTF(dirPath);
					out.writeUTF(fChecksum);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		for(File f : subDirs){
			sendFilesInDir(f, dirPath + "/" + f.getName());
		}
	}

	private static void connect() throws IOException {
		System.out.println("========================");
		startConnection(SERVER_IP, SERVER_PORT);
		System.out.println("Connected");

		updateWindow("Connected, installing files.");

		sendFilesInDir(folder, "");

		out.writeUTF("END");
		out.flush();

		//start receiving files and writing them
		while (in.readUTF().equals("FILE")){
			readFile(in);
		}

		//receive number of files that need to be deleted
		int numOfDelFiles = in.readInt();

		//delete files that aren't on the server
		for(int i=0; i< numOfDelFiles; i++){
			String s = readString(in);
			File f = checksumMap.get(s);
			checksumMap.remove(s);
			f.delete();
		}

		// Update the window and then stop the connection
		updateWindow("Finished", "All files downloaded");
		stopConnection();
	}

	private static String readString(DataInputStream in) throws IOException {
		return in.readUTF();
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
		int bytesCount;

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