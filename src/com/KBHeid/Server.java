package KBHeid;

import javax.swing.JFileChooser;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Server {
	private static final int 	PORT = 25566;

	//Helpers
	private static String readString(DataInputStream in) throws IOException {
		return in.readUTF();
	}

	private static void sendString(DataOutputStream out, String str) throws IOException {
		out.writeUTF(str);
	}

	private static void sendFile(DataOutputStream out, File file, String path) throws  IOException {
		sendString(out, path + "/" + file.getName());
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

	private static ServerSocket serverSocket;
	private static File folder;

	//TODO: Set up functionality to run in background(continuously)
	//TODO: Set up functionality for syncing things as directories
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		folder = getDirToSync();

		serverSocket = new ServerSocket(PORT);

		//Get the first connection
		connect();
	}

	private static void connect() throws IOException {
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

		HashMap<String, String> clientCheckSumsToPaths = new HashMap<>();
		HashMap<String, String> serverCheckSumsToPaths = new HashMap<>();

		DataOutputStream out;

		@Override
		public void run() {
			System.out.println("\n========  Connection from " + clientSocket.getRemoteSocketAddress() + "  ========\n");

			makeChecksumToPath(folder, "");

			DataInputStream in;
			try {
				in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
				out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

				//get all the files sent from the client
				while (in.readUTF().equals("FILE")){
					String path = in.readUTF();
					String checksum = in.readUTF();
					clientCheckSumsToPaths.put(checksum, path);
				}

				//check their checksum and filepath to the server's
				for (String s : clientCheckSumsToPaths.keySet()) {
					if (Objects.equals(clientCheckSumsToPaths.get(s), serverCheckSumsToPaths.get(s)) && serverCheckSumsToPaths.containsKey(s)) {
						clientCheckSumsToPaths.remove(s);
						serverCheckSumsToPaths.remove(s);
					}
				}

				sendFilesInDir(folder, ".");

				out.writeUTF("END");

				//Send number of files to delete
				out.writeInt(clientCheckSumsToPaths.size());


				//Send all files to delete
				for (String fn : clientCheckSumsToPaths.keySet()) {
					out.writeUTF(clientCheckSumsToPaths.get(fn));
				}

				out.close();
				in.close();
				clientSocket.close();
				System.out.println("\n======== Connection finished ========\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void makeChecksumToPath(File dir, String dirPath){
			ArrayList<File> subDirs = new ArrayList<>();
			for (File f : dir.listFiles()) {
				if(f.isDirectory()){
					subDirs.add(f);
				}
				else{
					try {
						String fChecksum = getFileChecksum(f);
						serverCheckSumsToPaths.put(fChecksum, dirPath + "/" + f.getName());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			for(File f : subDirs){
				makeChecksumToPath(f, dirPath + "/" + f.getName());
			}
		}

		private void sendFilesInDir(File dir, String dirPath) {
			ArrayList<File> subDirs = new ArrayList<>();

			for (File f : dir.listFiles()) {
				if(f.isDirectory()){
					subDirs.add(f);
				}
				else{
					try {
						String fChecksum = getFileChecksum(f);
						if(clientCheckSumsToPaths.containsKey(fChecksum)){
							clientCheckSumsToPaths.remove(fChecksum);
						}
						else{
							out.writeUTF("FILE");
							sendFile(out, f, dirPath);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			for(File f : subDirs){
				sendFilesInDir(f, dirPath + "/" + f.getName());
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
