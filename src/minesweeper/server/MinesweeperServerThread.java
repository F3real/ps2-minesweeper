package minesweeper.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import minesweeper.Board;

public class MinesweeperServerThread extends Thread {

	private static final String BYE_MESSAGE = "Bye!";
	private static final String HELP_MESSAGE = "RTFM!";
	private static final String BOOM_MESSAGE = "BOOM!";
	private static final String INVALID_INPUT_MESSAGE = "Invalid input!";

	/*
	 * Counter for number of players and a lock to make this operation
	 * threadsafe.
	 */
	private static int playerCount = 0;
	private static Object playerCountLock = new Object();

	private Socket clientSocket;
	private Board minesweeperBoard;
	private boolean debug;

	/**
	 * <pre>
	 * Invariants: 
	 *  - minesweeperBoard is never null
	 * </pre>
	 */

	public MinesweeperServerThread(Socket socket, Board minesweeperBoard, boolean debug) {
		this.clientSocket = socket;
		this.minesweeperBoard = minesweeperBoard;
		this.debug = debug;

		synchronized (playerCountLock) {
			playerCount++;
		}
	}

	public void run() {

		try {
			handleConnection(clientSocket);
			clientSocket.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		synchronized (playerCountLock) {
			playerCount--;
		}
	}

	/**
	 * Handle a single client connection. Returns when client disconnects.
	 * 
	 * @param socket
	 *            socket where the client is connected
	 * @throws IOException
	 *             if the connection encounters an error or terminates
	 *             unexpectedly
	 */
	private void handleConnection(Socket socket) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

		synchronized (playerCountLock) {
			out.println("Welcome to Minesweeper. Players: " + playerCount + " including you. Board: "
					+ minesweeperBoard.getColumnsNumber() + " columns by " + minesweeperBoard.getRowsNumber()
					+ " rows. Type 'help' for help.");
		}
		try {
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				
				String output = handleRequest(line);
				out.println(output);
				if (output == BYE_MESSAGE || (output == BOOM_MESSAGE && !debug)) {
					break;
				}

			}
		} finally {
			out.close();
			in.close();
		}
	}

	/**
	 * Handler for client input, performing requested operations and returning
	 * an output message.
	 * 
	 * @param input
	 *            message from client
	 * @return message to client, or null if none
	 */
	private String handleRequest(String input) {
		String regex = "(look)|(help)|(bye)|" + "(dig -?\\d+ -?\\d+)|(flag -?\\d+ -?\\d+)|(deflag -?\\d+ -?\\d+)";
		if (!input.matches(regex)) {
			// invalid input
			return INVALID_INPUT_MESSAGE;
		}
		String[] tokens = input.split(" ");
		if (tokens[0].equals("look")) {
			// 'look' request
			return this.minesweeperBoard.getBoardString();
		} else if (tokens[0].equals("help")) {
			// 'help' request
			return HELP_MESSAGE;
		} else if (tokens[0].equals("bye")) {
			// 'bye' request
			return BYE_MESSAGE;
		} else {
			int x = Integer.parseInt(tokens[1]);
			int y = Integer.parseInt(tokens[2]);
			if (tokens[0].equals("dig")) {
				// 'dig x y' request
				// TODO parameters are swapped for tests
				if (this.minesweeperBoard.dig(y, x)) {
					return this.minesweeperBoard.getBoardString();
				}
				return BOOM_MESSAGE;
			} else if (tokens[0].equals("flag")) {
				// 'flag x y' request
				// TODO parameters are swapped for tests
				this.minesweeperBoard.flag(y, x);
				return this.minesweeperBoard.getBoardString();
			} else if (tokens[0].equals("deflag")) {
				// 'deflag x y' request
				// TODO parameters are swapped for tests
				this.minesweeperBoard.deflag(y, x);
				return this.minesweeperBoard.getBoardString();
			}
		}
		throw new UnsupportedOperationException();
	}

}
