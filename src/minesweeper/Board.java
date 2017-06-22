/* Copyright (c) 2007-2017 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package minesweeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Random;

public class Board {

	private static final double BOMB_PROBABILITY = 0.25;

	private enum FieldStatus {
		UNTOUCHED, DUG, FLAGGED
	};

	private class Field {
		public FieldStatus status;
		public boolean hasBomb;
		public int neighbourBombs;

		public Field(FieldStatus status, boolean hasBomb) {
			this.status = status;
			this.hasBomb = hasBomb;
		}
	}

	/**
	 * Representation of the Minesweeper board.
	 * 
	 * <pre>
	 * Invariants: 
	 *  - board is always of dimensions [sizeX][sizeY] - x > 0 and y > 0
	 *  - there is no null fields in board
	 *  - number of bombs in neighbourhood of Field should be always >= 0
	 *  - boardString is never null
	 * </pre>
	 */

	private Field[][] board;
	private final int sizeX;
	private final int sizeY;
	private StringBuilder boardString;
	private boolean isStringUpdated;

	/**
	 * Constructor for a random board. Each fields has BOMB_PROBABILITY chance
	 * to contain a bomb.
	 * 
	 * @param x
	 *            Number of rows in board.
	 * @param y
	 *            Number of columns in board. the board.
	 */
	public Board(int x, int y) {
		// TODO Sizes are swapped for test
		this.sizeX = y;
		this.sizeY = x;

		assert (x > 0 && y > 0);

		this.board = new Field[sizeX][sizeY];

		/* Initialize board with bombs. */
		Random rand = new Random();
		double generatedValue;
		for (int i = 0; i < this.sizeX; i++) {
			for (int j = 0; j < this.sizeY; j++) {
				generatedValue = rand.nextDouble();
				if (generatedValue < BOMB_PROBABILITY) {
					board[i][j] = new Field(FieldStatus.UNTOUCHED, true);
				} else {
					board[i][j] = new Field(FieldStatus.UNTOUCHED, false);
				}
			}
		}

		updateBoardBombCount();
		createBoardString();
	}

	/**
	 * Construct board from file. The file format for input is:
	 * 
	 * <pre>
	 *  FILE ::= BOARD LINE+
	 * 	BOARD := X SPACE Y NEWLINE
	 * 	LINE ::= (VAL SPACE)* VAL NEWLINE
	 * 	VAL ::= 0 | 1
	 * 	X ::= INT
	 * 	Y ::= INT
	 * 	SPACE ::= " "
	 * 	NEWLINE ::= "\n" | "\r" "\n"?
	 * 	INT ::= [0-9]+
	 * </pre>
	 * 
	 * In a properly formatted file matching the FILE grammar, the first line
	 * specifies the board size, and it must be followed by exactly Y lines,
	 * where each line must contain exactly X values.
	 * 
	 * @param boardFile
	 *            File containing board in textual form.
	 */
	public Board(File boardFile) {

		/*
		 * Use temporary variables to avoid compiler complains about final
		 * variables possibly not being initialised.
		 */
		int sizeX = 0;
		int sizeY = 0;

		try (BufferedReader br = new BufferedReader(new FileReader(boardFile))) {
			String line;
			String[] elements;

			/* Get size of board. */
			line = br.readLine();
			elements = line.split(" ");
			// TODO Sizes are swapped for test
			sizeX = Integer.valueOf(elements[1]);
			sizeY = Integer.valueOf(elements[0]);
			assert (sizeX > 0 && sizeY > 0);
			this.board = new Field[sizeX][sizeY];

			int x = 0;
			while ((line = br.readLine()) != null) {
				int y = 0;
				elements = line.split(" ");
				for (String element : elements) {
					board[x][y] = new Field(FieldStatus.UNTOUCHED, Integer.valueOf(element) == 1);
					y++;
				}
				x++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		this.sizeX = sizeX;
		this.sizeY = sizeY;

		updateBoardBombCount();
		createBoardString();

		assert checkRep();
	}

	/**
	 * Checks rep invariants.
	 * 
	 * @return boolean true if rep invariants hold, false otherwise.
	 */
	private boolean checkRep() {
		if (this.sizeX <= 0 || this.sizeY <= 0) {
			return false;
		}
		if (board.length != this.sizeX) {
			return false;
		}
		if (board[0].length != this.sizeY) {
			return false;
		}
		for (int i = 0; i < this.sizeX; i++) {
			for (int j = 0; j < this.sizeY; j++) {
				if (this.board[i][j] == null) {
					return false;
				}
			}
		}
		if (this.boardString == null) {
			return false;
		}
		return true;
	}

	/**
	 * Get column number of board.
	 * 
	 * @return int Number of columns.
	 */
	public int getColumnsNumber() {
		return this.sizeY;
	}

	/**
	 * Get row number of board.
	 * 
	 * @return int Number of rows.
	 */
	public int getRowsNumber() {
		return this.sizeX;
	}

	/**
	 * Update number of bombs in neighbourhood of each Field.
	 * 
	 */
	private void updateBoardBombCount() {
		for (int i = 0; i < this.sizeX; i++) {
			for (int j = 0; j < this.sizeY; j++) {
				updateFieldBombCount(i, j);
			}
		}
	}

	/**
	 * Update number of bombs in neighbourhood of single Field.
	 * 
	 * @param x
	 *            Coordinate x of Field we are updating.
	 * @param y
	 *            Coordinate y of Field we are updating.
	 * 
	 */
	private void updateFieldBombCount(int x, int y) {
		int numberOfBombs = 0;

		int[] xCoords = { x - 1, x, x + 1, x - 1, x + 1, x - 1, x, x + 1 };
		int[] yCoords = { y - 1, y - 1, y - 1, y, y, y + 1, y + 1, y + 1 };

		for (int i = 0; i < xCoords.length; i++) {
			int xCoord = xCoords[i];
			int yCoord = yCoords[i];
			if (xCoord >= 0 && yCoord >= 0 && xCoord < this.sizeX && yCoord < this.sizeY) {
				if (this.board[xCoord][yCoord].hasBomb) {
					numberOfBombs++;
				}
			}
		}

		this.board[x][y].neighbourBombs = numberOfBombs;
	}

	/**
	 * Creates string representing board.
	 * 
	 */
	private void createBoardString() {
		this.boardString = new StringBuilder();
		for (int i = 0; i < this.sizeX; i++) {
			for (int j = 0; j < this.sizeY; j++) {

				if (this.board[i][j].status == FieldStatus.UNTOUCHED) {
					this.boardString.append("-");
				} else if (this.board[i][j].status == FieldStatus.DUG) {
					if (this.board[i][j].neighbourBombs == 0) {
						this.boardString.append(" ");
					} else {
						this.boardString.append(String.valueOf(this.board[i][j].neighbourBombs));
					}
				} else {
					// third option is that state is Flagged
					this.boardString.append("F");
				}

				if (j == this.sizeY - 1) {
					// Since pritln adds newline we have to remove last one for
					// tests to pass
					if (i != this.sizeX - 1) {
						this.boardString.append(System.getProperty("line.separator"));
					}
				} else {
					this.boardString.append(" ");
				}
			}
		}
		this.isStringUpdated = true;
	}

	/**
	 * Digs single field.
	 * 
	 * @param x
	 *            Coordinate x of Field we are digging.
	 * @param y
	 *            Coordinate y of Field we are digging.
	 * 
	 * @return boolean if dig was performed without triggering bomb
	 */
	public synchronized boolean dig(int x, int y) {
		// Check if dig should re performed
		if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY) {
			return true;
		}
		if (this.board[x][y].status != FieldStatus.UNTOUCHED) {
			return true;
		}
		boolean res = true;
		// flag that board should be updated
		this.isStringUpdated = false;

		if (this.board[x][y].hasBomb) {
			// if field has bomb remove it and update bomb counts
			this.board[x][y].hasBomb = false;
			// TODO we can only update 8 surrounding fields
			updateBoardBombCount();
			res = false;
		}

		dig_internal(x, y);
		assert checkRep();
		return res;
	}

	private void dig_internal(int x, int y) {
		// check for bounds of coordinate
		if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY) {
			return;
		}
		if (this.board[x][y].status != FieldStatus.UNTOUCHED) {
			return;
		}

		this.board[x][y].status = FieldStatus.DUG;

		if (this.board[x][y].neighbourBombs == 0) {
			// if field has 0 bombs in neighbourhood dig those fields too
			int[] xCoords = { x - 1, x, x + 1, x - 1, x + 1, x - 1, x, x + 1 };
			int[] yCoords = { y - 1, y - 1, y - 1, y, y, y + 1, y + 1, y + 1 };

			for (int i = 0; i < xCoords.length; i++) {
				dig_internal(xCoords[i], yCoords[i]);
			}
		}
	}

	/**
	 * Flags single field.
	 * 
	 * @param x
	 *            Coordinate x of Field we are flagging.
	 * @param y
	 *            Coordinate y of Field we are flagging.
	 */
	public synchronized void flag(int x, int y) {
		// check for bounds of coordinate
		if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY) {
			return;
		}
		if (this.board[x][y].status != FieldStatus.UNTOUCHED) {
			return;
		}

		// flag that board should be updated
		this.isStringUpdated = false;
		this.board[x][y].status = FieldStatus.FLAGGED;
	}

	/**
	 * Removes flag from single field.
	 * 
	 * @param x
	 *            Coordinate x of Field we are deflagging.
	 * @param y
	 *            Coordinate y of Field we are deflagging.
	 */
	public synchronized void deflag(int x, int y) {
		// check for bounds of coordinate
		if (x < 0 || x >= this.sizeX || y < 0 || y >= this.sizeY) {
			return;
		}
		if (this.board[x][y].status != FieldStatus.FLAGGED) {
			return;
		}

		// flag that board should be updated
		this.isStringUpdated = false;
		this.board[x][y].status = FieldStatus.UNTOUCHED;
	}

	/**
	 * Get string representing board.
	 * 
	 * @return String representing board status.
	 */
	public synchronized String getBoardString() {
		if (this.isStringUpdated) {
			return this.boardString.toString();
		}
		createBoardString();
		return this.boardString.toString();
	}
}
