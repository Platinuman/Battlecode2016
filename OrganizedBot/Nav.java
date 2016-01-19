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
			default:
				if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location))
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

	private static boolean move(Direction dir) throws GameActionException {
		rc.move(dir);
		return true;
	}

	private static boolean checkRubble(Direction dir) {
		if (rc.getType() == RobotType.TTM){
			return false;
		}
		double rubbleCount = rc.senseRubble(rc.getLocation().add(dir));
		return rubbleCount >= GameConstants.RUBBLE_OBSTRUCTION_THRESH && rubbleCount <= 1000; // hard-coded
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
		if (bugState == BugState.BUG) {
			if (canEndBug()) {
				// Debug.indicateAppend("nav", 1, "ending bug; ");
				bugState = BugState.DIRECT;
			}
		}

		// If DIRECT mode, try to go directly to target

		if (bugState == BugState.DIRECT) {
			if (!tryMoveDirect()) {
				// Debug.indicateAppend("nav", 1, "starting to bug; ");
				if (checkRubble(here.directionTo(dest))) {
					rc.clearRubble(here.directionTo(dest));
				} else {
					bugState = BugState.BUG;
					startBug();
				}
			}
			// checkRubbleAndClear(here.directionTo(dest));
			// Debug.indicateAppend("nav", 1, "successful direct move; ");
		}

		// If that failed, or if bugging, bug
		if (bugState == BugState.BUG) {
			// Debug.indicateAppend("nav", 1, "bugging; ");
			bugTurn();
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

	public static boolean moveInDir(Direction dir, NavSafetyPolicy theSafety) throws GameActionException {
		safety = theSafety;
		dest = here.add(dir);
		return tryMoveDirect();
	}

	public static void flee(RobotInfo[] unfriendly) throws GameActionException {
		MapLocation center = Util.centroidOfUnits(unfriendly);
		Direction away = center.directionTo(here);
		if (rc.canMove(away) && rc.onTheMap(here.add(away, 4))) {
			rc.move(away);
		} else {
			Direction dirLeft = away.rotateLeft();
			Direction dirRight = away.rotateRight();
			for (int i = 0; i < 3; i++) {

				if (rc.canMove(dirLeft) && rc.onTheMap(here.add(dirLeft, 4))) {
					rc.move(dirLeft);
					break;
				} else if (rc.canMove(dirRight) && rc.onTheMap(here.add(dirRight, 4))) {
					rc.move(dirRight);
					break;
				}
				dirLeft = dirLeft.rotateLeft();
				dirRight = dirRight.rotateRight();
			}
		}
		if (rc.isCoreReady()) { // oh shit we trapped
			Direction dirLeft = away.rotateLeft();
			Direction dirRight = away.rotateRight();
			for (int i = 0; i < 3; i++) {

				if (rc.canMove(dirLeft)) {
					rc.move(dirLeft);
					break;
				} else if (rc.canMove(dirRight)) {
					rc.move(dirRight);
					break;
				}
				dirLeft = dirLeft.rotateLeft();
				dirRight = dirRight.rotateRight();
			}
		}
		if (rc.isCoreReady()) {// last hope
			if (checkRubble(away)) {
				rc.clearRubble(away);
			}
		}
		// time to die... gg
	}

	public static void explore() throws GameActionException { // NEW INTO HARASS
		// explore
		RobotInfo[] hostileRobots = rc.senseHostileRobots(here, type.sensorRadiusSquared);
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(hostileRobots);
		if (rc.isCoreReady()) {
			if (directionIAmMoving == null) {
				int fate = rand.nextInt(1000);
				directionIAmMoving = Direction.values()[fate % 8];
			}
			boolean moved = Nav.moveInDir(directionIAmMoving, theSafety);
			if (!moved) {
				for (int i = 0; i < 8; i++) {
					directionIAmMoving = directionIAmMoving.rotateRight();
					boolean movedNow = Nav.moveInDir(directionIAmMoving, theSafety);
					if (movedNow) {
						moved = true;
						break;
					}
				}
			}
			if (!moved && hostileRobots.length > 0) {
				flee(hostileRobots);
				// Combat.retreat(Util.closest(hostileRobots, here).location);
			}
		}
	}
	public static void explore(RobotInfo[] hostileRobots) throws GameActionException { // NEW INTO HARASS
		// explore
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(hostileRobots);
		if (rc.isCoreReady()) {
			if (directionIAmMoving == null) {
				int fate = rand.nextInt(1000);
				directionIAmMoving = Direction.values()[fate % 8];
			}
			boolean moved = Nav.moveInDir(directionIAmMoving, theSafety);
			if (!moved) {
				for (int i = 0; i < 8; i++) {
					directionIAmMoving = directionIAmMoving.rotateRight();
					boolean movedNow = Nav.moveInDir(directionIAmMoving, theSafety);
					if (movedNow) {
						moved = true;
						break;
					}
				}
			}
			if (!moved && hostileRobots.length > 0) {
				flee(hostileRobots);
				// Combat.retreat(Util.closest(hostileRobots, here).location);
			}
		}
	}
}
