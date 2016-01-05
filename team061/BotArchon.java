package team061;

import java.util.Random;

import battlecode.common.*;

public class BotArchon extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
//        Debug.init("micro");

        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    private static void turn() throws GameActionException {
    	Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
                Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
        RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
                RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
        here = rc.getLocation();
        Random rand = new Random(rc.getID());
        int fate = rand.nextInt(1000);
        // Check if this ARCHON's core is ready
        if (fate % 10 == 2) {
            // Send a message signal containing the data (6370, 6147)
            rc.broadcastMessageSignal(6370, 6147, 80);
        }
        Signal[] signals = rc.emptySignalQueue();
        if (signals.length > 0) {
            // Set an indicator string that can be viewed in the client
            rc.setIndicatorString(0, "I received a signal this turn!");
        } else {
            rc.setIndicatorString(0, "I don't any signal buddies");
        }
        if (rc.isCoreReady()) {
            if (fate < 800) {
                // Choose a random direction to try to move in
                Direction dirToMove = directions[fate % 8];
                // Check the rubble in that direction
                if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                    // Too much rubble, so I should clear it
                    rc.clearRubble(dirToMove);
                    // Check if I can move in this direction
                } else if (rc.canMove(dirToMove)) {
                    // Move
                    rc.move(dirToMove);
                }
            } else {
                // Choose a random unit to build
                RobotType typeToBuild = robotTypes[fate % 8];
                // Check for sufficient parts
                if (rc.hasBuildRequirements(typeToBuild)) {
                    // Choose a random direction to try to build in
                    Direction dirToBuild = directions[rand.nextInt(8)];
                    for (int i = 0; i < 8; i++) {
                        // If possible, build in this direction
                        if (rc.canBuild(dirToBuild, typeToBuild)) {
                            rc.build(dirToBuild, typeToBuild);
                            break;
                        } else {
                            // Rotate the direction to try
                            dirToBuild = dirToBuild.rotateLeft();
                        }
                    }
                }
            }
        }
    }
}