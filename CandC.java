import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 *
 * Client class
 *
 * An instance accepts input from the user, marshalls this into a datagram, sends
 * it to a server instance and then waits for a reply. When a packet has been
 * received, the type of the packet is checked and if it is an acknowledgement,
 * a message is being printed and the waiting main method is being notified.
 *
 */
public class CandC extends Node {
	static final int DEFAULT_SRC_PORT = 50000; // Port of the client
	static final int DEFAULT_DST_PORT = 50001; // Port of the server
	static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the server
//DEFAULT_DST_NODE, DEFAULT_DST_PORT,
//Port of server, name of server
	static final int HEADER_LENGTH = 3; // Fixed length of the header
	static final int TYPE_POS = 0; // Position of the type within the header


	static final byte TYPE_UNKNOWN = 0;
	static final byte FINISH_WORK = 14;

	static final byte WORK_DES = 1; // Indicating a string payload
	static final int LENGTH_POS = 1;

	static final byte TYPE_ACK = 2;   // Indicating an acknowledgement
	static final int ACKCODE_POS = 1; // Position of the acknowledgement type in the header
	static final byte ACK_ALLOK = 10; // Indicating that everything is okay

	static Terminal terminal;
	InetSocketAddress dstAddress;
	int clientVersion = 0;
	String continueWorking = "Maybe?";

	CandC(Terminal Terminal, String dstHost, int dstPort, int srcPort) {
		try {
			terminal= new Terminal ("C&C");
			//(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT);
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	public synchronized void onReceipt(DatagramPacket packet)  
	{
		byte[] data;

		data = packet.getData();
		switch(data[TYPE_POS]) {
		case TYPE_ACK:
			terminal.println("Received Acknowledgement");
			//try {
				this.notify();
			//} catch (InterruptedException e1) {e1.printStackTrace();}
			break;
		case Broker.TYPE_NO_WORKERS:
			terminal.println("There are no workers avaliable, please wait.");
			//this.notify();
			break;
		case FINISH_WORK:
			//this.notify();
			System.out.println("");
			System.out.println("FINISH WORK HAS GOT TO CLIENT");
			try 
			{
				readWorkDes(packet, data);
			} catch (IOException e) {e.printStackTrace();}
			break;
		default:
			terminal.println("Unexpected packet" + packet.toString());
		}
	}

	public synchronized void sendMessage() throws Exception 
	{
		byte[] data= null;
		byte[] buffer= null;
		DatagramPacket packet= null;
		String input;

			input= terminal.read("C: ");
			
			buffer = input.getBytes();
			data = new byte[HEADER_LENGTH+buffer.length];
			data[TYPE_POS] = WORK_DES;	
			data[LENGTH_POS] = (byte)buffer.length;
			char[] inputCharArray = input.toCharArray();
			if(Character.isDigit(inputCharArray[1]))
			{
				byte byteNumberWorkers = (byte) Character.getNumericValue(inputCharArray[1]);
				System.out.println(byteNumberWorkers); // -22
				data[Broker.NUMBER_OF_WORKERS_POS] = byteNumberWorkers;
				System.out.println("Number of Workers: " + data[Broker.NUMBER_OF_WORKERS_POS]);
			}
			System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

			terminal.println("Sending packet...");
			packet= new DatagramPacket(data, data.length);
			packet.setSocketAddress(dstAddress);
			socket.send(packet);
			terminal.println("Packet sent");
			this.wait();
	}

	public synchronized void start() throws Exception {
		try {
			
			while(!(this.continueWorking.equalsIgnoreCase("y") || this.continueWorking.equalsIgnoreCase("n")))
			{
				this.continueWorking = terminal.read("Would you like to begin/continue work? (y/n)");
				if(this.continueWorking.equalsIgnoreCase("y"))
				{
					terminal.println("Enter command or enter Work Description.");
					
					sendMessage();
					this.wait();
					continueWorking = "Maybe?";
				}
				else if (!(this.continueWorking.equalsIgnoreCase("y") || this.continueWorking.equalsIgnoreCase("n")))
				{
					terminal.println("Incorrect input, enter either 'y' or 'n'.");
				}
			}
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
	
	public synchronized void readWorkDes (DatagramPacket packet, byte[] data) throws IOException 
	{
		String content;
		byte[] buffer;
		
		buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
		System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
		content= new String(buffer);										// tells us what's inside string
		terminal.println("Work Description: " + content);

		data = new byte[HEADER_LENGTH];										// creates new header
		data[TYPE_POS] = TYPE_ACK;											// this is an acknowledgement
		data[ACKCODE_POS] = ACK_ALLOK;										// indicating that the acknowledgement is true
		
		DatagramPacket response;
		response = new DatagramPacket(data, data.length);
		response.setSocketAddress(packet.getSocketAddress());
		//
		System.out.println("Packet Return to Broker: " + packet.getPort());
		//
		socket.send(response);
		this.notify();
	}
	
	public static void main(String[] args) {
		try {
			CandC commandAndControl = new CandC(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, DEFAULT_SRC_PORT);
			commandAndControl.start();
			System.out.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}