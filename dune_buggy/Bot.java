package dune_buggy;

import java.util.*;
import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static RobotType type;
	public static MapLocation here;
	public static MapLocation center;
	protected static Random rand;
	public static MapLocation[] targetDens;
	public static int targetDenSize;
	public static MapLocation targetLoc;
	public static int bestIndex;
	public static int numDensToHunt;
	public static int turnCreated; 
	public static Direction directionIAmMoving;
	protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	protected static int[] directionOrder = {0,1,-1,2,-2,3,-3,4};
	
	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;
		turnCreated = rc.getRoundNum();
		us = rc.getTeam();
		here = rc.getLocation();
		rand = new Random(rc.getID());
		type = rc.getType();
		targetDens = new MapLocation[10000];
		targetDenSize = bestIndex = numDensToHunt = 0;
		MapAnalysis.analyze();
	}
}
