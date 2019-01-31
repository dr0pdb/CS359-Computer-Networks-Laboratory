import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;

public class HTTPServer {
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final int PORT = 1234;

	private Socket connect;
	
	public HTTPServer(Socket c) {
		connect = c;
	}

	/*
		Client connection manager.
	*/
	void manageClient() {
		// Inpu and Output streams.
		BufferedReader in = null;
		PrintWriter out = null; 
		BufferedOutputStream dataOut = null;
		String fileRequested = "";

		try {
			// Initialize streams.
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());

			// get the request method and file requested.
			String input = in.readLine();
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase();
			fileRequested = parse.nextToken().toLowerCase();
			
			// Show index.html file
			if (fileRequested.endsWith("/")) {
				fileRequested += DEFAULT_FILE;
			}
			
			File file = new File(WEB_ROOT, fileRequested);
			int fileLength = (int) file.length();
			String content = getContentType(fileRequested);
			
			if (method.equals("GET")) { // GET method so we return content
				byte[] fileData = readFileData(file, fileLength);
				
				// send HTTP Headers
				appendHeaders(out, content, fileLength);
				
				// write file data to output stream.
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			}
			
			// Logging.
			System.out.println("File " + fileRequested + " of type " + content + " returned");
				
		} catch (FileNotFoundException fileNotFoundException) {
			try {
				// returning 404 page.
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
		} catch(IOException ioException) {
			System.err.println("Server IOException: " + ioException.getMessage());
		} finally {
			try {
				// close the streams and the connection.
				in.close();
				out.close();
				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}
		}
	}

	/*
		Appends the headers to the response.
	*/
	private void appendHeaders(PrintWriter out, String content, int fileLength) {
		out.println("HTTP/1.1 200 OK");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush();
	}

	/*
		Reads the contents of a file and returns as a byte array.
		Throws IOException.
	*/
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	
	// return supported MIME Types
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "text/plain";
	}

	/*
		Handle file not found exception. Sends a 404 page back.
	*/
	private void fileNotFound(PrintWriter out, BufferedOutputStream dataOut, String fileRequested) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		
		// append headers
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); // blank line between headers and content, very important !
		out.flush();
		
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
	}

	// Driver program
	public static void main(String[] args) {
		try {
			ServerSocket serverSocket = new ServerSocket(PORT);
			System.out.println("Server started, Listening on port: " + PORT);

			// Wait for client to establish connection.
			while(true) {
				HTTPServer myServer = new HTTPServer(serverSocket.accept());				
				System.out.println("Connecton opened. (" + new Date() + ")");

				myServer.manageClient();
			}
		} catch (IOException ioException) {
			System.err.println("Server Error: " + ioException.getMessage());
		}
	}
}