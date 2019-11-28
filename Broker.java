import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

//import java.net.InetSocketAddress;
//import java.net.SocketAddress;


public class Broker extends Node {
	static final int DEFAULT_PORT = 50001;

	static final int HEADER_LENGTH = 3;
	static final int TYPE_POS = 0;
	static final int LENGTH_POS = 1;
	static final int NUMBER_OF_WORKERS_POS = 2;
	
	static final byte TYPE_UNKNOWN = 0;
	
	static final byte WORK_DES = 1;

	//Worker Type
	static final byte WORKER_NAME = 11;
	static final byte WORKER_WORKING = 12;
	static final byte WORKER_NOT_WORKING = 13;
	static final byte FINISH_WORK = 14;
	static final byte CANCEL_WORK = 15;
	//
	
	
	
	static final byte TYPE_ACK = 2;   // Indicating an acknowledgement
	static final int ACKCODE_POS = 1; // Position of the acknowledgement type in the header
	static final byte ACK_ALLOK = 10; // Indicating that everything is okay
	static final byte TYPE_NO_WORKERS = 9;
	static final byte TYPE_NO_CLIENT = 8;
	static final int DEFAULT_SRC_PORT = 50000;
	

	Terminal terminal;
	DatagramPacket [] availableWorkers = new DatagramPacket[10];
	DatagramPacket [] unAvailableWorkers = new DatagramPacket[10];
	DatagramPacket [] clientLocation = new DatagramPacket[1];
	int clientLocationCount = 0;
	int availableWorkersCount = 0;
	/*
	 * 
	 */
	Broker(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}
	
	public synchronized void onReceipt(DatagramPacket packet) {
		try 
		{
			byte[] data;
			
			data = packet.getData();	
			switch(data[TYPE_POS]) {
			case WORK_DES:
				//this.notify();
				compileAndReturn(packet, data);
				setClientLocation(packet);
				System.out.println("Client Port: "+packet.getPort());
				sendToWorkers(packet, data);
				break;
			case WORKER_NAME:
				terminal.print("Worker's name is: ");
				compileAndReturn(packet, data);
				//this.notify();
				break;
			case WORKER_WORKING:
				compileAndReturn(packet, data);
				setWorkerLocation(packet);
				//this.wait();
				break;
			case WORKER_NOT_WORKING:
				terminal.println("The following Worker refused the work: ");
				compileAndReturn(packet, data);
				//this.wait();
				break;
			case FINISH_WORK:
				compileAndReturn(packet, data);
				terminal.println("finished work report check");
				sendToClient(packet, data);
				//this.notify();
				break;
			case CANCEL_WORK:
				sendToOtherWorkers(packet, data);
				break;
			case TYPE_ACK:
				terminal.println("Received Acknowledgement");
				//this.notify();
				break;
			default:
				terminal.println("Unexpected packet" + packet.toString());
			}

		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	public synchronized void sendToClient (DatagramPacket packet, byte[] data) throws IOException
	{
		try {		
			if(clientLocation == null)
			{
				data = new byte[HEADER_LENGTH];										// creates new header
				data[TYPE_POS] = TYPE_NO_CLIENT;									// there is no workers
				data[ACKCODE_POS] = ACK_ALLOK;										// indicating that the acknowledgement is true
				
				DatagramPacket response;
				response = new DatagramPacket(data, data.length);
				response.setSocketAddress(clientLocation[0].getSocketAddress());
				//
				System.out.println("No Clients Yet, Telling Worker: " + packet.getPort());
				//
				socket.send(response);
				//this.wait();
			}
			else
			{
				String content;
				byte[] buffer;
				
				buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
				System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
				content= new String(buffer);										// tells us whats inside string
				terminal.println("Sending the following content to client: " + content);
		
				buffer = content.getBytes();
				data = new byte[HEADER_LENGTH+buffer.length];
				data[TYPE_POS] = FINISH_WORK;
				data[LENGTH_POS] = (byte)buffer.length;
				System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
		
				terminal.println("Sending Work Acknowledgement...");
				packet= new DatagramPacket(data, data.length);
				packet.setSocketAddress(clientLocation[0].getSocketAddress());
				System.out.print(clientLocation[0].getSocketAddress());
				socket.send(packet);
				terminal.println("Work Acknowledgement sent");
				//this.wait();
			}
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
	
	public synchronized void compileAndReturn (DatagramPacket packet, byte[] data) throws IOException 
	{
		String content;
		byte[] buffer;
		
		buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
		System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
		content= new String(buffer);										// tells us whats inside string
		terminal.println(content);
		//terminal.println("Length: " + content.length());

		data = new byte[HEADER_LENGTH];										// creates new header
		data[TYPE_POS] = TYPE_ACK;											// this is an acknowledgement
		data[ACKCODE_POS] = ACK_ALLOK;										// indicating that the acknowledgement is true
		
		DatagramPacket response;
		response = new DatagramPacket(data, data.length);
		response.setSocketAddress(packet.getSocketAddress());
		System.out.println("Packet Address to Client: " + packet.getPort());
		socket.send(response);
	}
	
	public synchronized void setWorkerLocation(DatagramPacket packet)
	{
		try {
			terminal.println("Setting Worker Location......");
			availableWorkers[availableWorkersCount] = packet;
			availableWorkersCount++;
			terminal.println("Worker Location is set.");
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	public synchronized void setClientLocation(DatagramPacket packet)
	{
		try {
			terminal.println("Setting Client Location......");
			clientLocation[0] = packet;
			terminal.println("Client Location is set:" + clientLocation[0].getPort());
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	public synchronized void sendToWorkers (DatagramPacket packet, byte[] data) throws IOException, InterruptedException 
	{	
		System.out.println("Hello worker is null: test 1");
		if(availableWorkers[0] == null)
		{
			System.out.println("Hello worker is null: test 2");
			String content;
			byte[] buffer;
			
			buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
			System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
			content= new String(buffer);										// tells us whats inside string
			terminal.println(content);
			//terminal.println("Length: " + content.length());

			data = new byte[HEADER_LENGTH];										// creates new header
			data[TYPE_POS] = TYPE_NO_WORKERS;									// there is no workers
			data[ACKCODE_POS] = ACK_ALLOK;										// indicating that the acknowledgement is true
			
			DatagramPacket response;
			response = new DatagramPacket(data, data.length);
			response.setSocketAddress(packet.getSocketAddress());
			//
			System.out.println("No Workers Yet, Telling CandC: " + packet.getPort());
			//
			socket.send(response);
		}
		else
		{
			System.out.println("Hello worker is null: test 3");
			String content;
			byte[] buffer;
			
			buffer= new byte[data[LENGTH_POS]];	
			int i2 = data[Broker.NUMBER_OF_WORKERS_POS];
			if(i2 == 0)
			{
				i2 = 1;
			}
			System.out.println(data[Broker.NUMBER_OF_WORKERS_POS]);// creates header of array which tells us the content
			System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
			content= new String(buffer);										// tells us whats inside string
			terminal.println("Sending the following content to worker: " + content);
	
			buffer = content.getBytes();
			data = new byte[HEADER_LENGTH+buffer.length];
			data[TYPE_POS] = WORK_DES;
			data[LENGTH_POS] = (byte)buffer.length;
			System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);

			for(int i = 0; i < i2; i++)
			{
				if(availableWorkers[i] != null)
				{
					terminal.println("Sending packet to work in (sendToWorkers)...");
					packet= new DatagramPacket(data, data.length);
					packet.setSocketAddress(availableWorkers[i].getSocketAddress());
					socket.send(packet);
					terminal.println("Packet sent in (sendToWorkers)");
				}
			}
			//this.wait();
		}
	}
	
	public synchronized void sendToOtherWorkers (DatagramPacket packet, byte[] data) throws IOException, InterruptedException 
	{	

		for(int i = 0; availableWorkers[i] != null; i++)
		{
			if(!(packet.getSocketAddress().equals(availableWorkers[i].getSocketAddress())))
			{
				String content;
				byte[] buffer;
				
				buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
				System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
				content= new String(buffer);										// tells us whats inside string
				terminal.println("Sending the following content to other workers: " + content);				
				
				///////////////		
				
				buffer = content.getBytes();
				data = new byte[HEADER_LENGTH+buffer.length];
				data[TYPE_POS] = WORK_DES;	
				data[LENGTH_POS] = (byte)buffer.length;

				System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
				
				//////////////////
		
				terminal.println("Sending packet to work in (sendToOtherWorkers)...");
				packet= new DatagramPacket(data, data.length);
				packet.setSocketAddress(availableWorkers[i].getSocketAddress());
				socket.send(packet);
				terminal.println("Packet sent in (sendToOtherWorkers)");
			}
		}
		//this.wait();
	}

	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact");
		this.wait();
	}

	public static void main(String[] args) {
		try {		
			Terminal terminal= new Terminal("Broker");
			Broker broker = new Broker(terminal, DEFAULT_PORT);
			//broker.printCommands();
			broker.start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}