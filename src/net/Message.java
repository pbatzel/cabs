package net;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

import world.Agent;

public class Message implements Cloneable {

	public static class OfferHelpResponse {

		private int tlx;
		private int tly;
		private int width;
		private int height;
		private int globalWidth;
		private int globalHeight;
		public int sendertlx;
		public int sendertly;
		public int senderw;
		public int senderh;

		public void setTlx(int tlx) {
			this.tlx = tlx;
		}

		public int getTlx() {
			return tlx;
		}

		public void setTly(int tly) {
			this.tly = tly;
		}

		public int getTly() {
			return tly;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getWidth() {
			return width;
		}

		public void setHeight(int height) {
			this.height = height;
		}

		public int getHeight() {
			return height;
		}

		public void setGlobalWidth(int globalWidth) {
			this.globalWidth = globalWidth;
		}

		public int getGlobalWidth() {
			return globalWidth;
		}

		public void setGlobalHeight(int globalHeight) {
			this.globalHeight = globalHeight;
		}

		public int getGlobalHeight() {
			return globalHeight;
		}
	}

	public static class ReceivedAgent {
		public int x;
		public int y;
		public Agent agent;
	}

	public static final byte OFFERHELP = 0x1;
	public static final byte SENDAGENT = 0x2;
	public static final byte ENDTURN = 0x3;

	public static Comparator<Message> sendTurnComparator = new Comparator<Message>() {

		public int compare(Message o1, Message o2) {
			if (o1.sendTurn > o2.sendTurn)
				return 1;
			else if (o1.sendTurn < o2.sendTurn)
				return -1;
			else
				return 0;

		}

	};

	// This compares things in the opposite order so that messages in the
	// priority queue will be sorted correctly.
	public static Comparator<Message> reverseSendTurnComparator = new Comparator<Message>() {

		public int compare(Message o1, Message o2) {
			if (o1.sendTurn > o2.sendTurn)
				return -1;
			else if (o1.sendTurn < o2.sendTurn)
				return 1;
			else
				return 0;

		}

	};

	public void print() {
		System.out.println("sendTurn: " + sendTurn + " messageType: "
				+ messageType + " sign: " + sign + " data: " + data);
		try {
			System.out.write(data);
			System.out.println();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean equals(Object other) {
		boolean result = false;
		if (other instanceof Message) {
			Message otherMsg = (Message) other;
			result = ((this.sendTurn == otherMsg.sendTurn)
					&& (this.messageType == otherMsg.messageType)
					&& (this.sign != otherMsg.sign) && Arrays.equals(this.data,
					otherMsg.data));
		}
		return result;
	}

	public int sendTurn;
	public boolean sign;
	private int recvTurn;
	public byte messageType;
	private byte[] data;
	public String id;

	public Message(int sendTurn, boolean sign, String id) {
		this.sendTurn = sendTurn;
		this.sign = sign;
		this.id = id;
	}

	public Message(int recvTurn, byte messageType) {
		this.recvTurn = recvTurn;
		this.messageType = messageType;
	}

	private void writeMessage(DataOutputStream dos, byte messageType,
			int dataSize) {
		try {
			this.messageType = messageType;
			dos.writeByte(messageType);
			dos.writeInt(sendTurn);
			dos.writeBoolean(sign);
			dos.writeInt(dataSize);
			dos.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int readMessage(InputStream is) {
		int dataSize = 0;
		try {
			DataInputStream dis = new DataInputStream(is);
			sendTurn = dis.readInt();
			sign = dis.readBoolean();
			dataSize = dis.readInt();
			System.out.println("Read Message: sendTurn =" + sendTurn + " sign " + sign
					+ " dataSize " + dataSize);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
		return dataSize;
	}

	public static void sendOfferHelpReq(OutputStream out) {
		try {
			synchronized (out) {
				out.write(OFFERHELP);
				out.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void sendOfferHelpResp(OutputStream out, int tlx, int tly,
			int width, int height, int globalWidth, int globalHeight,
			int sendertlx, int sendertly, int senderw, int senderh) {
		try {
			synchronized (out) {
				DataOutputStream dos = new DataOutputStream(out);
				dos.write(OFFERHELP);
				dos.writeInt(tlx);
				dos.writeInt(tly);
				dos.writeInt(width);
				dos.writeInt(height);
				dos.writeInt(globalWidth);
				dos.writeInt(globalHeight);
				dos.writeInt(sendertlx);
				dos.writeInt(sendertly);
				dos.writeInt(senderw);
				dos.writeInt(senderh);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static OfferHelpResponse recvOfferHelpResp(InputStream in) {
		OfferHelpResponse r = new OfferHelpResponse();
		try {
			// TODO verify message type
			in.read();
			DataInputStream dis = new DataInputStream(in);
			r.tlx = (dis.readInt());
			r.tly = (dis.readInt());
			r.width = (dis.readInt());
			r.height = (dis.readInt());
			r.globalWidth = (dis.readInt());
			r.globalHeight = (dis.readInt());
			r.sendertlx = dis.readInt();
			r.sendertly = dis.readInt();
			r.senderw = dis.readInt();
			r.senderh = dis.readInt();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}

	public void sendMessage(OutputStream out) {
		synchronized (out) {
			DataOutputStream dos = new DataOutputStream(out);
			writeMessage(dos, (byte) this.messageType, data.length);
			System.out.println("Sending: ");
			this.print();
			try {
				dos.write(data, 0, data.length);
				dos.flush();
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void ackMessage(OutputStream out) {
		this.messageType = (byte) ~this.messageType;
		this.sendMessage(out);
		this.messageType = (byte) ~this.messageType;
	}

	/*
	 * sendAgent: +Request: requestType (1 byte) X (4 bytes) Y (4 bytes)
	 * Agent(serialized) (? bytes)
	 */
	public void sendAgent(int x, int y, Agent agent) {
		byte[] agentBytes = agent.toBytes();
		int messageSize = agentBytes.length + 4 + 4;
		ByteBuffer buffer = ByteBuffer.allocate(messageSize);
		this.messageType = SENDAGENT;
		buffer.putInt(x);
		buffer.putInt(y);
		buffer.put(agentBytes);
		byte[] bytes = buffer.array();
		this.data = bytes;
	}

	public ReceivedAgent recvAgent() {
		ReceivedAgent result = null;
		try {
			result = new ReceivedAgent();
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
			result.x = dis.readInt();
			result.y = dis.readInt();
			result.agent = Agent.read(dis);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void recvAgent(InputStream in) {

		try {
			int dataSize = readMessage(in);
			System.out.println("size:" + dataSize);
			data = new byte[dataSize];
			int bytesRead = 0;
			do {
				bytesRead += in.read(data, bytesRead, dataSize - bytesRead);
				System.out.println("Read " + bytesRead + " of " + dataSize);
			} while (bytesRead < dataSize);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Received agent: ");
		this.print();
	}

	public static void sendEndTurn(OutputStream out, int turn) {
		try {
			synchronized (out) {
				DataOutputStream dos = new DataOutputStream(out);
				dos.write(ENDTURN);
				dos.writeInt(turn);
				System.out.println("Sending ENDTURN " + turn);
				dos.flush();
				out.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int recvEndTurn(InputStream in) {
		int turn = -1;
		try {
			DataInputStream dis = new DataInputStream(in);
			// TODO: Check message type.
			turn = dis.readInt();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return turn;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
