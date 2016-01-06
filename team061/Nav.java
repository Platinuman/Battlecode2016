package team061;
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
                default:
                    if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) return false;
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
    
    private static void bugMove() throws GameActionException {
    	tryMoveDirect();
    	
    }
public static void goTo(MapLocation theDest, NavSafetyPolicy theSafety) throws GameActionException {
    if (!theDest.equals(dest)) {
        dest = theDest;
        bugState = BugState.DIRECT;
    }

    if (here.equals(dest)) return;
    
    safety = theSafety;
    
    bugMove();
}
}
