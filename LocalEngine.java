import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.nio.channels.SocketChannel;

public class LocalEngine extends Engine {

	LocalCell[][] cells;
	ArrayList<RemoteEngine> peerList;
	int globalWidth;
	int globalHeight;

	public LocalEngine(int tlx, int tly, int width, int height, int globalWidth, int globalHeight) {
		super(tlx, tly, width, height);
		this.globalWidth = globalWidth;
		this.globalHeight = globalHeight;
		peerList = new ArrayList<RemoteEngine>();
		cells = new LocalCell[height][width];
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				cells[i][j] = new LocalCell(j, i, this);
			}
		}
	}

	public void go(int turn) {
		for (LocalCell[] cell : cells) {
			for (LocalCell element : cell) {
				element.go(turn);
			}
		}
	}

	public void moveAgent(Agent agent, LocalCell oldCell, int x, int y) {
		Cell newCell = findCell(oldCell.x + x, oldCell.y + y);
		newCell.add(agent);
		oldCell.remove(agent);
	}

	private Cell findRemoteCell(int x, int y) {
		for (int i = 0; i < peerList.size(); i++) {
			if (peerList.get(i).hasCell(x, y))
				return peerList.get(i).findCell(x, y);
		}
		System.err.println("Didn't find remote cell: " + x + ", " + y);
		return null;
	}

	@Override
	public Cell findCell(int x, int y) {
		if (y >= globalHeight) {
			y = y % globalHeight;
		}
		if (x >= globalWidth) {
			x = x % globalWidth;
		}
		if (y < 0) {
			y = (y % globalHeight) + globalHeight;
		}
		if (x < 0) {
			x = (x % globalWidth) + globalWidth;
		}
		if (hasCell(x, y)) {
			System.err.println("Has cell " + x + ", " + y);
			return cells[y][x];
		} else {
			System.err.println("Checking remote for cell " + x + ", " + y);
			return findRemoteCell(x, y);
		}
	}

	public void placeAgent(int x, int y, Agent agent) {
		cells[y - tly][x - tlx].add(agent);
	}

	public void placeAgents(int agents) {
		for (int i = 0; i < agents; i++) {
			cells[i][0].add(new Rabbit());
		}
	}

	public void print() {
		for (LocalCell[] cell : cells) {
			for (LocalCell element : cell) {
				if (element.agents.size() > 0) {
					System.out.print("* ");
				} else {
					System.out.print("- ");
				}
			}
			System.out.println();
		}
	}

	private void handleMessages(){
		for(int i=0; i< peerList.size(); i++){
			int messageType = 0;
			try{
				ObjectInputStream in = (ObjectInputStream)peerList.get(i).in; 
				while(messageType != -1){
					messageType = in.read();
					switch(messageType){
					case Protocol.SENDAGENT:
						ReceivedAgent newAgent = Protocol.sendAgent(in);
						this.placeAgent(newAgent.x, newAgent.y, newAgent.agent);
						System.err.println("Receieved agent: " + newAgent);
						break;
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
				
	public static void main(String[] args) {

		int globalWidth = 10;
		int globalHeight = 10;
		int port = 1234;
		LocalEngine engine = null;
		try {

			// Client case
			if (args.length == 1) {
				byte[] x = new byte[25];
				InetAddress other = InetAddress.getByName(args[0]);
				Socket socket = new Socket(other, port);
//				SocketChannel channel = SocketChannel.open(socket.getRemoteSocketAddress());
//				channel.configureBlocking(false);
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				Protocol.offerHelpReq(out);
				OfferHelpResponse r = Protocol.offerHelpResp(in);
				engine = new LocalEngine(r.tlx, r.tly, r.width, r.height, r.globalWidth,
						r.globalHeight);
				engine.peerList.add(new RemoteEngine(socket, in, out, engine, 0,0,5,5));
				for(int i = 0; i< 8; i++){
					// Wait for agents.
					engine.go(i);
					engine.handleMessages();
					engine.print();
					// break;
				}
			}

			// Server case
			else {
				// TODO: Don't hard code everything.
				engine = new LocalEngine(0, 0, 5, 5, globalWidth, globalHeight);
				byte[] r = new byte[1];
				ServerSocket serverSocket = new ServerSocket(port);
				Socket clientSocket = serverSocket.accept();
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				in.read(r);
				System.out.println(r[0]);
				// TODO: Use a smart algorithm to figure out what
				// coordinates to assign the other node.
				Protocol.offerHelpResp(out, 5, 0, 5, 5, globalWidth, globalHeight);
				// We probably need some kind of ACK here.
				RemoteEngine remote = new RemoteEngine(clientSocket, in, out, engine, 5, 0, 5, 5);
				engine.peerList.add(remote);
				engine.placeAgents(5);

			}
			engine.print();
			for (int i = 0; i < 8; i++) {
				Thread.sleep(1000);
				engine.go(i);
				engine.print();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
