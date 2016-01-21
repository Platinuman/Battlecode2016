package TurretBot;

import java.util.*;

import battlecode.common.*;

public class BotScout extends Bot {
	static MapLocation alpha;
	static MapLocation dest;
	static int range;

	public static void loop(RobotController theRC) throws GameActionException {

		Bot.init(theRC);
		init();
		while (true) {
			try {
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		rc.setIndicatorString(0, "We see.");
		range = 3;
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
				int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(signals[i].getLocation(),message);
				alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
				break;
			}
		}
	}
	

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		Signal[] signals = rc.emptySignalQueue();
		updateMaxRange(signals);
		if (rc.isCoreReady()) {
			moveToLocFartherThanAlphaIfPossible(here);
		}
		if (rc.isCoreReady()) {
			Util.checkRubbleAndClear(here.directionTo(center));
		}
		broadcastEnemies();
		
	}
	private static void broadcastEnemies() throws GameActionException{
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		Arrays.sort(enemyRobots, new Comparator<RobotInfo>() {
		    public int compare(RobotInfo idx1, RobotInfo idx2) {
		        return (int) (100*(idx1.attackPower-idx2.attackPower) + (-idx1.health+idx2.health));
		    }
		});
		for (int i = Integer.max(0,enemyRobots.length-20); i < enemyRobots.length; i++) {
			MapLocation loc = enemyRobots[i].location;
			int[] message = MessageEncode.TURRET_TARGET.encode(new int[] { loc.x, loc.y });
			rc.broadcastMessageSignal(message[0], message[1],(int)GameConstants.BROADCAST_RANGE_MULTIPLIER*RobotType.SCOUT.sensorRadiusSquared);
		}
	}
	private static void moveToLocFartherThanAlphaIfPossible(MapLocation here) throws GameActionException {
		Direction dir = here.directionTo(center);
		boolean shouldMove = false;
		Direction bestDir = dir;
		int nearestScout = distToNearestScout(here);
		int distanceToAlpha = here.distanceSquaredTo(alpha);
		int bestScore = distanceToAlpha - nearestScout;
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)) {
				int newDistanceToAlpha = newLoc.distanceSquaredTo(alpha);
				if (newDistanceToAlpha <= range && theSafety.isSafeToMoveTo(newLoc)) {
					int newNearestScout = distToNearestScout(newLoc);
					int score = newDistanceToAlpha - newNearestScout;
					if (score > bestScore) {
						bestScore = score;
						bestDir = dir;
						shouldMove = true;
					}
				}
			}
			dir = dir.rotateLeft();
		}
		if (rc.canMove(bestDir) && shouldMove) {
			rc.move(bestDir);
		}
	}
	public static int distToNearestScout(MapLocation loc) throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, RobotType.SCOUT.sensorRadiusSquared, us);
		double nearestScout = 0;
		for (int i = 0; i < nearbyAllies.length; i++) {
			if (nearbyAllies[i].type == RobotType.SCOUT) {
				nearestScout += 1000.0/nearbyAllies[i].location.distanceSquaredTo(loc);
			}
		}
		return (int)nearestScout;
	}
	private static void updateMaxRange(Signal[] signals) {
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
				continue;
			}
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.PROXIMITY_NOTIFICATION) {
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(signals[i].getLocation(),message);
				range = decodedMessage[0] - 1;
				break;
			}
		}
		return;
	}
}