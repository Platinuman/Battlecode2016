package OrganizedBot;

import java.util.*;
import battlecode.common.*;

public class Bot {
	public static RobotController rc;
	protected static Team us;
	protected static Team them;
	protected static RobotType type;
	public static MapLocation here; // bot classes are responsible for keeping this up to date
	public static MapLocation center;
	protected static Random rand;
	public static MapLocation[] targetDens;
	public static int targetDenSize;
	public static MapLocation[] killedDens;
	public static int killedDenSize;
	public static MapLocation targetLoc;
	public static int bestIndex;
	public static int numDensToHunt;
	public static Direction directionIAmMoving;
	// TODO: get rid of this stupid directions thing and use direction order and dir.ordinal()
	protected static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
			Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
	protected static int[] directionOrder = {0,1,-1,2,-2,3,-3,4};
	public static RobotInfo[] enemyTurrets;
	public static int turretSize;
	
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
		MapAnalysis.analyze();
		enemyTurrets = new RobotInfo[64];
		turretSize = 0;
	}

//---- Enemy turret tracking methods below ----
	/*
	 * Keeps known turret location list up to date based only on message signals (from scouts).
	 */
	public static boolean updateTurretList(Signal[] signals) throws GameActionException{
		boolean updated = false;
		for (Signal signal : signals) {
			if (signal.getTeam() == us) {
				int[] message = signal.getMessage();
				if (message != null) {
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.WARN_ABOUT_TURRETS || purpose == MessageEncode.RELAY_TURRET_INFO) {
						MapLocation senderloc = signal.getLocation();
						int[] data = purpose.decode(senderloc, message);
						MapLocation loc;
						for(int i = 0; i< data.length; i +=2){
							if(data[i] == senderloc.x) break;
							loc = new MapLocation(data[i], data[i+1]);
							if(!isLocationInTurretArray(loc)){
								enemyTurrets[turretSize]= new RobotInfo(0, them, RobotType.TURRET, loc,0,0,0,0,0,0,0);
								turretSize++;
								//System.out.println("added turret @ " + data[i] + ", " + data[i+1]);
							}
						}
						updated = true;
					} else if(purpose == MessageEncode.ENEMY_TURRET_DEATH){
						int[] data = purpose.decode(signal.getLocation(), message);
						removeLocFromTurretArray(new MapLocation(data[0],data[1]));
						updated = true;
					}
				}
			}
		}
		return updated;
	}
	public static void removeLocFromTurretArray(MapLocation loc) {
		for(int i = 0 ; i < turretSize; i++){
			if( enemyTurrets[i].location.equals(loc)){
				Util.removeIndexFromArray(enemyTurrets, i, turretSize);
				turretSize--;
				return;
			}
		}
	}
	public static boolean isLocationInTurretArray(MapLocation loc){
		for(int i = 0 ; i < turretSize; i++){
			if( enemyTurrets[i].location.equals(loc)){
				return true;
			}
		}
		return false;
	}
	public static int numTurretsInRangeSquared(int range){
		int count = 0;
		for(int i = 0; i < turretSize ; i++){
			if(enemyTurrets[i].location.distanceSquaredTo(here) <= range)
				count++;
		}
		return count;
	}
}