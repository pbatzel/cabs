package net;

import java.io.InputStream;
import java.util.PriorityQueue;

import engine.LocalEngine;

public class MessageReader implements Runnable {

	private PriorityQueue<Message> recvdMessages;
	private InputStream in;
	private LocalEngine engine;

	public MessageReader(LocalEngine engine, InputStream in) {
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
					Message message = new Message(engine.turn, (byte) messageType);
					message.recvAgent(in);
					synchronized (recvdMessages) {
						if (!recvdMessages.remove(message)) {
							recvdMessages.add(message);
						} else {
							System.err.println("Message and antimessage annihilated");
						}
					}
					break;
				case Message.ENDTURN:
					Message.recvEndTurn(in);
					break;
				default:
					System.out.println("Unknown Message type " + messageType);
				}
				if (messageType == -1) {
					break;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
