package OrganizedBot;

import java.util.*;

import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	protected static MapLocation here; // bot classes are responsible for keeping this up to date
	public static MapLocation center;
    protected static Random rand;
    public static MapLocation[] targetDens;
    public static int targetDenSize;
    public static int bestIndex;
    public static int numDensToHunt;
    protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	protected static RobotType type;
	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();

		here = rc.getLocation();
		rand = new Random(rc.getID());
		type = rc.getType();
		targetDens = new MapLocation[10000];
		targetDenSize = bestIndex = numDensToHunt = 0;
		MapAnalysis.analyze();
	}
}