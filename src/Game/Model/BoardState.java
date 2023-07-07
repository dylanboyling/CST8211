package Game.Model;

import Game.Model.Enums.GridSquareStatus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ThreadLocalRandom;

import static Game.Util.Constants.DEFAULT_GRID_DIMENSION;

/**
 * This class tracks the state of a grid for a Battleship player, validates guesses, and also tracks what boats have or
 * can be placed on the board.
 */
@SuppressWarnings("deprecation")
public class BoardState extends Observable {

    /**
     * Dimension of one side of the square grid, e.g. gridDimension x gridDimension
     */
    private int gridDimension;

    /**
     * 2D grid containing GridSquares. If an element is null, there is no ship there and
     * if it exists it will contain a GridSquare with information about the ship
     */
    private GridSquare[][] grid;

    /**
     * True if board belongs to player, false otherwise
     */
    private final boolean isPlayer;

    /**
     * Stores current boats that can be placed during the design phase. Note: This is a last minute solution to display
     * how many boats can still be placed to the user. It is by no means an ideal solution but it works.
     */
    private List<DesignBoat> designBoats;

    /**
     * List of boat sizes that the player can place on the ship
     */
    private List<Integer> boatSizeOptions;

    /**
     * Constructs a fresh state of the Battleship game and initializes an empty grid
     *
     * @param isPlayer True if board belongs to player, false otherwise
     */
    public BoardState(boolean isPlayer) {
        this.isPlayer = isPlayer;
        this.resizeGrid(DEFAULT_GRID_DIMENSION);
    }

    /**
     * Resets the grid to its default dimension and re-initializes the state
     */
    public void reset() {
        gridDimension = DEFAULT_GRID_DIMENSION;
        grid = new GridSquare[DEFAULT_GRID_DIMENSION][DEFAULT_GRID_DIMENSION];
        notifyObservers();
    }

    /**
     * Clears the board of all ships
     */
    public void clearBoard() {
        grid = new GridSquare[gridDimension][gridDimension];
        if(isPlayer)
            populateDesignShips();
        notifyObservers();
    }

    /**
     * Validates a guess on the game board by checking if the grid contains a ship or not
     *
     * @param x X coordinate of the guess on the grid
     * @param y Y coordinate of the guess on the grid
     */
    public boolean validateGuess(final int x, final int y) {
        boolean isCorrect = false;
        if (grid[y][x] != null && grid[y][x].isAlive() == GridSquareStatus.ALIVE) {
            grid[y][x].setAlive(GridSquareStatus.HIT);
            isCorrect = true;
        } else {
            grid[y][x] = new GridSquare(0, isPlayer, GridSquareStatus.MISSED);
        }

        notifyObservers();
        return isCorrect;
    }

    /**
     * Places a boat of a given ship size at the provided coordinates
     * @param row Row that the boat begins
     * @param column Column that the boat begins
     * @param boatSize Size of the boat to be created
     * @param isHorizontal True if the boat is horizontal, false if it is vertical
     * @return True if the boat has been placed, false the location is invalid and it cannot be placed
     */
    public boolean placeShipOnBoard(final int row, final int column, final int boatSize, final boolean isHorizontal) {
        boolean isSuccessful = false;
        if (isLocationValid(row, column, boatSize, isHorizontal)) {
            createBoat(row, column, boatSize, isHorizontal);
            for (Iterator<DesignBoat> iterator = designBoats.iterator(); iterator.hasNext();) {
                DesignBoat boat = iterator.next();
                if(boat.getBoatSize() == boatSize) {
                    iterator.remove();
                    break;
                }
            }
            notifyObservers();
            isSuccessful = true;
        }
        return isSuccessful;
    }

    /**
     * Places a varying number of ships in random locations on the board. Number of ships placed changes on grid size.
     */
    public void randomizeShipLocations() {
        grid = new GridSquare[gridDimension][gridDimension];
        final int DIM = gridDimension / 2;
        for (int boatSize = DIM; boatSize > 0; boatSize--) {
            int debug_numberOfBoats = -1;
            for (int numberOfBoats = 1; numberOfBoats <= DIM - boatSize + 1; numberOfBoats++) {
                createRandomBoat(boatSize);
                debug_numberOfBoats = numberOfBoats;
            }
            System.out.printf("[DEBUG] %d ships of size %d were created%n", debug_numberOfBoats, boatSize);
        }

        notifyObservers();
    }

    /**
     * Repopulates the list of ships that can be placed on the board
     */
    private void populateDesignShips(){
        designBoats = new ArrayList<>();
        final int DIM = gridDimension / 2;
        for (int boatSize = DIM; boatSize > 0; boatSize--) {
            int debug_numberOfBoats = -1;
            for (int numberOfBoats = 1; numberOfBoats <= DIM - boatSize + 1; numberOfBoats++) {
                designBoats.add(new DesignBoat(boatSize));
                debug_numberOfBoats = numberOfBoats;
            }
            System.out.printf("[DEBUG] %d boats of size %d can be placed by the player%n", debug_numberOfBoats, boatSize);
        }
        populateBoatSizeOptions();
    }

    /**
     * Checks how many boats of size N are remaining and can still be placed on the board
     * @param boatSize Size of boat to be checked
     * @return Number of boats of boatSize remaining
     */
    public int numberOfBoatSizesRemaining(final int boatSize){
        int numberOfBoats = 0;
        for(DesignBoat boat : designBoats){
            if(boat.getBoatSize() == boatSize)
                numberOfBoats++;
        }

        return numberOfBoats;
    }

    /**
     * Populates the list of unique boat sizes that may be placed on the board
     */
    public void populateBoatSizeOptions(){
        int boatSize = -1;
        boatSizeOptions = new ArrayList<>();
        for(DesignBoat boat : designBoats){
            if(boatSize != boat.getBoatSize()) {
                boatSize = boat.getBoatSize();
                boatSizeOptions.add(boatSize);
            }
        }
    }

    /**
     * Returns list of unique boat sizes that may be placed on the board
     * @return List of unique  boat sizes that may be placed on the board
     */
    public List<Integer> getBoatSizeOptions(){
        return boatSizeOptions;
    }

    /**
     * Checks if there are any boats that can still be placed on the game board
     * @return True if all boats have been placed, false if there are some remaining
     */
    public boolean isDesignBoatsEmpty(){
        return designBoats.isEmpty();
    }

    /**
     * Checks if board belongs to player or not
     *
     * @return True if board state belongs to player, false otherwise
     */
    public boolean isPlayer() {
        return isPlayer;
    }

    /**
     * Gets the dimension of one side of the game board
     *
     * @return Dimension of one side of the game board
     */
    public int getGridDimension() {
        return gridDimension;
    }

    /**
     * Resizes the grid to a new dimension, resets the state of the grid, and repopulates design boat list
     *
     * @param newGridDimension New dimension of the board
     */
    public void resizeGrid(final int newGridDimension) {
        gridDimension = newGridDimension;
        System.out.printf("[DEBUG] New grid dimension is %dx%d%n", gridDimension, gridDimension);
        grid = new GridSquare[gridDimension][gridDimension];

        if(isPlayer)
            populateDesignShips();

        notifyObservers();
    }

    /**
     * Returns the grid containing the current state of the game
     *
     * @return Grid of GridSquares containing info about ships
     */
    public GridSquare[][] getGrid() {
        return this.grid;
    }

    /**
     * Marks itself as changed and notifies any observers. Overridden to bundle setChanged() with notify observers
     */
    @Override
    public void notifyObservers() {
        setChanged();
        super.notifyObservers();
    }

    /**
     * Creates one random boat of a given size on the grid
     *
     * @param boatSize Size of the boat to be created
     */
    private void createRandomBoat(final int boatSize) {
        final boolean HORIZONTAL = true;
        final boolean VERTICAL = false;
        boolean boatCreated = false;

        while (!boatCreated) {
            final int randomRow = ThreadLocalRandom.current().nextInt(0, gridDimension);
            final int randomCol = ThreadLocalRandom.current().nextInt(0, gridDimension);

            if (isLocationValid(randomRow, randomCol, boatSize, HORIZONTAL)) {
                createBoat(randomRow, randomCol, boatSize, HORIZONTAL);
                boatCreated = true;
            } else if (isLocationValid(randomRow, randomCol, boatSize, VERTICAL)) {
                createBoat(randomRow, randomCol, boatSize, VERTICAL);
                boatCreated = true;
            }
        }
    }

    /**
     * Verifies if the (row, column) location of a proposed ship location is valid
     *
     * @param row          Row of proposed ship location
     * @param column       Column of proposed ship location
     * @param boatSize     Ship size
     * @param isHorizontal True if ship is horizontal (left to right), false if vertical (top to bottom)
     * @return true if ship can be placed, false otherwise
     */
    private boolean isLocationValid(final int row, final int column, final int boatSize, final boolean isHorizontal) {
        if (isHorizontal) {
            if (column + boatSize > gridDimension) {
                return false;
            }

            for (int i = column; i < column + boatSize; i++) {
                if (grid[row][i] != null) {
                    return false;
                }
            }
        } else {
            if (row + boatSize > gridDimension) {
                return false;
            }

            for (int i = row; i < row + boatSize; i++) {
                if (grid[i][column] != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Creates a boat square at the location (row, column)
     *
     * @param row          Row of proposed ship location
     * @param column       Column of proposed ship location
     * @param boatSize     Ship size being created
     * @param isHorizontal True if ship is horizontal (left to right), false if vertical (top to bottom)
     */
    private void createBoat(final int row, final int column, final int boatSize, final boolean isHorizontal) {
        if (isHorizontal) {
            for (int i = 0; i < boatSize; i++) {
                grid[row][column + i] = new GridSquare(boatSize, isPlayer, GridSquareStatus.ALIVE);
            }
        } else {
            for (int i = 0; i < boatSize; i++) {
                grid[row + i][column] = new GridSquare(boatSize, isPlayer, GridSquareStatus.ALIVE);
            }
        }
        System.out.printf("[DEBUG] %s %s boat of size %d was created at (%dx%d)%n", isPlayer ? "Player" : "System"
                , isHorizontal ? "Horizontal" : "Vertical", boatSize, row + 1, column + 1);
    }
}
