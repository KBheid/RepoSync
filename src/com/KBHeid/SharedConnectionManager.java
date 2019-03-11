package com.KBHeid;


import java.io.*;
import java.net.Socket;

class SharedConnectionManager {
	private static Socket conn;
	private static DataOutputStream out;
	private static DataInputStream in;

	static void startConnection(String ip, int port) throws IOException {
		conn = new Socket(ip, port);
		in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
		out = new DataOutputStream(new BufferedOutputStream(conn.getOutputStream()));
	}

	static void stopConnection() throws IOException {
		in.close();
		out.close();
		conn.close();
		System.out.println("Connection Finished");
		System.out.println("========================");
	}

	static void sendString(String str) throws IOException{
		out.writeUTF(str);
		out.flush();
	}

	static String readString() throws IOException {
		return in.readUTF();
	}

	static void sendInt(int i) throws IOException {
		out.writeInt(i);
		out.flush();
	}

	static File readFile(String path) throws IOException {
		//getting the name and length of the file being sent
		String fileName = readString();
		//updateWindow("Downloading file: \"" + fileName + "\"");
		System.out.println("\t" + fileName);
		int lenOfFile = in.readInt();
		byte[] fileBytes = new byte[8192];

		//setting up a file writer
		FileOutputStream fileWriter = new FileOutputStream(path + "/" + fileName);

		//getting file from server
		int count;
		while (lenOfFile > 0 && (count = in.read(fileBytes, 0, Math.min(fileBytes.length, lenOfFile))) != -1) {
			fileWriter.write(fileBytes,0,count);
			lenOfFile -= count;
		}

		return new File(path + "/" + fileName);
	}

	static void sendFile(File f) throws IOException {
		String filename = f.getName();

		// Sending file name
		SharedConnectionManager.sendString(filename);
		System.out.println("\t" + f.getName());
	}

	static int readInt() throws IOException {
		return in.readInt();
	}
}
