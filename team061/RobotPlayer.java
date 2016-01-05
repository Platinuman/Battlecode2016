package team061;

import battlecode.common.*;

public class RobotPlayer {
    public static void run(RobotController theRC) throws Exception {

        // I think putting the missile check out in front prevents us from loading any
        // extraneous classes and invoking their static initializers, saving some crucial
        // bytecodes on the missile's first turn.

        switch (theRC.getType()) {
            case ARCHON:
                BotArchon.loop(theRC);
                break;

            case GUARD:
                BotGuard.loop(theRC);
                break;

            case SCOUT:
                BotScout.loop(theRC);
                break;

            case TURRET:
                BotTurret.loop(theRC);
                break;

            case SOLDIER:
                BotSoldier.loop(theRC);
                break;

            case VIPER:
                BotViper.loop(theRC);
                break;
/*
            case COMMANDER:
                BotCommander.loop(theRC);
                break;

            case DRONE:
                BotDrone.loop(theRC);
                break;

            case LAUNCHER:
                BotLauncher.loop(theRC);
                break;

            case AEROSPACELAB:
            case BARRACKS:
            case HELIPAD:
            case MINERFACTORY:
            case TANKFACTORY:
            case TECHNOLOGYINSTITUTE:
            case TRAININGFIELD:
                BotGenericProducer.loop(theRC);
                break;

            case HANDWASHSTATION:
            case SUPPLYDEPOT:
            case COMPUTER:
            case BASHER:
                BotDoNothing.loop(theRC);
                break;
*/
            default:
                throw new Exception("Unknown robot type!!! :(");
        }
    }
}