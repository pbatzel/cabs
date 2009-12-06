import java.io.InputStream;
import java.util.ArrayList;
import java.util.PriorityQueue;


public class MessageReader implements Runnable{
	
	private PriorityQueue<Message> recvdMessages;
	private InputStream in;
	private LocalEngine engine;

	public MessageReader(LocalEngine engine, InputStream in){
		
		this.engine = engine;
		this.recvdMessages = engine.recvdMessages;
		this.in = in;
	}
	
	public void run() {
		while (true) {
			try {
				int messageType = in.read();
				switch (messageType) {
				case Message.SENDAGENT:
					Message message = new Message(engine.turn, messageType);
					message.recvAgent(in);
					synchronized (recvdMessages) {
						recvdMessages.add(message);
					}
					break;
				case Message.ENDTURN:
					Message.endTurn(in);
					break;
				default:
					System.out.println("Unknown Message type " + messageType);
				}
				if(messageType == -1){
					break;
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

