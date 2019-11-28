import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class WorkerTwo extends Node {
	
	static final int DEFAULT_DST_PORT = 50001; // Port of the server
	static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the server

	static final int HEADER_LENGTH = 3; // Fixed length of the header
	static final int TYPE_POS = 0; // Position of the type within the header

	static final byte TYPE_UNKNOWN = 0;

	static final byte TYPE_STRING = 1; // Indicating a string payload
	static final int LENGTH_POS = 1;

	static final byte TYPE_ACK = 2;   // Indicating an acknowledgement
	static final int ACKCODE_POS = 1; // Position of the acknowledgement type in the header
	static final byte ACK_ALLOK = 10; // Indicating that everything is okay
	
	public static Terminal terminal;
	String name = "Error: No name assigned";
	String availability = "Error: Not assigned availability";
	
	public static int workerPort = 50004; // Port of the worker
	static int workerVersion = 2;
	int thisWorkerVersion = workerVersion;
	InetSocketAddress dstAddress;
	InetSocketAddress workerSocketAddress;
	public DatagramPacket wrkPacket;
	String continueWorking = "Maybe?";
	String finishedWork = "Maybe?";
	
	public WorkerTwo(Terminal Terminal, String dstHost, int dstPort, int srcPort) 
	{
		try {
			name = "unnamed";
			terminal = new Terminal ("Worker: " + workerVersion++);
			thisWorkerVersion = workerVersion;
			dstAddress = new InetSocketAddress(dstHost, dstPort);
			workerSocketAddress = null;
			socket = new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}
	
	@Override
	public synchronized void onReceipt(DatagramPacket packet) 
	{
		try {
			byte[] data;

			data = packet.getData();
			switch(data[TYPE_POS]) 
			{
			case Broker.WORK_DES:
				terminal.println("Sucessfully sent: Client -> Worker");
				compileAndReturn(packet, data);
				acceptThisTask(packet, data);
				//this.notify();
				break;
			case TYPE_ACK:
				terminal.println("Received Acknowledgement");
				//this.notify();
				break;
			case Broker.TYPE_NO_CLIENT:
				terminal.println("Currently No Client, please wait");
				break;
			default:
				terminal.println("Unexpected packet" + packet.toString());
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	public synchronized void sendMessage(String input, byte messageType) throws Exception 
	{
		byte[] data= null;
		byte[] buffer= null;
		DatagramPacket packet= null;

			buffer = input.getBytes();
			data = new byte[HEADER_LENGTH+buffer.length];
			data[TYPE_POS] = messageType;
			if(data[TYPE_POS] == Broker.WORKER_NAME)
			{
				terminal.println("Sending worker name...");
				terminal.println("Worker name sent.");
			}
			else if((data[TYPE_POS] == Broker.WORKER_WORKING) || (data[TYPE_POS] == Broker.WORKER_NOT_WORKING))
			{
				terminal.println("Sending worker availability");
				terminal.println("Worker availability sent");
			}
			else
			{
				terminal.println("Sending packet...");
				terminal.println("Packet sent");
			}
			data[LENGTH_POS] = (byte)buffer.length;

			System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
			System.out.println(new String(buffer));
			packet= new DatagramPacket(data, data.length);	
			packet.setSocketAddress(dstAddress);
			socket.send(packet);
	}
	
	public void start(WorkerTwo worker) throws Exception 
	{
		try {
			sendMessage(name, Broker.WORKER_NAME);
			while(!(this.availability.equalsIgnoreCase("y") || this.availability.equalsIgnoreCase("n")))
			{
				this.availability = terminal.read("Would you like to work?(y/n)");
				if(!(this.availability.equalsIgnoreCase("y") || this.availability.equalsIgnoreCase("n")))
				{
					terminal.println("Incorrect input, enter either 'y' or 'n'.");
				}
			}
			//while()
			if(this.availability.equalsIgnoreCase("y"))
			{
				sendMessage(name + " is available to work.", Broker.WORKER_WORKING);
				
			}
			else if(this.availability.equalsIgnoreCase("n"))
			{
				sendMessage(name + " is not available to work.", Broker.WORKER_NOT_WORKING);	
			}
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
	
	public synchronized void acceptThisTask(DatagramPacket packet, byte[] data) throws Exception 
	{
		try {
			this.continueWorking = "Maybe?";
			while(!(this.continueWorking.equalsIgnoreCase("y") || this.continueWorking.equalsIgnoreCase("n")))
			{
				this.continueWorking = terminal.read("Would you like to do/continue this work? (y/n)");
				if(this.continueWorking.equalsIgnoreCase("y"))
				{
					finishedWork = terminal.read("Report work: ");
					terminal.println("Sending finished work report....)");
					sendMessage(finishedWork, Broker.FINISH_WORK);
				}
				else if(this.continueWorking.equalsIgnoreCase("n"))
				{
					terminal.println("You have denied work!");
					//sendMessage("", Broker.CANCEL_WORK);
					
					String content;
					byte[] buffer;
					
					buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
					System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
					content= new String(buffer);										// tells us whats inside string
					terminal.println(content);
					//terminal.println("Length: " + content.length());

					data = new byte[HEADER_LENGTH+buffer.length];										// creates new header
					data[TYPE_POS] = Broker.CANCEL_WORK;								// there is no workers
					data[ACKCODE_POS] = ACK_ALLOK;										// indicating that the acknowledgement is true
					System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
					
					DatagramPacket response;
					response = new DatagramPacket(data, data.length);
					response.setSocketAddress(packet.getSocketAddress());
					//
					System.out.println("Send To Other Workers (Worker)" + packet.getPort());
					//
					socket.send(response);
				}
				else if (!(this.continueWorking.equalsIgnoreCase("y") || this.continueWorking.equalsIgnoreCase("n")))
				{
					terminal.println("Incorrect input, enter either 'y' or 'n'.");
				}
				//else no send this work to next worker
				//else if no other worker than do nothing
			}
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}

	public synchronized void compileAndReturn (DatagramPacket packet, byte[] data) throws IOException 
	{
		String content;
		byte[] buffer;
		
		buffer= new byte[data[LENGTH_POS]];									// creates header of array which tells us the content
		System.arraycopy(data, HEADER_LENGTH, buffer, 0, buffer.length);	// adds header + content
		content= new String(buffer);										// tells us what's inside string
//		for(int i = 0; i< availableWorkers.size(); i++)
//		{
			terminal.println("|" + content + "|");
//		}

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
	}
	
	public static void main (String args[])
	{
		WorkerTwo workerOne = new WorkerTwo(terminal, DEFAULT_DST_NODE, DEFAULT_DST_PORT, workerPort);
		try {
			workerOne.name = terminal.read("Enter a new name: ");
			terminal.changeName(terminal, workerOne.name);
			//InetSocketAddress wrkAddress = new InetSocketAddress(dstHost, dstPort);
			workerOne.start(workerOne);
		} catch (Exception e) {e.printStackTrace();}
	}
}
