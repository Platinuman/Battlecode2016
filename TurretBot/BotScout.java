package TurretBot;

import java.util.*;

import battlecode.common.*;

public class BotScout extends Bot {
	static MapLocation alpha;
	static MapLocation dest;
	static int range;
	static boolean firstTurn = true;

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
				int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(message);
				alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
				break;
			}
		}
	}
	private static boolean checkRubbleAndClear(Direction dir)throws GameActionException {
		if (rc.senseRubble(rc.getLocation().add(dir)) > 0) {
				rc.clearRubble(dir);
			return true;
		}
		return false;
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		if (rc.isCoreReady()) {
			Direction dirToClear = Direction.NORTH;
			for (int i = 0; i < 8; i++) {
				if (checkRubbleAndClear(dirToClear)) {
					break;
				}
				dirToClear = dirToClear.rotateRight();
			}
		}
		Signal[] signals = rc.emptySignalQueue();
		updateMaxRange(signals);
		if (rc.isCoreReady()) {
			moveToLocFartherThanAlphaIfPossible(here);
		}
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		Arrays.sort(enemyRobots, new Comparator<RobotInfo>() {
		    public int compare(RobotInfo idx1, RobotInfo idx2) {
		        return (int) (idx1.attackPower-idx2.attackPower);
		    }
		});
		for (int i = 0; i < enemyRobots.length; i++) {
			if (i == 20) {
				break;
			}
			MapLocation loc = enemyRobots[i].location;
			double health = enemyRobots[i].health;
			RobotType type = enemyRobots[i].type;
			int[] message = MessageEncode.TURRET_TARGET
					.encode(new int[] { (int) (health), type.ordinal(), loc.x, loc.y });
			rc.broadcastMessageSignal(message[0], message[1],
					//(int) (RobotType.SCOUT.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
					15);
		}
	}

	private static void moveToLocFartherThanAlphaIfPossible(MapLocation here) throws GameActionException {
		Direction dir = directions[rand.nextInt(8)];
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
				int newNearestScout = distToNearestScout(newLoc);
				if (newDistanceToAlpha <= range && theSafety.isSafeToMoveTo(newLoc)) {
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
		//rc.setIndicatorString(1, "Currently at: " + (distanceToAlpha - nearestScout) + " Could be at: " + bestScore);
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
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(message);
				range = decodedMessage[0] - 1;
				break;
			}
		}
		return;
	}
}