package OrganizedBot;

import battlecode.common.*;

interface NavSafetyPolicy {
	public boolean isSafeToMoveTo(MapLocation loc);
}

class SafetyPolicyAvoidAllUnits extends Bot implements NavSafetyPolicy {

	RobotInfo[] nearbyEnemies;

	public SafetyPolicyAvoidAllUnits(RobotInfo[] nearbyEnemies) {
		this.nearbyEnemies = nearbyEnemies;
	}

	public boolean isSafeToMoveTo(MapLocation loc) {
		for (RobotInfo enemy : nearbyEnemies) {
			switch (enemy.type) {
			case ARCHON:
				break;
			case ZOMBIEDEN:
				if (enemy.type.attackRadiusSquared + 2 >= loc.distanceSquaredTo(enemy.location)
				- ((type == RobotType.ARCHON) ? 20 : 0))// hardcoded 
			return false;
		break;
			default:
				if (enemy.type.attackRadiusSquared + ((enemy.type == RobotType.TURRET) ? 10 : 0) >= loc.distanceSquaredTo(enemy.location)
						- ((type == RobotType.ARCHON) ? 20 : 0))// hardcoded 
					return false;
				break;
			}
		}
		return true;
	}
}

public class Nav extends Bot {

	private static MapLocation dest;
	private static NavSafetyPolicy safety;

	private enum BugState {
		DIRECT, BUG
	}

	public enum WallSide {
		LEFT, RIGHT
	}

	private static BugState bugState;
	public static WallSide bugWallSide = WallSide.LEFT;
	private static int bugStartDistSq;
	private static Direction bugLastMoveDir;
	private static Direction bugLookStartDir;
	private static int bugRotationCount;
	private static int bugMovesSinceSeenObstacle = 0;
	private static int bugMovesSinceMadeProgress = 0;
	private static boolean move(Direction dir) throws GameActionException {
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}

	private static boolean checkRubble(int threshold) {
		if (rc.getType() == RobotType.TTM) {
			return false;
		}
		double rubbleCount = Util.rubbleBetweenHereAndThere(here, dest);
		return rubbleCount <= threshold; // hard-coded
	}

	private static boolean canMove(Direction dir) {
		return rc.canMove(dir) && safety.isSafeToMoveTo(here.add(dir));
	}

	private static boolean tryMoveDirect() throws GameActionException {
		Direction toDest = here.directionTo(dest);

		if (canMove(toDest)) {
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[2];
		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		if (here.add(dirLeft).distanceSquaredTo(dest) < here.add(dirRight).distanceSquaredTo(dest)) {
			dirs[0] = dirLeft;
			dirs[1] = dirRight;
		} else {
			dirs[0] = dirRight;
			dirs[1] = dirLeft;
		}
		for (Direction dir : dirs) {
			if (canMove(dir)) {
				move(dir);
				return true;
			}
		}
		return false;
	}

	private static void startBug() throws GameActionException {
		bugStartDistSq = here.distanceSquaredTo(dest);
		bugLastMoveDir = here.directionTo(dest);
		bugLookStartDir = here.directionTo(dest);
		bugRotationCount = 0;
		bugMovesSinceSeenObstacle = 0;
		bugMovesSinceMadeProgress = 0;

		// try to intelligently choose on which side we will keep the wall
		Direction leftTryDir = bugLastMoveDir.rotateLeft();
		for (int i = 0; i < 3; i++) {
			if (!canMove(leftTryDir))
				leftTryDir = leftTryDir.rotateLeft();
			else
				break;
		}
		Direction rightTryDir = bugLastMoveDir.rotateRight();
		for (int i = 0; i < 3; i++) {
			if (!canMove(rightTryDir))
				rightTryDir = rightTryDir.rotateRight();
			else
				break;
		}
		if (dest.distanceSquaredTo(here.add(leftTryDir)) < dest.distanceSquaredTo(here.add(rightTryDir))) {
			bugWallSide = WallSide.RIGHT;
		} else {
			bugWallSide = WallSide.LEFT;
		}
	}

	private static Direction findBugMoveDir() throws GameActionException {
		bugMovesSinceSeenObstacle++;
		Direction dir = bugLookStartDir;
		for (int i = 8; i-- > 0;) {
			if (canMove(dir))
				return dir;
			dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir.rotateLeft());
			bugMovesSinceSeenObstacle = 0;
		}
		return null;
	}

	private static int numRightRotations(Direction start, Direction end) {
		return (end.ordinal() - start.ordinal() + 8) % 8;
	}

	private static int numLeftRotations(Direction start, Direction end) {
		return (-end.ordinal() + start.ordinal() + 8) % 8;
	}

	private static int calculateBugRotation(Direction moveDir) {
		if (bugWallSide == WallSide.LEFT) {
			return numRightRotations(bugLookStartDir, moveDir) - numRightRotations(bugLookStartDir, bugLastMoveDir);
		} else {
			return numLeftRotations(bugLookStartDir, moveDir) - numLeftRotations(bugLookStartDir, bugLastMoveDir);
		}
	}

	private static void bugMove(Direction dir) throws GameActionException {
		if (move(dir)) {
			bugRotationCount += calculateBugRotation(dir);
			bugLastMoveDir = dir;
			if (bugWallSide == WallSide.LEFT)
				bugLookStartDir = dir.rotateLeft().rotateLeft();
			else
				bugLookStartDir = dir.rotateRight().rotateRight();
		}
	}

	private static boolean detectBugIntoEdge() throws GameActionException {
		if (bugWallSide == WallSide.LEFT) {
			return !rc.onTheMap(here.add(bugLastMoveDir.rotateLeft()));
		} else {
			return !rc.onTheMap(here.add(bugLastMoveDir.rotateRight()));
		}
	}

	private static void reverseBugWallFollowDir() throws GameActionException {
		bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
		startBug();
	}

	private static void bugTurn() throws GameActionException {
		if (detectBugIntoEdge()) {
			reverseBugWallFollowDir();
		}
		Direction dir = findBugMoveDir();
		if (dir != null) {
			bugMove(dir);
		}
	}

	private static boolean canEndBug() {
		if (bugMovesSinceSeenObstacle >= 4)
			return true;
		return (bugRotationCount <= 0 || bugRotationCount >= 8) && here.distanceSquaredTo(dest) <= bugStartDistSq;
	}

	private static void bugMove() throws GameActionException {
		// Debug.clear("nav");
		// Debug.indicate("nav", 0, "bugMovesSinceSeenObstacle = " +
		// bugMovesSinceSeenObstacle + "; bugRotatoinCount = " +
		// bugRotationCount);
		// Check if we can stop bugging at the *beginning* of the turn
//		rc.setIndicatorString(2, "I've been bugging for " +bugMovesSinceMadeProgress+ "turns.");
//		rc.setIndicatorString(1, "bugMovesSinceSeenObstacle = " +
//				 bugMovesSinceSeenObstacle + "; bugRotatoinCount = " +
//				 bugRotationCount);
		if (bugState == BugState.BUG) {
			if (canEndBug()) {
				// Debug.indicateAppend("nav", 1, "ending bug; ");
				bugState = BugState.DIRECT;
				bugMovesSinceMadeProgress = 0;
			}
		}

		// If DIRECT mode, try to go directly to target

		if (bugState == BugState.DIRECT) {
			if (!tryMoveDirect()) {
				// Debug.indicateAppend("nav", 1, "starting to bug; ");
				if (type != RobotType.SCOUT && !rc.isLocationOccupied(here.add(here.directionTo(dest)))&&checkRubble(2000)) {
					rc.clearRubble(here.directionTo(dest));
				} else {
					bugState = BugState.BUG;
					startBug();
				}
			}
			// checkRubbleAndClear(here.directionTo(dest));
			// Debug.indicateAppend("nav", 1, "successful direct move; ");
		}
		if (rc.isCoreReady()) {
			if (here.distanceSquaredTo(dest) < type.attackRadiusSquared) {
				if (rc.senseRubble(here.add(here.directionTo(dest))) > 0) {
					rc.clearRubble(here.directionTo(dest));
				}
				return;
			} else if (bugState == BugState.BUG && bugMovesSinceMadeProgress > 20) {
				if (rc.senseRubble(here.add(here.directionTo(dest))) > 0) {
					rc.clearRubble(here.directionTo(dest));

					return;
				}
			}

		}
		// If that failed, or if bugging, bug
		
		if (bugState == BugState.BUG) {
			// Debug.indicateAppend("nav", 1, "bugging; ");
			bugTurn();
			bugMovesSinceMadeProgress++;
		}
	}

	// private static void runAway() throws GameActionException {
	// Direction away =
	// here.directionTo(Util.centroidOfUnits(rc.senseHostileRobots(here, -1)));
	// if (rc.canMove(away)) {
	// rc.move(away);
	// } else if (rc.canMove(away.rotateLeft())) {
	// rc.move(away.rotateLeft());
	// } else if (rc.canMove(away.rotateRight())) {
	// rc.move(away.rotateRight());
	// }
	// }

	public static void goTo(MapLocation theDest, NavSafetyPolicy theSafety) throws GameActionException {
		if (!theDest.equals(dest)) {
			dest = theDest;
			bugState = BugState.DIRECT;
		}

		if (here.equals(dest))
			return;

		safety = theSafety;

		bugMove();
		// if (false && type==RobotType.ARCHON && rc.isCoreReady()) {
		// runAway();
		// }
	}

	private static boolean tryMoveDirectScout(Direction toDest) throws GameActionException {
		// Direction toDest = here.directionTo(dest);

		if (canMove(toDest) && rc.onTheMap(here.add(toDest, (int) (Math.sqrt(type.sensorRadiusSquared / 2.0))))) {
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[2];
		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		dirs[0] = dirLeft;
		dirs[1] = dirRight;
		//dirs[2] = dirLeft.rotateLeft();
		//dirs[3] = dirRight.rotateRight();
		for (Direction dir : dirs) {
			if (canMove(dir)
					&& rc.onTheMap(here.add(dir,(int) (Math.sqrt(type.sensorRadiusSquared / 2.0))))) {
				move(dir);
				return true;
			}
		}
		return false;
	}

	public static void flee(RobotInfo[] unfriendly) throws GameActionException {
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(unfriendly, here);
		double bestDistSq = -10000;
		for (Direction dir : Direction.values()) {
			if (!rc.canMove(dir))
				continue;
			MapLocation retreatLoc = here.add(dir);
			if(isInRangeOfTurrets(retreatLoc))
				continue;
			RobotInfo closestEnemy = Util.closest(unfriendly, retreatLoc);
			int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
			double rubble = rc.senseRubble(retreatLoc);
			double rubbleMod = rubble<GameConstants.RUBBLE_SLOW_THRESH?0:rubble*2.5/GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			if (distSq-rubbleMod > bestDistSq) {
				bestDistSq = distSq-rubbleMod;
				bestRetreatDir = dir;
			}
		}
		if (bestRetreatDir != null && rc.isCoreReady() && rc.canMove(bestRetreatDir)) {
			rc.move(bestRetreatDir);
		}
	}

	// public static void explore() throws GameActionException { // NEW INTO
	// HARASS
	// // explore
	// RobotInfo[] hostileRobots = rc.senseHostileRobots(here,
	// type.sensorRadiusSquared);
	// RobotInfo[] allies = rc.senseNearbyRobots(here, type.sensorRadiusSquared,
	// us);
	// NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(
	// Util.combineTwoRIArrays(enemyTurrets, turretSize, hostileRobots));
	// if (rc.isCoreReady()) {
	// if (type != RobotType.SCOUT && allies.length > 1) {
	// directionIAmMoving = here.directionTo(Util.centroidOfUnits(allies));
	// }
	// if (directionIAmMoving == null) {
	// int fate = rand.nextInt(1000);
	// directionIAmMoving = Direction.values()[fate % 8];
	// }
	// boolean moved = Nav.moveInDir(directionIAmMoving, theSafety);
	// if (!moved) {
	// for (int i = 0; i < 8; i++) {
	// directionIAmMoving = directionIAmMoving.rotateRight();
	// moved = Nav.moveInDir(directionIAmMoving, theSafety);
	// if (moved) {
	// break;
	// }
	// }
	// }
	// if (!moved && hostileRobots.length > 0) {
	// flee(hostileRobots);
	// // Combat.retreat(Util.closest(hostileRobots, here).location);
	// }
	// }
	// }

	public static void explore(RobotInfo[] hostileRobots, RobotInfo[] allies) throws GameActionException {
		// explore
		safety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets, turretSize, hostileRobots));
		RobotInfo[] scouts = Util.getUnitsOfType(allies, RobotType.SCOUT);
		if (directionIAmMoving == null) {
			directionIAmMoving = center.directionTo(here);
		} else if(scouts.length > 0){
			Direction tempDirection = Util.centroidOfUnits(scouts).directionTo(here);
			directionIAmMoving = (new Direction[] {
					tempDirection.rotateLeft(),
					tempDirection,
					tempDirection.rotateRight() })[rand.nextInt(3)];
		}
		//		} else if(hostileRobots.length > 0 && directionIAmMoving == here.directionTo(Util.centroidOfUnits(hostileRobots)))
		//			directionIAmMoving = directionIAmMoving.rotateRight();
		if(tryMoveDirectScout(directionIAmMoving))return;
		else
			directionIAmMoving = (new Direction[] {
					directionIAmMoving.opposite().rotateLeft(),
					directionIAmMoving.opposite().rotateRight() })[rand.nextInt(2)];
		if( tryMoveDirectScout(directionIAmMoving)) return;
		if(hostileRobots.length > 0)
			flee(hostileRobots);
		// Combat.retreat(Util.closest(hostileRobots, here).location);
	}

	public static void goAwayFrom(MapLocation loc, NavSafetyPolicy theSafety) throws GameActionException{
		int farthestDist = -1;
		MapLocation bestLoc = null;
		for(Direction dir: Direction.values()){
			if(!rc.canMove(dir))
				continue;
			MapLocation newLoc = here.add(dir);
			if(loc.distanceSquaredTo(newLoc) > farthestDist){
				bestLoc = newLoc;
				farthestDist = loc.distanceSquaredTo(newLoc);
			}
		}
		if(bestLoc != null)
			Nav.goTo(bestLoc, theSafety);
	}
}