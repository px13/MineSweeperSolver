import java.awt.AWTException; 
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

public final class MineSweeper {	
	private static MinesweeeperGame game;
	
	public static void main(String[] args) throws AWTException, InterruptedException {
		System.out.println("pred spustenim si pusti minesweeper.exe v beginner mode");		
		System.out.println("prestan drgat do mysi, to nemam rad !");		
		Thread.sleep(2000);		
		System.out.println("a nechyt sa mysi az do konca testu !!!");		
		game = new MinesweeeperGame(50);
		int wins = 0;
		int lost = 0;
		final int NUMBEROFGAMES = 100;
		for (int i = 0; i<NUMBEROFGAMES ; i++) {
			System.out.print("\t\t\t\t\t");
			boolean solved = solveGame(); 
			if (solved)
				wins++;
			else 
				lost++;
			System.out.println("\nGame " + (i+1) + " of "+ NUMBEROFGAMES + ((solved)?" WIN! :-) [":" LOST :-( [") + wins + ":" + lost + "=" + wins*100/(wins+lost) + "%]" );
		}
		System.out.println("WINS:"+wins+ ", LOSTS:"+lost);
	}
	
	/**
	 * hra hru az kym sme vyhrali ci prehrali
	 * @return true, ak sme vyhrali, false ak sme prehlali
	 */
	private static boolean solveGame() {
		Random random = new Random();
		game.initGame();	// priprav hru		
		int tah = 0;
		game.setFlagged(game.rows-1, game.cols-1);
		while (true) {
			if (tah == 0)  // prvy tah je vzdy nahodny, niet co riesit !
				game.openCell(random.nextInt(game.cols), random.nextInt(game.rows));
			else if (smartMove())  // dalsie tahy
				System.out.print('s'); // ak neexistuje sikovny tah, tahaj nahodne
			else {
				System.out.print('r'); // r ako random 
				randomMove();
			}
			tah++;
			game.readSmiley();
			if (game.isSadFace())  // prehrali sme 
				return false;
			else if (game.isWinnersFace())  // vyhrali sme
				return true;
			game.readGameBoard();
		}
	}
	
	/*
	 * pokusi sa o sikovny tah
	 * @return true, ak sme nasli sikovny tah, false, ak sme ho nanasli
	 */
	private static boolean smartMove() {
		boolean zmena = true;
		while (zmena != true);
			zmena = false;
			for (int y = 0 ; y < game.rows ; y++) {
				for (int x = 0 ; x < game.cols ; x++) {
					if (game.isFlagged(x, y) || game.isUnOpen(x, y)) continue;
					int cislo = game.getBombsAround(x, y);
					if (cislo == 0) continue;
					int pocetNeotvorenychSusedov = countNeighboringUnopened(x,y); 
					int pocetVlajkovychSusedov = countNeighboringFlagged(x,y);
					if (pocetNeotvorenychSusedov + pocetVlajkovychSusedov == cislo && pocetNeotvorenychSusedov != 0) {
						setNeighboringUnopenedToFlag(x,y);
						zmena = true;
						continue;
					}
					if (cislo - pocetVlajkovychSusedov == 0) {
						for (int yy = y - 1; yy <= y + 1; yy++) {
							for (int xx = x - 1; xx <= x + 1; xx++) {
								if (game.isUnOpen(xx, yy) && (game.isFlagged(xx, yy) != true)) {
									game.openCell(xx, yy);
									return true;
								}
							}
						}
						continue;
					}		
				}
			}
		return pravdepodobnostnyTah();
	}
	
	private static boolean pravdepodobnostnyTah() {
		int minX = 0;
		int minY = 0;
		double minPr = 1;
		int vlajky = 0;
		int prazdne = 0;
		double[][][] pole = new double[game.cols][game.rows][8];
		for (int y = 0 ; y < game.rows ; y++) {
			for (int x = 0 ; x < game.cols ; x++) {
				if (game.isFlagged(x, y)) {
					vlajky++;
					continue;
				}
				if (game.isUnOpen(x, y)) {
					prazdne++;
					continue;
				}
				int cislo = game.getBombsAround(x, y);
				if (cislo == 0) continue;
				int pocetNeotvorenychSusedov = countNeighboringUnopened(x,y); 
				int pocetVlajkovychSusedov = countNeighboringFlagged(x,y);
				if (cislo - pocetVlajkovychSusedov < pocetNeotvorenychSusedov && cislo - pocetVlajkovychSusedov > 0) {
					double pom = ((double) (cislo - pocetVlajkovychSusedov)) / pocetNeotvorenychSusedov;

					for (int yy = y - 1; yy <= y + 1; yy++) {
						for (int xx = x - 1; xx <= x + 1; xx++) {
							if (game.isUnOpen(xx, yy) && (game.isFlagged(xx, yy) != true)) {
								int poc = 0;
								while (pole[xx][yy][poc] != 0) {
									poc++;
								}			
								pole[xx][yy][poc] = pom;
							}
						}
					}
				}
				
				
			}
		}
		for (int y = 0 ; y < game.rows ; y++) {
			for (int x = 0 ; x < game.cols ; x++) {
				if (pole[x][y][0] == 0) continue;
				int poc = 0;
				while (pole[x][y][poc] != 0) {
					poc++;
				}
				double pr = spracujPr(pole[x][y],poc-1);
				if (pr < minPr) {
					minPr = pr;
					minX = x;
					minY = y;
				}
			}
		}
		
		if (minPr > 1.5*((double) (10-vlajky)) / prazdne) {
			return false;
		}
		game.openCell(minX, minY);
		return true;
	}
	
	private static double spracujPr(double[] pole, int k) {
		int poc = 1;
		while (poc != k) {
			if (pole[poc] == 0) {
				return pole[0];
			}
			pole[0] = (pole[0]+pole[poc])/2;
			poc++;
		}
		return pole[0];
	}
	/*
	 * urobi prvy mozny hlupy tah
	 */
	
	private static void setNeighboringUnopenedToFlag(int x, int y) {
		for (int yy = y - 1; yy <= y + 1; yy++) {
			for (int xx = x - 1; xx <= x + 1; xx++) {
				if (game.isUnOpen(xx, yy) && game.isFlagged(xx, yy) != true) {
					game.flagCell(xx, yy);
					game.setFlagged(xx, yy);
				}
			}
		}
	}
	
	private static void firstMove() {
		game.openCell(0, 0);
	}
	
	/*
	 * urobi nahodne hlupy tah
	 */
	private static void randomMove() {
		Random random = new Random();
		while (true) {
			int y = random.nextInt(game.rows); 
			int x = random.nextInt(game.cols); 
			if (game.isUnOpen(x, y) && game.isFlagged(x, y) != true) {
				game.openCell(x, y);
				return;
			}
		}
	}

	/**
	 * pocet neodkrytych v okoli x,y 
	 */
	private static int countNeighboringUnopened(int x, int y) {
		int count = 0;
		for (int yy = y - 1; yy <= y + 1; yy++) {
			for (int xx = x - 1; xx <= x + 1; xx++) {
				if (game.isUnOpen(xx, yy))
					count++;
			}
		}
		return count;
	}
	/**
	 * pocet policok s vlajockou v okoli x,y 
	 */	
	private static int countNeighboringFlagged(int x, int y) {
		int count = 0;
		for (int yy = y - 1; yy <= y + 1; yy++) {
			for (int xx = x - 1; xx <= x + 1; xx++) {
				if (game.isFlagged(xx, yy))
					count++;
			}
		}
		return count;
	}
	private static boolean isNeighbor(int x0, int y0, int x1, int y1) {
		return Math.max(Math.abs(x0 - x1), Math.abs(y0 - y1)) <= 1;
	}
}

//-------------------------------------------------- ODTIALTO DALEJ NIC NEMENTE
//-------------------------------------------------- ODTIALTO DALEJ NIC NEMENTE
//-------------------------------------------------- staci si pozriet a nastudovat
//-------------------------------------------------- public metody a premenne
//-------------------------------------------------- ODTIALTO DALEJ NIC NEMENTE
//-------------------------------------------------- ODTIALTO DALEJ NIC NEMENTE

final class MinesweeeperGame {
	private MyRobot robot;
	private final Rectangle smileyButton;
	private final Rectangle gameBoardRectangle;
	private final Point cellSize;
	
	public int cols;	// velkost hracej plochy
	public int rows;

	private final static int SAD_FACE = 1;
	private final static int WINNERS_FACE = 2;
	private int gameState;	// SMILE/FROWN/WINNERS FACE

	/**
	 * @return vrati true, ak sme prehrali, v GUI je sad face
	 */
	public boolean isSadFace() {
		return gameState == SAD_FACE;
	}
	/**
	 * @return vrati true, ak sme vyhrali, v GUI je happy face
	 */
	public boolean isWinnersFace() {
		return gameState == WINNERS_FACE;
	}
	
	private static MyImage[] SMILEY_IMAGES = {
		new MyImage("smile.png"),
		new MyImage("frown.png"),
		new MyImage("sunglasses.png")
	};
    //---------------------------------------------
	
	class Box {
		private int bombsAroundIfOpen;
		private boolean unopen;		// neodkryte policko
		private boolean flag;		// policko s vlajockou
		
		public int getBombsAround() {
			return bombsAroundIfOpen;
		}
		public void setBombsAround(int bombsAround) {
			this.bombsAroundIfOpen = bombsAround;
		}
		public boolean isUnOpen() {
			return unopen;
		}
		public void setUnOpen(boolean unopen) {
			this.unopen = unopen;
		}
		public boolean isFlag() {
			return flag;
		}
		public void setFlag(boolean flag) {
			this.flag = flag;
		}
		public int getState() {
			return getBombsAround();
		}
		public void setState(int b) {
			setBombsAround(b);
		}
	}
	private Box[][] gameBoard;		// hracia plocha
	
	
	public MinesweeeperGame(int actionDelay) throws AWTException {
		robot = new MyRobot(actionDelay);	
		// Detect screen resolution
		Rectangle allScreensBounds = new Rectangle();
		for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			for (GraphicsConfiguration gc : gd.getConfigurations())
				allScreensBounds = allScreensBounds.union(gc.getBounds());
		}
		// Find smiley button
		MyImage screenshot = robot.getScreenshot(allScreensBounds);
		Point smiley = findRegion(screenshot, SMILEY_IMAGES[0]);  // Try to find smile face
		if (smiley == null)
			smiley = findRegion(screenshot, SMILEY_IMAGES[1]);  // Or try to find frown face
		if (smiley == null)
			smiley = findRegion(screenshot, SMILEY_IMAGES[2]);  // Or try to find sunglasses face
		if (smiley == null)
			throw new RuntimeException("Smile button not found on screen");
		smileyButton = new Rectangle(allScreensBounds.x + smiley.x, allScreensBounds.y + smiley.y, SMILEY_IMAGES[0].width, SMILEY_IMAGES[0].height);
		clickSmiley();  // Give focus to Minesweeeper (if not focused it will give focus without activating the button; else it will click the button)
		clickSmiley();  // Click the button for sure
		System.out.printf("Smiley button: left=%d top=%d width=%d height=%d%n", smileyButton.x, smileyButton.y, smileyButton.width, smileyButton.height);
		
		// Find mine grid and its size
		MyImage UNOPENEDIMG = new MyImage("unopened.png");
		screenshot = robot.getScreenshot(allScreensBounds);
		
		// Find top left cell
		Point topLeft = findRegion(screenshot, UNOPENEDIMG);  // Relative to image coordinates (always non-negative), not screen coordinates (possibly negative or offset from zero)
		if (topLeft == null)
			throw new RuntimeException("Top left unopened cell not found on screen");
		
		// Find number of columns
		cellSize = new Point(UNOPENEDIMG.width, UNOPENEDIMG.height);
		cols = 0;
		for (int x = topLeft.x; x + cellSize.x <= screenshot.width && UNOPENEDIMG.equals(screenshot, x, topLeft.y); x += cellSize.x)
			cols++;
		
		// Find number of rows
		rows = 0;
		for (int y = topLeft.y; y + cellSize.y <= screenshot.height && UNOPENEDIMG.equals(screenshot, topLeft.x, y); y += cellSize.y)
			rows++;
		
		gameBoardRectangle = new Rectangle(allScreensBounds.x + topLeft.x, allScreensBounds.y + topLeft.y, cols * cellSize.x, rows * cellSize.y);
		// System.out.printf("Game board: cols=%d rows=%d; left=%d top=%d width=%d height=%d%n", cols, rows, gameBoard.x, gameBoard.y, gameBoardRectangle.width, gameBoardRectangle.height);
		
		gameBoard = new Box[rows][cols];
		System.out.printf("Action delay: %d ms%n", actionDelay);
		System.out.println();
	}
	/**
	 * inicializacia hry a nacitanie stavu z hracej plochy mineseeper.exe 
	 */	
	public void initGame() {
		clickSmiley();
		readGameBoard();
	}
	/**
	 * policko x,y je v hracej ploche  
	 */	
	public boolean isInBounds(int x, int y) {
		return x >= 0 && x < cols && y >= 0 && y < rows;
	}
	/**
	 * policko x,y je odkryte 
	 */
	public boolean isUnOpen(int x, int y) {
		if (isInBounds(x, y))
			return gameBoard[y][x].isUnOpen();
		else
			return false;
	}
	/**
	 * policko x,y ma zastavku 
	 */
	public boolean isFlagged(int x, int y) {
		if (isInBounds(x, y))
			return gameBoard[y][x].isFlag();
		else
			return false;
	}
	/**
	 * odkry policko x,y 
	 */
	public void setOpen(int x, int y) {
		if (isInBounds(x, y))
			gameBoard[y][x].setUnOpen(false);
	}
	/**
	 * daj zastavku na policko x,y 
	 */
	public void setFlagged(int x, int y) {
		if (isInBounds(x, y))
			gameBoard[y][x].setFlag(true);
	}
	/**
	 * ak je policko odkryte, kolko je bomb okolo 
	 */
	public int getBombsAround(int x, int y) {
		if (isInBounds(x, y))
			return gameBoard[y][x].getBombsAround();
		else
			return -1;
	}
			
	private void clickSmiley() {
		robot.click(smileyButton.x + smileyButton.width / 2, smileyButton.y + smileyButton.height / 2, InputEvent.BUTTON1_MASK);
	}
	/**
	 * klikli sme na policko x,y, lavou myskou 
	 */
	public void openCell(int x, int y) {
		clickCell(x, y, InputEvent.BUTTON1_MASK);
	}
	/**
	 * klikli sme na policko x,y, pravou myskou 
	 */
	public void flagCell(int x, int y) {
		clickCell(x, y, InputEvent.BUTTON3_MASK);
	}
	/**
	 * klikli sme na policko x,y, pravou myskou 
	 */
	public void show3x3Cels(int x, int y) {
		clickCell(x, y, InputEvent.BUTTON2_MASK);
	}
	
	private void clickCell(int x, int y, int button) {
		if (!isInBounds(x, y))
			throw new IllegalArgumentException();
		robot.click(gameBoardRectangle.x + x * cellSize.x + cellSize.x / 2, gameBoardRectangle.y + y * cellSize.y + cellSize.y / 2, button);
	}
	
	private final int MAXDELAY = 1024;  // in ms.
	/** 
	 * nacita hodnoty stavu (gameState) hry   
	 */	
	public void readSmiley() {
		for (int delay = 1; delay < MAXDELAY; delay *= 2) {
			gameState = indexOfImage(robot.getScreenshot(smileyButton), 0, 0, SMILEY_IMAGES);
			if (gameState != -1)
				return;
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {}
		}
		throw new RuntimeException("Unknown smiley state");
	}
	
	/** 
	 * nacita hodnoty z hracej plochy  
	 */	
	public void readGameBoard() {
		final int UNOPENED = 9;
		final int FLAG = 10;
		
		MyImage[] CELL_IMAGES = {
			new MyImage("neighbor0.png"),// 0
			new MyImage("neighbor1.png"),// 1
			new MyImage("neighbor2.png"),// 2
			new MyImage("neighbor3.png"),// 3
			new MyImage("neighbor4.png"),// 4
			new MyImage("neighbor5.png"),// 5
			new MyImage("neighbor6.png"),// 6
			new MyImage("neighbor7.png"),// 7
			new MyImage("neighbor8.png"),// 8
			new MyImage("unopened.png"),// 9
			new MyImage("flag.png"),	// 10
			new MyImage("question.png")	// 11
		};
		for (int delay = 1; delay < MAXDELAY; delay *= 2) {
			MyImage image = robot.getScreenshot(gameBoardRectangle);
			boolean fail = false;
			middle:
			for (int y = 0; y < rows; y++) {
				for (int x = 0; x < cols; x++) {
					int pom = indexOfImage(image, x * cellSize.x, y * cellSize.y, CELL_IMAGES);
					if (pom == -1) {
						fail = true;
						break middle;
					} else {
						gameBoard[y][x] = new Box();
						gameBoard[y][x].setBombsAround(pom);
						gameBoard[y][x].setUnOpen(pom == UNOPENED);
						gameBoard[y][x].setFlag(pom == FLAG);
							
					}
				}
			}
			if (!fail)
				return;
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {}
		}
		throw new RuntimeException("Unknown cell state");
	}
	
	/*
	 * hlada patter v obrazku image, postupne skusa vsetky offsety, 
	 * ak ho najde, vrati jeho Point, t.j. dvojicu offX, offY, inak null 
	 */	
	private static Point findRegion(MyImage image, MyImage pattern) {
		for (int offY = 0; offY + pattern.height <= image.height; offY++) 
			for (int offX = 0; offX + pattern.width <= image.width; offX++) 
				if (pattern.equals(image, offX, offY))
					return new Point(offX, offY);
		return null;
	}
	
	/*
	 * hlada niektory z patternov v obrazku image na offset offX, offY 
	 * ak najde, vrati jeho index, inak -1 
	 */
	private static int indexOfImage(MyImage image, int offX, int offY, MyImage[] patterns) {
		for (int i = 0; i < patterns.length; i++)
			if (patterns[i].equals(image, offX, offY))
				return i;
		return -1;
	}
}


/* Lightweight image class */

final class MyImage {
	
	public final int[] pixels;
	public final int width;
	public final int height;
	
	
	public MyImage(BufferedImage image) {
		width = image.getWidth();
		height = image.getHeight();
		pixels = new int[width * height];
		
		SampleModel sm = image.getRaster().getSampleModel();
		if (image.getType() == BufferedImage.TYPE_INT_RGB &&
		    sm.getDataType() == DataBuffer.TYPE_INT &&
		    Arrays.equals(sm.getSampleSize(), new int[]{8, 8, 8})) {  // Fast path
			
			int[] temp = image.getRaster().getPixels(0, 0, width, height, (int[])null);
			for (int i = 0; i < pixels.length; i++) {
				pixels[i] = temp[i * 3 + 0] << 16
				          | temp[i * 3 + 1] <<  8
				          | temp[i * 3 + 2] <<  0;
			}
			
		} else {  // General path
			image.getRGB(0, 0, width, height, pixels, 0, width);
			for (int i = 0; i < pixels.length; i++)
				pixels[i] &= 0xFFFFFF;  // Get rid of alpha channel
		}
	}
	
	
	public MyImage(String filename) {
		this(readFile(filename));
	}
	
	
	public boolean equals(MyImage other, int offX, int offY) {
		if (other.width < width || other.height < height)
			throw new IllegalArgumentException();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (other.pixels[(offY + y) * other.width + offX + x] != pixels[y * width + x])
					return false;
			}
		}
		return true;
	}
	
	
	private static BufferedImage readFile(String filename) {
		File file = new File(filename);
		if (!file.isFile()) {
			System.err.println("File does not exist: " + file);
			System.exit(1);
		}
		try {
			return ImageIO.read(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}

/* Convenience wrapper for java.awt.Robot */

final class MyRobot {
	
	private Robot robot;
	private int delay;  // Milliseconds
	private Point lastMouseLocation;
	
	
	public MyRobot(int delay) throws AWTException {
		robot = new Robot();
		robot.setAutoWaitForIdle(true);
		lastMouseLocation = null;
		this.delay = delay;
	}
	
	public MyImage getScreenshot(Rectangle rect) {
		return new MyImage(robot.createScreenCapture(rect));
	}
	
	public void click(int x, int y, int button) {
		if (lastMouseLocation != null && !MouseInfo.getPointerInfo().getLocation().equals(lastMouseLocation)) {
			System.err.println("Mouse moved. Program aborted");
			System.exit(1);
			return;
		}
		robot.mouseMove(x, y);
		lastMouseLocation = new Point(x, y);
		robot.mousePress(button);
		robot.mouseRelease(button);
		robot.delay(delay);
	}	
}
