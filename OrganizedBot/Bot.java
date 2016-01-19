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
	public static MapLocation[] killedDens;
	public static MapLocation targetLoc;
	public static int killedDenSize;
	public static int targetDenSize;
	public static int bestIndex;
	public static int numDensToHunt;
	public static Direction directionIAmMoving;
	protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };

	protected static RobotType type;

	public static ArrayList<RobotInfo> enemyTurrets;

//	public static void loop(RobotController theRC) throws GameActionException {
//		init(theRC);
//		init();
//		while (true) {
//			try {
//				turn();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			Clock.yield();
//		}
//	}

//	public static void init() throws GameActionException{ return; }
//	public static void turn() throws GameActionException{ return; }
	protected static void init(RobotController theRC) throws GameActionException {
		rc = theRC;

		us = rc.getTeam();
		them = us.opponent();

		here = rc.getLocation();
		rand = new Random(rc.getID());
		type = rc.getType();
		targetDens = new MapLocation[10000];
		killedDens = new MapLocation[10000];
		targetDenSize = bestIndex = numDensToHunt = killedDenSize = 0;
		enemyTurrets = new ArrayList<RobotInfo>();
		MapAnalysis.analyze();
	}

	public static void updateTurretList(Signal[] signals){
//		for (Signal signal : signals) {
//			if (signal.getTeam() == us) {
//				int[] message = signal.getMessage();
//				if (message != null) {
//					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
//					if (purpose == MessageEncode.WARN_ABOUT_TURRETS) {
//						int[] data = purpose.decode(signal.getLocation(), message);
//						turretLoc = new MapLocation(data[0], data[1]);
//						rc.setIndicatorString(2, "loc we looked at" + turretLoc);
//					}
//				}
//			}
//		}
	}
}