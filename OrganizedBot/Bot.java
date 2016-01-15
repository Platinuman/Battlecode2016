package Battlecode2016.OrganizedBot;

import java.util.*;

import java.lang.Math.*;

import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	protected static MapLocation here; // bot classes are responsible for
										// keeping this up to date
	protected static Random rand;
	protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
			Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	protected static RobotType type;

	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;
		us = rc.getTeam();
		them = us.opponent();
		here = rc.getLocation();
		rand = new Random(rc.getID());
		type = rc.getType();
	}
}