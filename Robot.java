/**
 * Robot Interface:
 * Your code must implement this simple interface in order to be
 * useable by the simulator.
 */
public interface Robot
{
     /** Represents skill point allocation of Robot.*/
     public class Robot_Specs implements Clone
     {
          /**Attack skill.*/
          public int attack;

          /**Defense skill.*/
          public int defense;

          /**Power skill.*/
          public int power;

          /**Charge skill.*/
          public int charge;
     }

     /**
      * Represents current status of Robot, passed to Robot.act() method
      * to let robot know its current health, charge, and capsules.
      */
     public class Robot_Status implements Clone
     {
          /**current charge.*/
          public int charge;

          /**current health.*/
          public int health;

          /**
           * Current number and power of capsules.<br>
           * Each capsule is represented by an element of the array.<br>
           * The value of the element of the array is equal to the power
           * of the capsule.<br>
           * In-progress capsules are not represented;
           * use getInvestedBuildPower() for that.
           */
          public int[] capsules;

          /**Overrides clone: see Java language documentation for explanation*/
          public Object clone()
               {
                    Robot_Status to_return = (Robot_Status)(super.clone());
                    to_return.capsules = capsules.clone();
                    return to_return;
               }
     };

     /**Represents object located in particular GridCell.*/
     public enum GridObject { EMPTY, SELF, ALLY, ENEMY, WALL, FORT, CAPSULE };

     /**Represents orientation in simulator's world.*/
     public enum Direction { UP, DOWN, LEFT, RIGHT };

     /**Represents cell in grid of simulator's world.*/
     public class GridCell implements Cloneable
     {
          /**X-coordinate of cell.*/
          public int x_coord;

          /**Y-coordinate of cell.*/
          public int y_coord;

          /**contents of cell.*/
          public GridObject contents;

          /**orientation of fort, if there is a fort in the cell.*/
          public Direction fort_orientation;
     }

     /**Attack type: melee, ranged, or capsule*/
     public enum AttackType { MELEE, RANGED, CAPSULE };

     /**Class representing information about a particular attack or
      * attempted attack you suffered the previous round.*/
     public class AttackNotice
     {
          /**Cell from which the attack originated.*/
          GridCell origin;

          /**Form of the attack.*/
          AttackType form;
          
          /**Amount of damage suffered from attack (0 means attack failed)*/
          int damage;
     }

     /**Represents result of attack: missed, hit, destroyed target*/
     public enum AttackResult { MISSED, HIT, DESTROYED_TARGET };

     /**
      * Entry point for your robot on its creation
      * @param api a reference to a WorldAPI object you can use to
      *            interact with the simulator (currently unused)
      * @param skill_points the number of skill points your robot is
      *                     allowed to have.
      * @param message a 64-byte message from the robot who created you.
      *                If you were created by the simulator, the first two
      *                bytes of the message will contain your ID, which is
      *                unique among the IDs of all your team's robots
      *                created by the world.  Otherwise, the format of the
      *                message is unspecified: it's up to you to define it.
      * @return You are to return a Robot_Specs object containing the
      *         allocation of skill points you have chosen for yourself.
      */
     Robot_Specs createRobot(WorldAPI api, int skill_points, byte[] message);

     /**Represents what a robot is currently building*/
     enum BuildStatus { WALL, FORT, CAPSULE, ROBOT };

     /**
      * Each turn, this method is called to allow your robot to act.
      * @param api a reference to a WorldAPI object you can use to
      *            interact with the simulator.
      * @param status a reference to a Robot_Status object containing
      *               information about your current health and energy level
      * @param received_radio the radio signals you have received this
      *                       round.  Each message is exactly 64 bytes long.
      *                       You may be able to receive additional radio
      *                       signals by calling getMessages() with a
      *                       nonzero power if you are being jammed.
      */
     void act(WorldAPI api, Robot_Status status, byte[][] received_radio);
}
