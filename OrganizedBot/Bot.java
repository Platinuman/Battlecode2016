package OrganizedBot;

import java.util.*;
import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	public static MapLocation here; // bot classes are responsible for keeping this up to date
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

	public static boolean updateTurretList(Signal[] signals, RobotInfo[] enemies){
		boolean updated = false;
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.WARN_ABOUT_TURRETS) {
						int[] data = purpose.decode(signal.getLocation(), message);
						for(int i = 0; i< data.length; i +=2){
							if(data[i] == -1) break;
							enemyTurrets.add(new RobotInfo(0, them, RobotType.TURRET, new MapLocation(data[i], data[i+1]),0,0,0,0,0,0,0));
						}
						updated = true;
					} else if(purpose == MessageEncode.ENEMY_TURRET_DEATH){
						int[] data = purpose.decode(signal.getLocation(), message);
						MapLocation deathLoc = new MapLocation(data[0],data[1]);
						removeLocFromTurretArray(deathLoc);
						updated = true;
					}
				}
			}
		}
		return updated;
	}

	public static void removeLocFromTurretArray(MapLocation loc) {
		for(RobotInfo ri : enemyTurrets){
			if( ri.location.equals(loc)){
				enemyTurrets.remove(ri);
				System.out.println("removed turret");
				return;
			}
		}
	}
	
	public static boolean locationInTurretArray(MapLocation loc){
		System.out.println(loc);
		for(RobotInfo ri : enemyTurrets){
			System.out.println(ri.location);
			if( ri.location.equals(loc)){
				System.out.println("found one");
				return true;
			}
		}
		return false;
	}
}