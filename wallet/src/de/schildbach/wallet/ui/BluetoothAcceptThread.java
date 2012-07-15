package de.schildbach.wallet.ui;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

public abstract class BluetoothAcceptThread extends Thread {

	private BluetoothServerSocket listeningSocket;
	private AtomicBoolean running = new AtomicBoolean(true);

	public BluetoothAcceptThread(BluetoothServerSocket listeningSocket) {
		this.listeningSocket = listeningSocket;
	}

	@Override
	public void run() {
		System.out
				.println("BTTX Thread run");
		try {
			if (listeningSocket != null) {
				System.out.println("BTTX we have a listening socket");
				while (running.get()) {
					try {
						// start a blocking call, and return only on success or
						// exception
						BluetoothSocket socket = listeningSocket.accept();
						if (socket != null) {
							System.out.println("BTTX accepted");
							DataInputStream inputStream = new DataInputStream(
									socket.getInputStream());
							int msgLeft = inputStream.readInt();
							while (msgLeft > 0) {
								System.out.println("BTTX reading msg");
								int msgLength = inputStream.readInt();
								byte[] msg = new byte[msgLength];
								inputStream.readFully(msg);
								handleTx(msg);
								msgLeft--;
							}
							inputStream.close();
							socket.close();
						}
					} catch (IOException e) {
						System.out.println("BTTX exception during accept "
								+ e.getMessage());
					}
				}
				listeningSocket.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void stopAccepting() {
		running.set(false);
		try {
			listeningSocket.close();
			System.out.println("BTTX stop accepting");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract void handleTx(byte[] msg);
}
