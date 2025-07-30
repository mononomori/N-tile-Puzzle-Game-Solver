package fifteenpuzzle;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class Solver
{
	static class Board
	{
		private final byte[] tiles;
		private final byte prevBlankTileID;
		private byte curBlankTileID;
		private final int moves;
		private byte wrongPlace;
		private int heuristicWeight;
		Board nextBoard = null, prevBoard = null;
		public Board(byte[] blocks)
		{
			tiles = Arrays.copyOf(blocks, boardSize);
			this.prevBlankTileID = this.curBlankTileID = -1;
			this.moves = 0;
			heuristicWeight = wrongPlace = 0;
			for (byte i = 0; i < boardSize; i++)
			{
				this.tiles[i] = blocks[i];
				if (tiles[i] == -1)
				{
					this.curBlankTileID = i;
				}
				else
				{
					if (isWrongPlace(i))
					{
						wrongPlace++;
					}
					heuristicWeight += getManhattan(i);
				}
			}
			for(int row=0;row<rows;row++)
			{
				heuristicWeight += getLinearConflictRow(row);
			}
			for(int col=0;col<cols;col++)
			{
				heuristicWeight += getLinearConflictCol(col);
			}
		}
		public Board(Board prev, byte newId)
		{
			this.prevBoard = prev;
			this.tiles = Arrays.copyOf(prev.tiles, boardSize);
			this.moves = prev.moves + 1;
			this.wrongPlace = prev.wrongPlace;
			this.heuristicWeight = prev.heuristicWeight;
			this.prevBlankTileID = prev.curBlankTileID;
			this.curBlankTileID = newId;
			swapWithPrev();
		}
		private void swapWithPrev()
		{
			this.heuristicWeight++;
			if (isWrongPlace(this.curBlankTileID))
			{
				wrongPlace--;
			}
			this.heuristicWeight -= getManhattan(this.curBlankTileID);

			int row1 = this.curBlankTileID/cols,
					row2 = this.prevBlankTileID/cols,
					row3 = this.tiles[this.curBlankTileID]/cols;
			int col1 = this.curBlankTileID%cols,
					col2 = this.prevBlankTileID%cols,
					col3 = this.tiles[this.curBlankTileID]%cols;

			if(col1 != col2 && (col1 == col3 || col2 == col3))
			{
				this.heuristicWeight -= getLinearConflictCol(col3);
			}
			else if(row1 != row2 && (row1 == row3 || row2 == row3))
			{
				this.heuristicWeight -= getLinearConflictRow(row3);
			}

			byte tile = this.tiles[curBlankTileID];
			this.tiles[curBlankTileID] = this.tiles[prevBlankTileID];
			this.tiles[prevBlankTileID] = tile;

			if (isWrongPlace(this.prevBlankTileID))
			{
				wrongPlace++;
			}

			this.heuristicWeight += getManhattan(this.prevBlankTileID);

			if(col1 != col2 && (col1 == col3 || col2 == col3))
			{
				this.heuristicWeight += getLinearConflictCol(col3);
			}
			else if(row1 != row2 && (row1 == row3 || row2 == row3))
			{
				this.heuristicWeight += getLinearConflictRow(row3);
			}
		}
		private boolean isWrongPlace(byte id)
		{
			return tiles[id] != id;
		}
		private int getManhattan(byte id)
		{
			return manhattanTable[id][this.tiles[id]];
		}
		private int getLinearConflictRow(int row)
		{
			int max = -1;
			int result = 0;
			for(int col=0,id=row*cols;col<cols;col++,id++)
			{
				if(tiles[id] == EMPTY || tiles[id]/cols != row)
					continue;
				if(max < tiles[id])
				{
					max = tiles[id];
				}
				else
				{
					result += 2;
				}
			}
			return result;
		}
		private int getLinearConflictCol(int col)
		{
			int max = 0;
			int result = 0;
			for(int row=0,id=col;row<rows;row++,id+=cols)
			{
				if(tiles[id] == EMPTY || tiles[id]%cols != col)
					continue;
				if(max < tiles[id])
				{
					max = tiles[id];
				}
				else
				{
					result += 2;
				}
			}
			return result;
		}
		public boolean isGoal()
		{
			if (wrongPlace != 0)
			{
				return false;
			}
			return tiles[boardSize - 1] == EMPTY;
		}
	}
	// Puzzle
	static final int EMPTY = -1;
	static byte rows, cols, boardSize;
	static byte[][]nextMove;
	static int[][]manhattanTable;
	public static void main(String[] args) throws Exception
	{
		System.out.println("number of arguments: " + args.length);
		for (String arg : args)
		{
			System.out.println(arg);
		}
		if (args.length < 2)
		{
			System.out.println("File names are not specified");
			System.out.println("usage: java " + MethodHandles.lookup().lookupClass().getName() + " input_file output_file");
			return;
		}
		FileInputStream inputFile = new FileInputStream(args[0]);
		FileOutputStream outputFile = new FileOutputStream(args[1]);
		final InputReader input = new InputReader(inputFile);
		final OutputWriter output = new OutputWriter(outputFile);
		rows = input.readByte();
		cols = rows;
		Board initial;
		{
			boardSize = (byte)(rows * cols);
			byte[]inputTiles = new byte[boardSize];
			for(int i=0;i<boardSize;i++)
			{
				inputTiles[i]=(byte)(input.readByte()-1);
			}
			nextMove = new byte[boardSize][];
			for(int i=0;i<boardSize;i++)
			{
				int moves = 4;
				int row = i/cols;
				int col = i%cols;
				if(row==0||(row+1==rows))moves--;
				if(col==0||(col+1==cols))moves--;
				nextMove[i] = new byte[moves];
				if(row > 0)
				{
					nextMove[i][--moves]=(byte)(((row-1)*cols) + col);
				}
				if(col > 0)
				{
					nextMove[i][--moves]=(byte)((row*cols) + col-1);
				}
				if(row+1 < rows)
				{
					nextMove[i][--moves]=(byte)(((row+1)*cols) + col);
				}
				if(col+1 < cols)
				{
					nextMove[i][--moves]=(byte)((row*cols) + col+1);
				}
			}
			manhattanTable = new int[boardSize][boardSize];
			for(int i=0;i<boardSize;i++)
			{
				manhattanTable[i][i]=0;
			}
			for(int i=0;i<boardSize;i++)
			{
				for(int j=i+1;j<boardSize;j++)
				{
					int goalDistance = Math.abs((i / cols) - (j / cols)) + Math.abs((i % cols) - (j % cols));
					manhattanTable[i][j] = manhattanTable[j][i] = goalDistance;
				}
			}
			initial = new Board(inputTiles);
		}
		calcIDAStar(initial);
		outputSolution(initial, output);

	}
	static void outputSolution(Board initial, OutputWriter output)
	{

		while(initial != null)
		{
			int curBlankPosition = initial.curBlankTileID;
			int prevBlankPosition = initial.prevBlankTileID;
			if (curBlankPosition - prevBlankPosition == 1 && curBlankPosition != 0)
			{
				output.printLine(initial.tiles[prevBlankPosition] + 1 + " L");
			}
			else if(curBlankPosition - prevBlankPosition == -1 && curBlankPosition != boardSize)
			{
				output.printLine(initial.tiles[prevBlankPosition] + 1 + " R");
			}
			else if (curBlankPosition - prevBlankPosition == cols && curBlankPosition != cols - 1)
			{
				output.printLine(initial.tiles[prevBlankPosition] + 1 + " U");
			}
			else if (prevBlankPosition - curBlankPosition == cols)
			{
				output.printLine(initial.tiles[prevBlankPosition] + 1 + " D");
			}
			initial = initial.nextBoard;
		}

		output.close();
	}
	static void calcIDAStar(Board initial)
	{
		int costBound = initial.heuristicWeight;
		while(initial.nextBoard == null)
		{
			if(dfs(initial, costBound)!=null)
			{
				break;
			}
			costBound+=2;
		}
	}
	static Board dfs(Board board, int maxBound)
	{
		if(board.heuristicWeight > maxBound)
		{
			return null;
		}
		if(board.isGoal())
		{
			return board;
		}
		for(byte i=0;i<nextMove[board.curBlankTileID].length;i++)
		{
			if(board.prevBlankTileID == nextMove[board.curBlankTileID][i])
			{

				continue;
			}
			board.nextBoard = dfs(new Board(board, nextMove[board.curBlankTileID][i]), maxBound);
			if(board.nextBoard != null)
			{
				return board;
			}
		}
		return null;
	}

	static class InputReader
	{
		private final InputStream stream;
		private final byte[] inputBuffer = new byte[32768];
		private int curChar;
		private int numChars;
		public InputReader(InputStream stream)
		{
			this.stream = stream;
		}
		public int read()
		{
			if (curChar >= numChars)
			{
				curChar = 0;
				try
				{
					numChars = stream.read(inputBuffer);
				}
				// We know our inputs will be valid based on the test cases
				catch (IOException ignored)
				{

				}
				if (numChars <= 0)
					return -1;
			}
			return inputBuffer[curChar++];
		}
		public byte readByte()
		{
			int currentChar = read();
			int spaces = 0;
			while (isSpaceChar(currentChar))
			{
				spaces++;
				if (spaces == 2)
				{
					currentChar = '0';

					break;
				}
				currentChar = read();
			}
			byte result = 0;
			do
			{
				if (currentChar < '0' || currentChar > '9')
				{
					throw new InputMismatchException();
				}
				result *= 10;
				result += currentChar - '0';
				currentChar = read();
			} while (!isSpaceChar(currentChar));
			return result;
		}
		public boolean isSpaceChar(int c)
		{
			return c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == -1;
		}
	}
	static class OutputWriter
	{
		private final PrintWriter writer;
		public OutputWriter(OutputStream outputStream)
		{
			writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream)));
		}
		public void print(Object... objects)
		{
			for (int i = 0; i < objects.length; i++)
			{
				if (i != 0)
				{
					writer.print(' ');
				}
				writer.print(objects[i]);
			}
		}
		public void printLine(Object... objects)
		{
			print(objects);
			writer.println();
		}
		public void close()
		{
			writer.close();
		}
	}
}
