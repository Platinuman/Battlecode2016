package team061;

import java.util.*;

import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	protected static MapLocation here; // bot classes are responsible for keeping this up to date
    protected static Random rand;
    protected static int roundToStopHuntingDens = 500;
    protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();

		here = rc.getLocation();
		rand = new Random(rc.getID());
//		System.out.println(here.x + ", " + here.y + " decoded: " +MessageEncode.TURRET_TARGET.decode(here, MessageEncode.TURRET_TARGET.encode(new int[]{69, RobotType.SOLDIER.ordinal(), here.x + 34,here.y - 21}))[3]);
	}
	
}